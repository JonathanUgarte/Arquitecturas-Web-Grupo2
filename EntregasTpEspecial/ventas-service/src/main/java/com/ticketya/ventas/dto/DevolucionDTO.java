package com.ticketya.ventas.dto;

import com.ticketya.ventas.model.EstadoDevolucion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DevolucionDTO(
        Long id,
        Long entradaId,
        EstadoDevolucion estado,
        String motivo,
        BigDecimal monto,
        LocalDateTime solicitadaEn,
        LocalDateTime resueltaEn,
        boolean reintegroSolicitado
) {
}
