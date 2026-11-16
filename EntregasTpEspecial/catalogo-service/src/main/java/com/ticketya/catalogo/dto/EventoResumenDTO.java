package com.ticketya.catalogo.dto;

import com.ticketya.catalogo.model.EstadoEvento;
import com.ticketya.catalogo.model.TipoEvento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EventoResumenDTO(
        Long id,
        String nombre,
        TipoEvento tipo,
        String organizador,
        List<String> participantes,
        LocalDateTime fechaHora,
        EstadoEvento estado,
        String lugar,
        String ciudad,
        BigDecimal precioDesde
) {
}
