package com.tudai.terceraentrega.repository;
import com.tudai.terceraentrega.entities.*; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface InscripcionRepository extends JpaRepository<Inscripcion,InscripcionId> { List<Inscripcion> findAllByOrderByCarreraNombreAscAnioInscripcionAsc(); }
