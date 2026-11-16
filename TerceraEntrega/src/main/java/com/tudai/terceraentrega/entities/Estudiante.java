package com.tudai.terceraentrega.entities;
import jakarta.persistence.*;
import java.util.*;
@Entity @Table(name="estudiante")
public class Estudiante {
 @Id @Column(name="id_estudiante") private int idEstudiante;
 @Column(nullable=false) private String nombres;
 @Column(nullable=false) private String apellido;
 @Column(nullable=false) private int edad;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Genero genero;
 @Column(name="numero_documento",nullable=false,unique=true) private String numeroDocumento;
 @Column(name="ciudad_residencia",nullable=false) private String ciudadResidencia;
 @Column(name="numero_libreta_universitaria",nullable=false,unique=true) private String numeroLibretaUniversitaria;
 @OneToMany(mappedBy="estudiante") private List<Inscripcion> inscripciones=new ArrayList<>();
 public Estudiante(){}
 public int getIdEstudiante(){return idEstudiante;} public void setIdEstudiante(int v){idEstudiante=v;}
 public String getNombres(){return nombres;} public void setNombres(String v){nombres=v;}
 public String getApellido(){return apellido;} public void setApellido(String v){apellido=v;}
 public int getEdad(){return edad;} public void setEdad(int v){edad=v;}
 public Genero getGenero(){return genero;} public void setGenero(Genero v){genero=v;}
 public String getNumeroDocumento(){return numeroDocumento;} public void setNumeroDocumento(String v){numeroDocumento=v;}
 public String getCiudadResidencia(){return ciudadResidencia;} public void setCiudadResidencia(String v){ciudadResidencia=v;}
 public String getNumeroLibretaUniversitaria(){return numeroLibretaUniversitaria;} public void setNumeroLibretaUniversitaria(String v){numeroLibretaUniversitaria=v;}
 public List<Inscripcion> getInscripciones(){return inscripciones;} public void setInscripciones(List<Inscripcion> v){inscripciones=v;}
}
