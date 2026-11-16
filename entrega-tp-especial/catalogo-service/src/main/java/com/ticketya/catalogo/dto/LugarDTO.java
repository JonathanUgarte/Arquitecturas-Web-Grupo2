package com.ticketya.catalogo.dto;

import java.util.List;

public record LugarDTO(
        Long id,
        String nombre,
        String direccion,
        String ciudad,
        Double latitud,
        Double longitud,
        List<SectorDTO> sectores
) {
}
