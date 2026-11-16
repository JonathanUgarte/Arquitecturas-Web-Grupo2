package com.ticketya.catalogo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Precio de un sector para un evento concreto (el organizador lo define por evento)
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "sector_id"}))
@Getter
@Setter
@NoArgsConstructor
public class PrecioSector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;
}
