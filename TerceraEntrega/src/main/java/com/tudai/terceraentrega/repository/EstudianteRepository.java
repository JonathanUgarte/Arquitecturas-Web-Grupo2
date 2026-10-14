package com.tudai.terceraentrega.repository;

import com.tudai.terceraentrega.entities.Estudiante;
import com.tudai.terceraentrega.entities.Genero;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> {

    // d) Recuperar un estudiante por libreta universitaria.
    Optional<Estudiante> findByNumeroLibretaUniversitaria(String libreta);

    // e) Filtrar por genero. Pageable resuelve paginacion y ordenamiento.
    Page<Estudiante> findByGenero(Genero genero, Pageable pageable);

    // g) ?1 = carrera, ?2 = ciudad. Pageable resuelve paginacion y ordenamiento.
    @Query("SELECT e FROM Estudiante e " +
           "JOIN e.inscripciones i " +
           "WHERE i.carrera.nombre = ?1 " +
           "AND e.ciudadResidencia = ?2")
    Page<Estudiante> buscarPorCarreraYCiudad(
            String carrera,
            String ciudad,
            Pageable pageable
    );

    boolean existsByNumeroDocumento(String dni);

    boolean existsByNumeroLibretaUniversitaria(String libreta);
}
