package com.ticketya.ventas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Bitacora de cada escaneo en el ingreso, permitido o rechazado (auditoria: quien escaneo que y cuando)
@Entity
@Getter
@Setter
@NoArgsConstructor
public class RegistroAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String codigoQr;

    @Column(nullable = false)
    private Long eventoId;

    // null si el codigo no corresponde a ninguna entrada
    private Long entradaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoAcceso resultado;

    // personal de acceso que escaneo
    private Long usuarioAccesoId;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
