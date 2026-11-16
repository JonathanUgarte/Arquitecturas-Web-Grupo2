package com.ticketya.catalogo.dto;

import com.ticketya.catalogo.model.EstadoEvento;
import com.ticketya.catalogo.model.TipoEvento;

import java.time.LocalDateTime;
import java.util.List;

public record EventoDTO(
        Long id,
        String nombre,
        String descripcion,
        TipoEvento tipo,
        String organizador,
        List<String> participantes,
        LocalDateTime fechaHora,
        EstadoEvento estado,
        LugarDTO lugar,
        List<PrecioSectorDTO> precios
) {
}
