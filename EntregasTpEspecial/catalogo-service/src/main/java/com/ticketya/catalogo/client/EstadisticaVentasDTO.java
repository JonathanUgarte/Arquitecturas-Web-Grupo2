package com.ticketya.catalogo.client;

import java.math.BigDecimal;

// Respuesta de ventas (GET /interno/eventos/{id}/estadisticas): ventas de un sector
public record EstadisticaVentasDTO(Long sectorId, long vendidas, long utilizadas, long devueltas, long aReintegrar,
                                   BigDecimal recaudacion, BigDecimal montoReintegrado) {
}
