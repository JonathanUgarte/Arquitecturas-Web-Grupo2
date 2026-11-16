package com.ticketya.catalogo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Lugar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false)
    private String ciudad;

    private Double latitud;
    private Double longitud;

    @OneToMany(mappedBy = "lugar", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sector> sectores = new ArrayList<>();
}
