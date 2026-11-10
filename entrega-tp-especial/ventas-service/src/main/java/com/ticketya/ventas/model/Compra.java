package com.ticketya.ventas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Intento de pago de una reserva. La clave de idempotencia es unica: si el cliente reintenta
// el pago (por un corte de conexion) con la misma clave, no se cobra dos veces.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"clave_idempotencia"}))
@Getter
@Setter
@NoArgsConstructor
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Column(name = "clave_idempotencia", nullable = false)
    private String claveIdempotencia;

    @Column(nullable = false)
    private String medioPago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCompra estado;

    private String transaccionPago;
    private String motivoRechazo;

    @Column(nullable = false)
    private LocalDateTime creadaEn;
}
