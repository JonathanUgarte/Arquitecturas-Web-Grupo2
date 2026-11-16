package com.ticketya.ventas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un asiento numerado reservado o vendido. La restriccion de unicidad es lo que impide
// la doble venta: la base rechaza al segundo que intente tomar el mismo asiento.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "sector_id", "fila", "numero"}))
@Getter
@Setter
@NoArgsConstructor
public class AsientoOcupado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @Column(name = "fila", nullable = false)
    private int fila;

    @Column(name = "numero", nullable = false)
    private int numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;
}
