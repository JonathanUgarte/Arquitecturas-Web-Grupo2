package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.canal.CanalDeEnvio;
import com.ticketya.notificaciones.client.CatalogoClient;
import com.ticketya.notificaciones.client.PreferenciasClient;
import com.ticketya.notificaciones.dto.NotificacionDTO;
import com.ticketya.notificaciones.mensajeria.CompraConfirmadaEvento;
import com.ticketya.notificaciones.mensajeria.EventoCanceladoEvento;
import com.ticketya.notificaciones.mensajeria.EventoPublicadoEvento;
import com.ticketya.notificaciones.model.EstadoNotificacion;
import com.ticketya.notificaciones.model.Notificacion;
import com.ticketya.notificaciones.model.TipoNotificacion;
import com.ticketya.notificaciones.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificacionRepository repo;
    private final CanalDeEnvio canal;
    private final CatalogoClient catalogoClient;
    private final PreferenciasClient preferenciasClient;
    private final Clock reloj;

    // ---- (11) al confirmar una compra ----
    public void compraConfirmada(CompraConfirmadaEvento e) {
        String evento = nombreEvento(e.eventoId());
        String mensaje = "Tu compra #" + e.compraId() + " de " + e.cantidadEntradas()
                + (e.cantidadEntradas() == 1 ? " entrada" : " entradas") + " para " + evento
                + " fue confirmada. Total: " + pesos(e.total()) + ". Tus entradas con codigo QR estan en 'Mis entradas'.";
        notificar(e.usuarioId(), TipoNotificacion.COMPRA_CONFIRMADA, "compra:" + e.compraId(),
                "Compra confirmada", mensaje);
    }

    // ---- (11) ante la cancelacion de un evento: un aviso a cada comprador ----
    public void eventoCancelado(EventoCanceladoEvento e) {
        String evento = nombreEvento(e.eventoId());
        String mensaje = "El evento " + evento + " fue cancelado. Tus entradas pasaron a reintegro: "
                + "el dinero se devuelve al mismo medio de pago.";
        for (Long comprador : e.compradores()) {
            notificar(comprador, TipoNotificacion.EVENTO_CANCELADO, "evento:" + e.eventoId(),
                    "Evento cancelado", mensaje);
        }
    }

    // ---- (11) cuando un artista favorito publica un evento en la ciudad del cliente ----
    public void eventoPublicado(EventoPublicadoEvento e) {
        if (e.participantes() == null || e.participantes().isEmpty() || e.ciudad() == null) {
            return;
        }
        List<Long> interesados = preferenciasClient.interesados(e.ciudad(), e.participantes());
        String fecha = e.fechaHora() == null ? "" : " el " + e.fechaHora().format(FORMATO_FECHA);
        String mensaje = String.join(", ", e.participantes()) + " se presenta en " + e.ciudad() + fecha
                + ": " + e.nombre() + ". Ya podes comprar tus entradas.";
        for (Long usuarioId : interesados) {
            notificar(usuarioId, TipoNotificacion.ARTISTA_FAVORITO, "evento:" + e.eventoId(),
                    "Tu artista favorito viene a tu ciudad", mensaje);
        }
        log.info("Evento {} publicado: {} cliente(s) avisado(s) por artista favorito", e.eventoId(), interesados.size());
    }

    public List<NotificacionDTO> listar(Long usuarioId) {
        return repo.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(n -> new NotificacionDTO(n.getId(), n.getTipo(), n.getCanal(), n.getTitulo(), n.getMensaje(),
                        n.getReferencia(), n.getEstado(), n.getFecha()))
                .toList();
    }

    // Envia y registra un aviso. Idempotente: el mismo aviso (tipo + referencia + usuario) sale una sola vez,
    // aunque RabbitMQ entregue el mensaje repetido.
    void notificar(Long usuarioId, TipoNotificacion tipo, String referencia, String titulo, String mensaje) {
        String clave = tipo + ":" + referencia + ":" + usuarioId;
        if (repo.existsByClaveIdempotencia(clave)) {
            log.info("Aviso {} ya enviado: se ignora el duplicado", clave);
            return;
        }
        Notificacion n = new Notificacion();
        n.setUsuarioId(usuarioId);
        n.setTipo(tipo);
        n.setCanal(canal.nombre());
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setReferencia(referencia);
        n.setClaveIdempotencia(clave);
        n.setFecha(LocalDateTime.now(reloj));
        try {
            canal.enviar(usuarioId, titulo, mensaje);
            n.setEstado(EstadoNotificacion.ENVIADA);
        } catch (RuntimeException ex) {
            // Queda registrado como FALLIDA para poder reenviarlo (los reintentos se suman con la resiliencia)
            log.warn("No se pudo enviar el aviso {}: {}", clave, ex.getMessage());
            n.setEstado(EstadoNotificacion.FALLIDA);
            n.setDetalleError(ex.getMessage());
        }
        try {
            repo.save(n);
        } catch (DuplicateKeyException ex) {
            // dos copias del mismo mensaje procesadas a la vez: el indice unico deja pasar solo una
            log.info("Aviso {} registrado por otro consumidor en paralelo", clave);
        }
    }

    private String nombreEvento(Long eventoId) {
        return catalogoClient.nombreEvento(eventoId).map(n -> "\"" + n + "\"").orElse("#" + eventoId);
    }

    private static String pesos(BigDecimal monto) {
        if (monto == null) {
            return "-";
        }
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).format(monto);
    }
}
