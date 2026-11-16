package com.ticketya.catalogo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lugar_id")
    private Lugar lugar;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSector tipo;

    // Solo para sectores NO_NUMERADO (zona con cupo maximo)
    private Integer capacidad;

    // Solo para sectores NUMERADO
    private Integer filas;
    private Integer asientosPorFila;

    public int capacidadTotal() {
        if (tipo == TipoSector.NUMERADO) {
            return filas * asientosPorFila;
        }
        return capacidad;
    }
}
