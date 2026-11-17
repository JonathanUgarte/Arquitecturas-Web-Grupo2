package com.ticketya.notificaciones.service;

import com.ticketya.notificaciones.canal.CanalDeEnvio;
import com.ticketya.notificaciones.client.CatalogoClient;
import com.ticketya.notificaciones.client.PreferenciasClient;
import com.ticketya.notificaciones.client.VentasClient;
import com.ticketya.notificaciones.dto.NotificacionDTO;
import com.ticketya.notificaciones.mensajeria.CompraConfirmadaEvento;
import com.ticketya.notificaciones.mensajeria.EventoCanceladoEvento;
import com.ticketya.notificaciones.mensajeria.EventoPublicadoEvento;
import com.ticketya.notificaciones.mensajeria.EventoSuspendidoEvento;
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

    // Un aviso FALLIDA se reenvia hasta completar este total de intentos; despues queda FALLIDA para revision
    public static final int MAX_INTENTOS = 5;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificacionRepository repo;
    private final CanalDeEnvio canal;
    private final CatalogoClient catalogoClient;
    private final PreferenciasClient preferenciasClient;
    private final VentasClient ventasClient;
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

    // ---- (11) ante un cambio de un evento (suspension): un aviso a cada cliente con entradas vigentes ----
    // Los compradores se piden a ventas, que es quien conoce las entradas. Si ventas no responde, el mensaje
    // se reintenta (y termina en la cola de fallidos): el aviso no se pierde.
    public void eventoSuspendido(EventoSuspendidoEvento e) {
        List<Long> compradores = ventasClient.compradores(e.eventoId());
        String evento = e.nombre() != null ? "\"" + e.nombre() + "\"" : nombreEvento(e.eventoId());
        String fecha = e.fechaHora() == null ? "" : " (previsto para el " + e.fechaHora().format(FORMATO_FECHA) + ")";
        String mensaje = "El evento " + evento + fecha + " fue suspendido. Tus entradas siguen siendo validas: "
                + "te vamos a avisar si se reprograma o se cancela.";
        for (Long comprador : compradores) {
            notificar(comprador, TipoNotificacion.EVENTO_SUSPENDIDO, "evento:" + e.eventoId(),
                    "Evento suspendido", mensaje);
        }
        log.info("Evento {} suspendido: {} comprador(es) avisado(s)", e.eventoId(), compradores.size());
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
        enviar(n);
        try {
            repo.save(n);
        } catch (DuplicateKeyException ex) {
            // dos copias del mismo mensaje procesadas a la vez: el indice unico deja pasar solo una
            log.info("Aviso {} registrado por otro consumidor en paralelo", clave);
        }
    }

    // Un intento de envio por el canal. Si falla queda FALLIDA y la tarea periodica lo vuelve a intentar.
    private void enviar(Notificacion n) {
        n.setIntentos(n.getIntentos() + 1);
        try {
            canal.enviar(n.getUsuarioId(), n.getTitulo(), n.getMensaje());
            n.setEstado(EstadoNotificacion.ENVIADA);
            n.setDetalleError(null);
        } catch (RuntimeException ex) {
            log.warn("No se pudo enviar el aviso {} (intento {}): {}", n.getClaveIdempotencia(), n.getIntentos(),
                    ex.getMessage());
            n.setEstado(EstadoNotificacion.FALLIDA);
            n.setDetalleError(ex.getMessage());
        }
    }

    // ---- reenvio programado de los avisos que no se pudieron enviar (ver ReintentosProgramados) ----
    // Devuelve cuantos se enviaron en esta pasada.
    public int reintentarFallidas() {
        int enviadas = 0;
        for (Notificacion n : repo.findByEstadoAndIntentosLessThan(EstadoNotificacion.FALLIDA, MAX_INTENTOS)) {
            enviar(n);
            repo.save(n);
            if (n.getEstado() == EstadoNotificacion.ENVIADA) {
                enviadas++;
            } else if (n.getIntentos() >= MAX_INTENTOS) {
                log.error("El aviso {} no se pudo enviar despues de {} intentos: queda FALLIDA",
                        n.getClaveIdempotencia(), n.getIntentos());
            }
        }
        if (enviadas > 0) {
            log.info("Avisos fallidos reenviados: {}", enviadas);
        }
        return enviadas;
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
