package com.ticketya.ventas.dto;

import com.ticketya.ventas.model.EstadoReserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReservaDTO(
        Long id,
        Long usuarioId,
        Long eventoId,
        Long sectorId,
        EstadoReserva estado,
        int cantidad,
        List<AsientoDTO> asientos,
        BigDecimal precioUnitario,
        BigDecimal total,
        LocalDateTime venceEn
) {
}
