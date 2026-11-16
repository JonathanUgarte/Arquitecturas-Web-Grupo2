package com.ticketya.catalogo.dto;

import com.ticketya.catalogo.model.EstadoEvento;
import com.ticketya.catalogo.model.TipoSector;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Lo que el servicio de ventas necesita saber de un sector de un evento para vender
public record SectorVentaDTO(
        Long eventoId,
        String evento,
        EstadoEvento estadoEvento,
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
