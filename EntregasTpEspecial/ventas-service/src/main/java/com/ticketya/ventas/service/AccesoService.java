package com.ticketya.ventas.service;

import com.ticketya.ventas.trazabilidad.Bitacora;
import com.ticketya.ventas.dto.AccesoDTO;
import com.ticketya.ventas.exception.ConflictException;
import com.ticketya.ventas.exception.NotFoundException;
import com.ticketya.ventas.model.*;
import com.ticketya.ventas.repository.EntradaRepository;
import com.ticketya.ventas.repository.RegistroAccesoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

// (8) Control de acceso: el personal escanea el QR y el sistema valida que la entrada exista,
// sea de ese evento y no se haya usado, y la marca como USADA. Un QR no puede ingresar dos veces.
@Service
@RequiredArgsConstructor
public class AccesoService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

    private final EntradaRepository entradaRepo;
    private final RegistroAccesoRepository registroRepo;
    private final TransactionTemplate tx;
    private final Clock clock;

    private record Resultado(ResultadoAcceso resultado, Entrada entrada) {
    }

    public AccesoDTO validarIngreso(Long eventoId, String codigoQr, Long usuarioAccesoId) {
        String qr = codigoQr.trim();
        Resultado r;
        try {
            // Una sola transaccion: el UPDATE atomico y el registro en la bitacora
            r = tx.execute(status -> intentar(eventoId, qr, usuarioAccesoId));
        } catch (DataAccessException e) {
            // Segun la base, si otra puerta estaba marcando el mismo QR en ese instante, el UPDATE puede
            // fallar por bloqueo en lugar de devolver 0 filas. Se vuelve a mirar el estado real:
            ResultadoAcceso motivo = tx.execute(status -> motivoDeRechazo(eventoId, qr));
            if (motivo == null) {
                throw e;   // la entrada sigue vendida: fue otro tipo de error, no una carrera
            }
            r = tx.execute(status -> registrar(eventoId, qr, usuarioAccesoId, motivo));
        }

        Entrada e = r.entrada();
        Bitacora.registrar(r.resultado() == ResultadoAcceso.PERMITIDO ? "INGRESO_PERMITIDO" : "INGRESO_RECHAZADO",
                "evento=" + eventoId + " entrada=" + (e == null ? "-" : e.getId()) + " resultado=" + r.resultado()
                        + " puerta=" + usuarioAccesoId);
        switch (r.resultado()) {
            case PERMITIDO:
                return new AccesoDTO(ResultadoAcceso.PERMITIDO, e.getId(), e.getEventoId(), e.getSectorId(),
                        e.getFila(), e.getNumero(), e.getFechaUso());
            case NO_EXISTE:
                throw new NotFoundException("Acceso denegado: no existe una entrada con ese codigo");
            case OTRO_EVENTO:
                throw new ConflictException("Acceso denegado: la entrada corresponde a otro evento (#" + e.getEventoId() + ")");
            case YA_UTILIZADA:
                throw new ConflictException("Acceso denegado: la entrada ya fue utilizada"
                        + (e.getFechaUso() == null ? "" : " el " + e.getFechaUso().format(HORA)));
            default:
                throw new ConflictException("Acceso denegado: la entrada no esta vigente (estado " + e.getEstado() + ")");
        }
    }

    private Resultado intentar(Long eventoId, String qr, Long usuarioAccesoId) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        int marcadas = entradaRepo.marcarComoUsada(qr, eventoId, EstadoEntrada.VENDIDA, EstadoEntrada.USADA, ahora);
        ResultadoAcceso resultado = marcadas == 1 ? ResultadoAcceso.PERMITIDO : motivoDeRechazo(eventoId, qr);
        // null: seguia VENDIDA pero no se marco (otra puerta en el mismo instante); se informa como usada
        return registrar(eventoId, qr, usuarioAccesoId, resultado == null ? ResultadoAcceso.YA_UTILIZADA : resultado);
    }

    // Por que no se pudo marcar (se consulta despues del UPDATE fallido, solo para informar).
    // Devuelve null si la entrada sigue VENDIDA (es decir, no hay motivo de rechazo).
    private ResultadoAcceso motivoDeRechazo(Long eventoId, String qr) {
        Optional<Entrada> entrada = entradaRepo.findByCodigoQr(qr);
        if (entrada.isEmpty()) {
            return ResultadoAcceso.NO_EXISTE;
        }
        Entrada e = entrada.get();
        if (!e.getEventoId().equals(eventoId)) {
            return ResultadoAcceso.OTRO_EVENTO;
        }
        if (e.getEstado() == EstadoEntrada.USADA) {
            return ResultadoAcceso.YA_UTILIZADA;
        }
        if (e.getEstado() == EstadoEntrada.VENDIDA) {
            return null;
        }
        return ResultadoAcceso.NO_VIGENTE;
    }

    private Resultado registrar(Long eventoId, String qr, Long usuarioAccesoId, ResultadoAcceso resultado) {
        Entrada entrada = entradaRepo.findByCodigoQr(qr).orElse(null);
        RegistroAcceso registro = new RegistroAcceso();
        registro.setCodigoQr(qr.length() > 255 ? qr.substring(0, 255) : qr);
        registro.setEventoId(eventoId);
        registro.setEntradaId(entrada == null ? null : entrada.getId());
        registro.setResultado(resultado);
        registro.setUsuarioAccesoId(usuarioAccesoId);
        registro.setFecha(LocalDateTime.now(clock));
        registroRepo.save(registro);
        return new Resultado(resultado, entrada);
    }
}
