package com.ticketya.notificaciones.mensajeria;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Mensaje "compra.confirmada" (lo publica ventas)
public record CompraConfirmadaEvento(Long compraId, Long usuarioId, Long eventoId, int cantidadEntradas,
                                     BigDecimal total, LocalDateTime fecha) {
}
