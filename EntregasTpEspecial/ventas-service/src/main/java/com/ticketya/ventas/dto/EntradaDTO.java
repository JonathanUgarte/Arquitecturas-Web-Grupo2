package com.ticketya.ventas.dto;

import com.ticketya.ventas.model.EstadoEntrada;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EntradaDTO(
        Long id,
        String codigoQr,
        Long eventoId,
        Long sectorId,
        Integer fila,
        Integer numero,
        BigDecimal precio,
        EstadoEntrada estado,
        LocalDateTime fechaEvento
) {
}
