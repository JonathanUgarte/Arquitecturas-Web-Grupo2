package com.ticketya.preferencias.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

// Reemplaza las preferencias del cliente. Un campo en null se deja como estaba.
public record PreferenciasRequest(
        @Schema(example = "Tandil") String ciudad,
        @Schema(description = "RECITAL, TEATRO, DEPORTE, CONFERENCIA, FESTIVAL u OTRO", example = "[\"RECITAL\", \"TEATRO\"]")
        Set<String> tiposFavoritos,
        @Schema(example = "[\"Banda Uno\"]") Set<String> artistasFavoritos) {
}
