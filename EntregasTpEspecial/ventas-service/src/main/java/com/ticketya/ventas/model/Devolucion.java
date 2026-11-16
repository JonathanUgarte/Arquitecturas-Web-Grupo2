package com.ticketya.ventas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrada_id")
    private Entrada entrada;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private String motivo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDevolucion estado;

    @Column(nullable = false)
    private LocalDateTime solicitadaEn;

    private LocalDateTime resueltaEn;

    // true cuando ya se le pidio el reintegro a la pasarela de pagos
    private boolean reintegroSolicitado;
}
