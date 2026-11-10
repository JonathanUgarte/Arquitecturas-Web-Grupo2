package com.ticketya.catalogo.dto;

import com.ticketya.catalogo.model.TipoSector;

import java.math.BigDecimal;

public record PrecioSectorDTO(Long sectorId, String sector, TipoSector tipo, BigDecimal precio) {
}
