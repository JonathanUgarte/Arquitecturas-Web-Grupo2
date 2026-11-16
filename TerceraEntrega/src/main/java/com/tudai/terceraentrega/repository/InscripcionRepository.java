package com.tudai.terceraentrega.repository;

import com.tudai.terceraentrega.entities.Inscripcion;
import com.tudai.terceraentrega.entities.InscripcionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, InscripcionId> {

    // h) Inscriptos por carrera y anio. Se navega con "." (i.carrera.nombre) en lugar de hacer JOIN explicito.
    @Query("SELECT i.carrera.nombre AS carrera, i.anioInscripcion AS anio, COUNT(i) AS cantidad " +
           "FROM Inscripcion i " +
           "GROUP BY i.carrera.nombre, i.anioInscripcion " +
           "ORDER BY i.carrera.nombre ASC, i.anioInscripcion ASC")
    List<ConteoAnualProjection> contarInscriptosPorCarreraYAnio();

    // h) Egresados por carrera y anio de egreso.
    @Query("SELECT i.carrera.nombre AS carrera, i.anioEgreso AS anio, COUNT(i) AS cantidad " +
           "FROM Inscripcion i " +
           "WHERE i.anioEgreso IS NOT NULL " +
           "GROUP BY i.carrera.nombre, i.anioEgreso " +
           "ORDER BY i.carrera.nombre ASC, i.anioEgreso ASC")
    List<ConteoAnualProjection> contarEgresadosPorCarreraYAnio();
}
