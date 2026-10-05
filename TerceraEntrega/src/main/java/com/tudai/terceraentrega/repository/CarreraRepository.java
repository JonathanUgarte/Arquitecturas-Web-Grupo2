package com.tudai.terceraentrega.repository;
import com.tudai.terceraentrega.entities.Carrera; import com.tudai.terceraentrega.dto.CarreraDTO; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface CarreraRepository extends JpaRepository<Carrera,Integer> {
 Optional<Carrera> findByNombre(String nombre);
 @Query("SELECT new com.tudai.terceraentrega.dto.CarreraDTO(c.nombre, COUNT(i)) FROM Carrera c JOIN c.inscripciones i GROUP BY c.idCarrera,c.nombre ORDER BY COUNT(i) DESC,c.nombre ASC") List<CarreraDTO> carrerasConInscriptos();
}
