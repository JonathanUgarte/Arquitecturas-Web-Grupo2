package com.ticketya.fila.dto;

// Vista del organizador / administrador
public record EstadoFilaDTO(Long eventoId, long enEspera, long habilitadosEnTotal, int tamanioTanda,
                            long intervaloSegundos, int minutosTurno) {
}
