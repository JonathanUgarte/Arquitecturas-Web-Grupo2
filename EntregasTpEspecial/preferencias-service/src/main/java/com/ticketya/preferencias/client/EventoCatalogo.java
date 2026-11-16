package com.ticketya.preferencias.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Lo que devuelve GET /eventos del catalogo (un evento a la venta). Los campos que no se usan se ignoran.
public record EventoCatalogo(Long id, String nombre, String tipo, List<String> participantes,
                             LocalDateTime fechaHora, String ciudad, BigDecimal precioDesde) {
}
