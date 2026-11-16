package com.ticketya.catalogo.service;

import com.ticketya.catalogo.client.VentasClient;
import com.ticketya.catalogo.dto.CancelacionDTO;
import com.ticketya.catalogo.dto.EventoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Orquesta la cancelacion: primero se confirma el cambio en la base (transaccion de EventoService)
// y recien despues se avisa a ventas por HTTP. Asi la llamada HTTP queda FUERA de la transaccion.
@Service
@RequiredArgsConstructor
public class CancelacionService {

    private final EventoService eventoService;
    private final VentasClient ventasClient;

    public CancelacionDTO cancelar(Long eventoId, String motivo) {
        EventoDTO evento = eventoService.cancelar(eventoId, motivo);
        boolean notificado = ventasClient.notificarCancelacion(eventoId);
        return new CancelacionDTO(evento.id(), evento.estado(), motivo, notificado);
    }
}
