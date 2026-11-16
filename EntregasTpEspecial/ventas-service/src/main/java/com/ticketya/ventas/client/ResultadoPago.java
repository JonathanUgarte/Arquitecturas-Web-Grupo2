package com.ticketya.ventas.client;

// aprobado: se cobro | rechazado (aprobado = false): respuesta definitiva de la pasarela (p. ej. fondos insuficientes)
// pasarelaNoDisponible: no se pudo cobrar porque la pasarela no responde (despues de reintentar): la reserva se mantiene
public record ResultadoPago(boolean aprobado, String transaccionId, String motivo, boolean pasarelaNoDisponible) {

    public static ResultadoPago aprobado(String transaccionId) {
        return new ResultadoPago(true, transaccionId, null, false);
    }

    public static ResultadoPago rechazado(String motivo) {
        return new ResultadoPago(false, null, motivo, false);
    }

    public static ResultadoPago noDisponible(String motivo) {
        return new ResultadoPago(false, null, motivo, true);
    }
}
