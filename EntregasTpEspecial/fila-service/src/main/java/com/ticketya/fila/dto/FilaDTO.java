package com.ticketya.fila.dto;

import java.time.LocalDateTime;

// Situacion de un cliente en la fila de un evento.
// Con estado HABILITADO, "turno" es el token que hay que enviar en el header X-Turno-Fila al reservar.
public record FilaDTO(Long eventoId, EstadoEnFila estado, Long posicion, Long personasAdelante,
                      Long tiempoEstimadoSegundos, String turno, LocalDateTime turnoVenceEn, String mensaje) {
}
