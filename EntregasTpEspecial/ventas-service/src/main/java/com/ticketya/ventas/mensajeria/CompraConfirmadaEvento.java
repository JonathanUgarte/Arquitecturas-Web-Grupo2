package com.ticketya.ventas.mensajeria;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Se publica con la clave "compra.confirmada" cuando un pago se aprueba y se emiten las entradas.
// Lo consumen notificaciones (aviso al cliente) y preferencias (historial para recomendaciones).
public record CompraConfirmadaEvento(Long compraId, Long usuarioId, Long eventoId, int cantidadEntradas,
                                     BigDecimal total, LocalDateTime fecha) {

    public static final String CLAVE = "compra.confirmada";
}
