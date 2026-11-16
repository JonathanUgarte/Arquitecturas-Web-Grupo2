package com.ticketya.ventas.dto;

import com.ticketya.ventas.model.ResultadoAcceso;

import java.time.LocalDateTime;

// Respuesta de un ingreso permitido: datos para que el personal indique por donde entrar
public record AccesoDTO(ResultadoAcceso resultado, Long entradaId, Long eventoId, Long sectorId,
                        Integer fila, Integer numero, LocalDateTime fechaUso) {
}
