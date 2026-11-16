package com.ticketya.preferencias.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Una compra del historial del cliente, embebida en su perfil (sin JOINs: se lee junto con el perfil).
// Guarda los datos del evento que usan las recomendaciones (tipo, participantes, ciudad).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompraRegistrada {

    private Long compraId;

    // Indice para responder rapido "quienes mas compraron este evento"
    @Indexed
    private Long eventoId;

    private String nombreEvento;
    private String tipo;
    private List<String> participantes = new ArrayList<>();
    private String ciudad;
    private LocalDateTime fecha;
}
