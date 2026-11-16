package com.ticketya.ventas.mensajeria;

import java.time.LocalDateTime;
import java.util.List;

// Se publica con la clave "evento.cancelado" despues de pasar a reintegro las entradas de un evento
// cancelado. Lleva a quienes compraron, para que notificaciones les avise.
public record EventoCanceladoEvento(Long eventoId, List<Long> compradores, int entradasAfectadas,
                                    LocalDateTime fecha) {

    public static final String CLAVE = "evento.cancelado";
}
