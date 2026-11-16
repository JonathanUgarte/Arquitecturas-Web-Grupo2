package com.ticketya.preferencias.mensajeria;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Copia local del contrato del mensaje "compra.confirmada" que publica ventas (cada servicio tiene la suya:
// no se comparten clases entre microservicios, solo el formato JSON)
public record CompraConfirmadaEvento(Long compraId, Long usuarioId, Long eventoId, int cantidadEntradas,
                                     BigDecimal total, LocalDateTime fecha) {
}
