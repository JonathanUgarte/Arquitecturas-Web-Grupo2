package com.ticketya.ventas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Reserva temporal de lugares mientras el cliente paga (vence a los N minutos)
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId;

    // ids del servicio de catalogo (otro microservicio: solo se guarda el id)
    @Column(nullable = false)
    private Long eventoId;

    @Column(nullable = false)
    private Long sectorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSector tipoSector;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    // copia de la fecha del evento (para la regla de devolucion sin consultar a catalogo)
    @Column(nullable = false)
    private LocalDateTime fechaEvento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    @Column(nullable = false)
    private LocalDateTime venceEn;
}
