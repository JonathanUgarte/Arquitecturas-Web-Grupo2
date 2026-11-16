package com.ticketya.preferencias.dto;

import java.util.List;
import java.util.Set;

public record PerfilDTO(Long usuarioId, String ciudad, Set<String> tiposFavoritos, Set<String> artistasFavoritos,
                        List<CompraHistorialDTO> historial) {
}
