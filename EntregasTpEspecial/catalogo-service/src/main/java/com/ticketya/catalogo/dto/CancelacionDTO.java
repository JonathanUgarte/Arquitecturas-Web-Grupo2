package com.ticketya.catalogo.dto;

import com.ticketya.catalogo.model.EstadoEvento;

public record CancelacionDTO(
        Long eventoId,
        EstadoEvento estado,
        String motivo,
        boolean reintegroNotificado   // false si ventas no pudo ser avisado (queda pendiente)
) {
}
