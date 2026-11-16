package com.ticketya.catalogo.dto;

import com.ticketya.catalogo.model.EstadoEvento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// (12) Reporte del organizador: por sector y en total
public record ReporteEventoDTO(Long eventoId, String evento, EstadoEvento estado, LocalDateTime fechaHora,
                               String lugar, String ciudad, LocalDateTime generadoEn,
                               List<ReporteSectorDTO> sectores,
                               int capacidadTotal, long vendidas, long devueltas, long aReintegrar, long utilizadas,
                               BigDecimal ocupacionPorcentaje, BigDecimal recaudacion, BigDecimal montoReintegrado) {
}
