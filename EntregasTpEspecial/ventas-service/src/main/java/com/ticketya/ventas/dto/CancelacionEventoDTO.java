package com.ticketya.ventas.dto;

public record CancelacionEventoDTO(Long eventoId, int entradasAReintegrar, int reservasLiberadas) {
}
