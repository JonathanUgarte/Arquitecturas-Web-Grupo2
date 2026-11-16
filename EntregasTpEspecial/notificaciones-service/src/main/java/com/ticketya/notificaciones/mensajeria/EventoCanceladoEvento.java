package com.ticketya.notificaciones.mensajeria;

import java.time.LocalDateTime;
import java.util.List;

// Mensaje "evento.cancelado" (lo publica ventas despues de pasar las entradas a reintegro)
public record EventoCanceladoEvento(Long eventoId, List<Long> compradores, int entradasAfectadas,
                                    LocalDateTime fecha) {
}
