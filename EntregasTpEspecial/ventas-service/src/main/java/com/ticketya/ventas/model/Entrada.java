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
public class Entrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // codigo QR unico: es lo que escanea el personal de acceso en el ingreso
    @Column(nullable = false, unique = true)
    private String codigoQr;

    @Column(nullable = false)
    private Long eventoId;

    @Column(nullable = false)
    private Long sectorId;

    // solo para sectores numerados
    private Integer fila;
    private Integer numero;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEntrada estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @Column(nullable = false)
    private LocalDateTime fechaEvento;

    @Column(nullable = false)
    private LocalDateTime fechaCompra;

    // momento en que se escaneo en el ingreso (estado USADA)
    private LocalDateTime fechaUso;
}
