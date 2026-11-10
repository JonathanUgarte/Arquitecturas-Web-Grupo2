package com.ticketya.catalogo.dto;

import com.ticketya.catalogo.model.TipoSector;

public record SectorDTO(
        Long id,
        String nombre,
        TipoSector tipo,
        Integer capacidad,
        Integer filas,
        Integer asientosPorFila,
        int capacidadTotal
) {
}
