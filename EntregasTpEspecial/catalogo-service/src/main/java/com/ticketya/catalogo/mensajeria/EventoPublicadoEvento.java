package com.ticketya.catalogo.mensajeria;

import java.time.LocalDateTime;
import java.util.List;

// Se publica con la clave "evento.publicado" cuando un evento pasa a PUBLICADO.
// Notificaciones lo usa para avisar a los clientes cuyo artista favorito toca en su ciudad.
public record EventoPublicadoEvento(Long eventoId, String nombre, String tipo, List<String> participantes,
                                    String ciudad, LocalDateTime fechaHora) {

    public static final String CLAVE = "evento.publicado";
}
