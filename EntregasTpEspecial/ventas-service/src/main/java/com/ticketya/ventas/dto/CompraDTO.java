package com.ticketya.ventas.dto;

import com.ticketya.ventas.model.EstadoCompra;

import java.math.BigDecimal;
import java.util.List;

public record CompraDTO(
        Long id,
        Long reservaId,
        EstadoCompra estado,
        BigDecimal monto,
        String medioPago,
        String transaccionPago,
        String motivoRechazo,
        List<EntradaDTO> entradas
) {
}
