package com.ticketya.catalogo.dto;

import com.ticketya.catalogo.model.TipoSector;

import java.math.BigDecimal;

public record ReporteSectorDTO(Long sectorId, String sector, TipoSector tipo, BigDecimal precio, int capacidad,
                               long vendidas, long disponibles, BigDecimal ocupacionPorcentaje,
                               long utilizadas, long devueltas, long aReintegrar,
                               BigDecimal recaudacion, BigDecimal montoReintegrado) {
}
