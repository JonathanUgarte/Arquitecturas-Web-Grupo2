package com.ticketya.preferencias.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Un evento recomendado, con el puntaje y los motivos (para poder explicar por que se recomienda)
public record RecomendacionDTO(Long eventoId, String nombre, String tipo, String ciudad, LocalDateTime fechaHora,
                               BigDecimal precioDesde, int puntaje, List<String> motivos) {
}
