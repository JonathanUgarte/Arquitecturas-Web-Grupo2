package com.ticketya.ventas.client;

// La pasarela no respondio (timeout, caida, error 5xx). Es una falla TEMPORAL: se puede reintentar.
// Distinto de un pago rechazado (fondos insuficientes, tarjeta invalida), que es una respuesta definitiva.
public class PasarelaNoDisponibleException extends RuntimeException {
    public PasarelaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
