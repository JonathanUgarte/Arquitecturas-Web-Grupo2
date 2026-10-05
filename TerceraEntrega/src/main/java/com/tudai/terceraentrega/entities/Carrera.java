package com.tudai.terceraentrega.entities;
import jakarta.persistence.*; import java.util.*;
@Entity @Table(name="carrera")
public class Carrera {
 @Id @Column(name="id_carrera") private int idCarrera;
 @Column(nullable=false,unique=true) private String nombre;
 @Column(nullable=false) private int duracion;
 @OneToMany(mappedBy="carrera") private List<Inscripcion> inscripciones=new ArrayList<>();
 public Carrera(){}
 public int getIdCarrera(){return idCarrera;} public void setIdCarrera(int v){idCarrera=v;}
 public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
 public int getDuracion(){return duracion;} public void setDuracion(int v){duracion=v;}
 public List<Inscripcion> getInscripciones(){return inscripciones;} public void setInscripciones(List<Inscripcion> v){inscripciones=v;}
}
