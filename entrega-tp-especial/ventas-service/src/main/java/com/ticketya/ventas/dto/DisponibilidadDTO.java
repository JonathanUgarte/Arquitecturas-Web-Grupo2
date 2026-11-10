package com.ticketya.ventas.dto;

import com.ticketya.ventas.model.TipoSector;

import java.util.List;

public record DisponibilidadDTO(
        Long eventoId,
        Long sectorId,
        TipoSector tipo,
        int capacidadTotal,
        int disponibles,
        List<AsientoDTO> asientosOcupados   // vacio en zonas sin asiento
) {
}
