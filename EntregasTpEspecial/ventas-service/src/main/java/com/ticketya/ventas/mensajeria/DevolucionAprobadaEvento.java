package com.ticketya.ventas.mensajeria;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Se publica con la clave "devolucion.aprobada" cuando un administrador aprueba una devolucion: se libero un lugar.
// Lo consume catalogo para volver a poner a la venta un evento que estaba AGOTADO.
public record DevolucionAprobadaEvento(Long devolucionId, Long eventoId, Long usuarioId, BigDecimal monto,
                                       LocalDateTime fecha) {

    public static final String CLAVE = "devolucion.aprobada";
}
