package com.ticketya.notificaciones.mensajeria;

import java.time.LocalDateTime;
import java.util.List;

// Mensaje "evento.publicado" (lo publica catalogo)
public record EventoPublicadoEvento(Long eventoId, String nombre, String tipo, List<String> participantes,
                                    String ciudad, LocalDateTime fechaHora) {
}
