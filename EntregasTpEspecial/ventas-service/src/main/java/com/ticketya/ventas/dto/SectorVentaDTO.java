package com.ticketya.ventas.dto;

import com.ticketya.ventas.model.TipoSector;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Respuesta del servicio de catalogo: lo que hace falta saber de un sector para venderlo
public record SectorVentaDTO(
        Long eventoId,
        String evento,
        String estadoEvento,
        LocalDateTime fechaHora,
        Long sectorId,
        String sector,
        TipoSector tipo,
        int capacidadTotal,
        Integer filas,
        Integer asientosPorFila,
        BigDecimal precio
) {
}
