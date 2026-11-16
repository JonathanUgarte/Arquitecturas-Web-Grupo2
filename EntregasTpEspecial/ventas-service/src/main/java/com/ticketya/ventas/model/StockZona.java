package com.ticketya.ventas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Cupo disponible de una zona sin asiento (ej. "Campo"). Se modifica con bloqueo pesimista
// (SELECT ... FOR UPDATE), asi el cupo nunca queda negativo.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "sector_id"}))
@Getter
@Setter
@NoArgsConstructor
public class StockZona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(nullable = false)
    private int disponibles;
}
