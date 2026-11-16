package com.ticketya.catalogo.exception;

import java.time.LocalDateTime;

public record ErrorDTO(LocalDateTime timestamp, int status, String error, String mensaje) {
}
