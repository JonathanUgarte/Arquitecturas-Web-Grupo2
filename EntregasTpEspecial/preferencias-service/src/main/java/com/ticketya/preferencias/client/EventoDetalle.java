package com.ticketya.preferencias.client;

import java.time.LocalDateTime;
import java.util.List;

// Lo que devuelve GET /eventos/{id} del catalogo (solo los campos que se usan)
public record EventoDetalle(Long id, String nombre, String tipo, List<String> participantes,
                            LocalDateTime fechaHora, Lugar lugar) {

    public record Lugar(String ciudad) {
    }

    public String ciudad() {
        return lugar == null ? null : lugar.ciudad();
    }
}
