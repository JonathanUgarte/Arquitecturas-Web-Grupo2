package com.ticketya.catalogo.service;

import com.ticketya.catalogo.client.VentasClient;
import com.ticketya.catalogo.dto.CancelacionDTO;
import com.ticketya.catalogo.dto.EventoDTO;
import com.ticketya.catalogo.trazabilidad.Bitacora;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Orquesta la cancelacion: primero se confirma el cambio en la base (transaccion de EventoService)
// y recien despues se avisa a ventas por HTTP. Asi la llamada HTTP queda FUERA de la transaccion.
// Si ventas no recibe el aviso (aun con reintentos), el evento queda con el aviso PENDIENTE y una tarea periodica
// lo vuelve a enviar hasta que ventas lo confirme: las entradas terminan pasando a reintegro (consistencia eventual).
@Service
@RequiredArgsConstructor
@Slf4j
public class CancelacionService {

    private final EventoService eventoService;
    private final VentasClient ventasClient;

    public CancelacionDTO cancelar(Long eventoId, String motivo) {
        EventoDTO evento = eventoService.cancelar(eventoId, motivo);
        boolean notificado = ventasClient.notificarCancelacion(eventoId);   // con reintentos y circuit breaker
        eventoService.registrarAvisoCancelacion(eventoId, notificado);
        Bitacora.registrar("EVENTO_CANCELADO", "evento=" + eventoId + " motivo=" + motivo
                + " ventasNotificado=" + notificado);
        return new CancelacionDTO(evento.id(), evento.estado(), motivo, notificado);
    }

    // Reenvia los avisos de cancelacion pendientes. Devuelve cuantos se confirmaron.
    public int reintentarAvisosPendientes() {
        int confirmados = 0;
        for (Long eventoId : eventoService.eventosConAvisoCancelacionPendiente()) {
            if (ventasClient.notificarCancelacion(eventoId)) {
                eventoService.registrarAvisoCancelacion(eventoId, true);
                confirmados++;
            }
        }
        if (confirmados > 0) {
            log.info("Avisos de cancelacion pendientes confirmados por ventas: {}", confirmados);
        }
        return confirmados;
    }
}
