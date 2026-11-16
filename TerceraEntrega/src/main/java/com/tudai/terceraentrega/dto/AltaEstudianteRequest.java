package com.tudai.terceraentrega.dto;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class AltaEstudianteRequest { private String nombres; private String apellido; private int edad; private String genero; private String numeroDocumento; private String ciudadResidencia; private String numeroLibretaUniversitaria; }
