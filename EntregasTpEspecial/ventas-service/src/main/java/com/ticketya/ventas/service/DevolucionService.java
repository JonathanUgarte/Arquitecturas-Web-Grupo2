package com.ticketya.ventas.service;

import com.ticketya.ventas.client.ClientePasarela;
import com.ticketya.ventas.dto.DevolucionDTO;
import com.ticketya.ventas.dto.DevolucionRequest;
import com.ticketya.ventas.exception.ConflictException;
import com.ticketya.ventas.exception.ForbiddenException;
import com.ticketya.ventas.exception.NotFoundException;
import com.ticketya.ventas.model.*;
import com.ticketya.ventas.repository.AsientoOcupadoRepository;
import com.ticketya.ventas.repository.DevolucionRepository;
import com.ticketya.ventas.repository.EntradaRepository;
import com.ticketya.ventas.repository.StockZonaRepository;
import com.ticketya.ventas.mensajeria.DevolucionAprobadaEvento;
import com.ticketya.ventas.trazabilidad.Bitacora;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

// Devoluciones: el cliente las solicita hasta 72 horas antes del evento y un administrador las
// aprueba o rechaza. Al aprobar: se libera el lugar y se pide el reintegro a la pasarela.
@Service
@RequiredArgsConstructor
@Slf4j
public class DevolucionService {

    private static final long HORAS_LIMITE = 72;

    private final DevolucionRepository devolucionRepo;
    private final EntradaRepository entradaRepo;
    private final AsientoOcupadoRepository asientoRepo;
    private final StockZonaRepository stockRepo;
    private final ClientePasarela pasarela;
    private final ApplicationEventPublisher eventos;
    private final TransactionTemplate tx;
    private final Clock clock;

    private record DatosReintegro(BigDecimal monto, String transaccionPago, Long eventoId, Long usuarioId) {
    }

    // ---------- el cliente solicita la devolucion ----------
    public DevolucionDTO solicitar(Long usuarioId, DevolucionRequest req) {
        return tx.execute(status -> {
            Entrada e = entradaRepo.findByIdForUpdate(req.entradaId())
                    .orElseThrow(() -> new NotFoundException("No existe la entrada " + req.entradaId()));
            if (!e.getUsuarioId().equals(usuarioId)) {
                throw new ForbiddenException("La entrada no pertenece al usuario");
            }
            if (e.getEstado() != EstadoEntrada.VENDIDA) {
                throw new ConflictException("La entrada esta " + e.getEstado() + " y no se puede devolver");
            }
            LocalDateTime ahora = LocalDateTime.now(clock);
            if (e.getFechaEvento().isBefore(ahora.plusHours(HORAS_LIMITE))) {
                throw new ConflictException("Solo se puede solicitar la devolucion hasta " + HORAS_LIMITE
                        + " horas antes del evento");
            }
            if (devolucionRepo.existsByEntradaIdAndEstado(e.getId(), EstadoDevolucion.PENDIENTE)) {
                throw new ConflictException("Ya hay una solicitud de devolucion pendiente para esta entrada");
            }

            Devolucion d = new Devolucion();
            d.setEntrada(e);
            d.setUsuarioId(usuarioId);
            d.setMotivo(req.motivo());
            d.setMonto(e.getPrecio());
            d.setEstado(EstadoDevolucion.PENDIENTE);
            d.setSolicitadaEn(ahora);
            devolucionRepo.save(d);
            Bitacora.registrar("DEVOLUCION_SOLICITADA", "devolucion=" + d.getId() + " entrada=" + e.getId()
                    + " usuario=" + usuarioId + " monto=" + d.getMonto());
            return Mapeos.toDTO(d);
        });
    }

    // ---------- el administrador aprueba ----------
    public DevolucionDTO aprobar(Long devolucionId) {
        // 1. transaccion: cambiar estados y liberar el lugar
        DatosReintegro datos = tx.execute(status -> aprobarEnBase(devolucionId));

        // 2. fuera de la transaccion: pedir el reintegro a la pasarela
        boolean resultado;
        try {
            pasarela.reintegrar(datos.monto(), datos.transaccionPago());   // con reintentos y circuit breaker
            resultado = true;
        } catch (RuntimeException e) {
            log.warn("No se pudo pedir el reintegro de la devolucion {}: {}", devolucionId, e.getMessage());
            resultado = false;
        }
        final boolean reintegroSolicitado = resultado;

        // Se libero un lugar: catalogo lo usa para volver a poner a la venta un evento AGOTADO (asincronico)
        eventos.publishEvent(new DevolucionAprobadaEvento(devolucionId, datos.eventoId(), datos.usuarioId(),
                datos.monto(), LocalDateTime.now(clock)));
        Bitacora.registrar("DEVOLUCION_APROBADA", "devolucion=" + devolucionId + " evento=" + datos.eventoId()
                + " usuario=" + datos.usuarioId() + " monto=" + datos.monto()
                + " reintegro=" + (reintegroSolicitado ? "pedido" : "pendiente"));

        // 3. transaccion: dejar registrado si el reintegro se pidio (si no, queda pendiente de reintentar)
        return tx.execute(status -> {
            Devolucion d = devolucionRepo.findById(devolucionId).orElseThrow();
            d.setReintegroSolicitado(reintegroSolicitado);
            return Mapeos.toDTO(d);
        });
    }

    // ---------- reintento programado de reintegros pendientes ----------
    // Si al aprobar una devolucion la pasarela no respondio, el reintegro queda pendiente (reintegroSolicitado = false)
    // y se vuelve a pedir periodicamente (ver ReintentosProgramados). Devuelve cuantos se pudieron pedir.
    public int reintentarReintegrosPendientes() {
        List<Long> pendientes = tx.execute(status ->
                devolucionRepo.findByEstadoAndReintegroSolicitadoFalse(EstadoDevolucion.APROBADA).stream()
                        .map(Devolucion::getId).toList());
        int pedidos = 0;
        for (Long id : pendientes) {
            DatosReintegro datos = tx.execute(status -> {
                Devolucion d = devolucionRepo.findById(id).orElseThrow();
                return new DatosReintegro(d.getMonto(), d.getEntrada().getCompra().getTransaccionPago(),
                        d.getEntrada().getEventoId(), d.getUsuarioId());
            });
            try {
                pasarela.reintegrar(datos.monto(), datos.transaccionPago());
            } catch (RuntimeException e) {
                log.warn("Reintegro de la devolucion {} sigue pendiente: {}", id, e.getMessage());
                continue;
            }
            tx.executeWithoutResult(status -> devolucionRepo.findById(id).orElseThrow().setReintegroSolicitado(true));
            pedidos++;
        }
        if (pedidos > 0) {
            log.info("Reintegros pendientes pedidos a la pasarela: {}", pedidos);
        }
        return pedidos;
    }

    private DatosReintegro aprobarEnBase(Long devolucionId) {
        Devolucion d = devolucionRepo.findByIdForUpdate(devolucionId)
                .orElseThrow(() -> new NotFoundException("No existe la devolucion " + devolucionId));
        if (d.getEstado() != EstadoDevolucion.PENDIENTE) {
            throw new ConflictException("La devolucion ya esta " + d.getEstado());
        }
        Entrada e = entradaRepo.findByIdForUpdate(d.getEntrada().getId()).orElseThrow();
        if (e.getEstado() != EstadoEntrada.VENDIDA) {
            throw new ConflictException("La entrada ya no esta vendida (esta " + e.getEstado() + ")");
        }

        e.setEstado(EstadoEntrada.DEVUELTA);
        d.setEstado(EstadoDevolucion.APROBADA);
        d.setResueltaEn(LocalDateTime.now(clock));

        // el lugar queda libre para otro cliente
        if (e.getFila() != null) {
            asientoRepo.eliminarAsiento(e.getEventoId(), e.getSectorId(), e.getFila(), e.getNumero());
        } else {
            StockZona stock = stockRepo.findForUpdate(e.getEventoId(), e.getSectorId()).orElseThrow();
            stock.setDisponibles(stock.getDisponibles() + 1);
        }
        return new DatosReintegro(d.getMonto(), e.getCompra().getTransaccionPago(), e.getEventoId(), d.getUsuarioId());
    }

    // ---------- el administrador rechaza ----------
    public DevolucionDTO rechazar(Long devolucionId) {
        return tx.execute(status -> {
            Devolucion d = devolucionRepo.findByIdForUpdate(devolucionId)
                    .orElseThrow(() -> new NotFoundException("No existe la devolucion " + devolucionId));
            if (d.getEstado() != EstadoDevolucion.PENDIENTE) {
                throw new ConflictException("La devolucion ya esta " + d.getEstado());
            }
            d.setEstado(EstadoDevolucion.RECHAZADA);
            d.setResueltaEn(LocalDateTime.now(clock));
            Bitacora.registrar("DEVOLUCION_RECHAZADA", "devolucion=" + devolucionId + " usuario=" + d.getUsuarioId());
            return Mapeos.toDTO(d);
        });
    }

    public List<DevolucionDTO> listar(EstadoDevolucion estado) {
        return tx.execute(status -> devolucionRepo.findByEstadoOrderBySolicitadaEnAsc(estado).stream()
                .map(Mapeos::toDTO)
                .toList());
    }
}
