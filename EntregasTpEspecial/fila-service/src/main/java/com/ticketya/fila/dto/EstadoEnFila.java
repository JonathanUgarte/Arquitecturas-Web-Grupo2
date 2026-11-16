package com.ticketya.fila.dto;

public enum EstadoEnFila {
    // en la fila, esperando su tanda
    ESPERANDO,
    // le toco: tiene un turno vigente para reservar
    HABILITADO,
    // no esta en la fila (nunca entro, salio o se le vencio el turno)
    FUERA_DE_FILA
}
