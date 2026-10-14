package repository;

import dto.CarreraDTO;
import dto.ReporteCarreraDTO;
import entities.Inscripcion;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CarreraRepository {

    private final EntityManager em;

    public CarreraRepository(EntityManager em) {
        this.em = em;
    }

    // Inciso f) Carreras con estudiantes inscriptos, ordenadas por cantidad.
    // Devuelve DTO para no exponer la entidad Carrera fuera del repository.
    public List<CarreraDTO> obtenerCarrerasConInscriptosOrdenadas() {
        String jpql = "SELECT new dto.CarreraDTO(c.nombre, COUNT(i)) " +
                "FROM Carrera c JOIN c.inscripciones i " +
                "GROUP BY c.idCarrera, c.nombre " +
                "ORDER BY COUNT(i) DESC, c.nombre ASC";

        return em.createQuery(jpql, CarreraDTO.class).getResultList();
    }

    // Punto 3) Reporte por carrera y por anio.
    public List<ReporteCarreraDTO> generarReporteCarreras() {
        String jpql = "SELECT i FROM Inscripcion i JOIN FETCH i.carrera c ORDER BY c.nombre ASC";
        List<Inscripcion> inscripciones = em.createQuery(jpql, Inscripcion.class).getResultList();

        // LinkedHashMap conserva el orden alfabetico de carreras de la consulta.
        // TreeMap mantiene los anios en orden cronologico.
        Map<String, Map<Integer, ReporteCarreraDTO>> reporteMap = new LinkedHashMap<>();

        for (Inscripcion i : inscripciones) {
            String carrera = i.getCarrera().getNombre();
            reporteMap.putIfAbsent(carrera, new TreeMap<>());
            Map<Integer, ReporteCarreraDTO> aniosMap = reporteMap.get(carrera);

            int anioInscripcion = i.getAnioInscripcion();
            aniosMap.putIfAbsent(anioInscripcion, new ReporteCarreraDTO(carrera, anioInscripcion));
            aniosMap.get(anioInscripcion).sumarInscripto();

            if (i.getAnioEgreso() != null) {
                int anioEgreso = i.getAnioEgreso();
                aniosMap.putIfAbsent(anioEgreso, new ReporteCarreraDTO(carrera, anioEgreso));
                aniosMap.get(anioEgreso).sumarEgresado();
            }
        }

        List<ReporteCarreraDTO> resultado = new ArrayList<>();
        for (Map<Integer, ReporteCarreraDTO> anios : reporteMap.values()) {
            resultado.addAll(anios.values());
        }
        return resultado;
    }
}
