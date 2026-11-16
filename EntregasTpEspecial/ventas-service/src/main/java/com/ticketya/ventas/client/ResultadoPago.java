package com.ticketya.ventas.client;

public record ResultadoPago(boolean aprobado, String transaccionId, String motivo) {
}
