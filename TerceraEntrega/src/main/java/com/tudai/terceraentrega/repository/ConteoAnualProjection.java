package com.tudai.terceraentrega.repository;

// Proyeccion para los conteos por carrera y anio del reporte (h)
public interface ConteoAnualProjection {
    String getCarrera();
    Integer getAnio();
    Long getCantidad();
}
