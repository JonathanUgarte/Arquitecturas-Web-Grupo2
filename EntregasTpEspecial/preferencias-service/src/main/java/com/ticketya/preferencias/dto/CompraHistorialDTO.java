package com.ticketya.preferencias.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CompraHistorialDTO(Long compraId, Long eventoId, String nombreEvento, String tipo,
                                 List<String> participantes, String ciudad, LocalDateTime fecha) {
}
