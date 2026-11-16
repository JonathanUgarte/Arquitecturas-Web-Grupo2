package com.ticketya.ventas.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Marca de que catalogo aviso la cancelacion de un evento: los pagos que todavia estaban
// en curso emiten sus entradas directamente en A_REINTEGRAR.
@Entity
@Getter
@Setter
@NoArgsConstructor
public class EventoCancelado {

    @Id
    private Long eventoId;

    private LocalDateTime canceladoEn;
}
