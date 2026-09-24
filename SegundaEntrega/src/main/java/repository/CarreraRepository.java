package repository;
import entities.Carrera;
import jakarta.persistence.EntityManager;
import dto.ReporteCarreraDTO;
import entities.Inscripcion;
import java.util.*;

public class CarreraRepository {

    private EntityManager em;

    public CarreraRepository(EntityManager em) {
        this.em = em;
    }

    // Inciso f) Recuperar las carreras con estudiantes inscriptos y ordenarlas por cantidad de inscriptos
    public List<Carrera> obtenerCarrerasConInscriptosOrdenadas() {
        String jpql = "SELECT c FROM Carrera c JOIN c.inscripciones i " +
                "GROUP BY c " +
                "ORDER BY COUNT(i.estudiante) DESC";
        return em.createQuery(jpql, Carrera.class).getResultList();
    }
    public List<ReporteCarreraDTO> generarReporteCarreras() {
        // 1. Traemos todas las inscripciones junto con su carrera, ordenadas alfabéticamente
        String jpql = "SELECT i FROM Inscripcion i JOIN FETCH i.carrera c ORDER BY c.nombre ASC";
        List<Inscripcion> inscripciones = em.createQuery(jpql, Inscripcion.class).getResultList();

        // 2. Estructura para agrupar: Map<NombreCarrera, Map<Anio, ReporteDTO>>
        Map<String, Map<Integer, ReporteCarreraDTO>> reporteMap = new LinkedHashMap<>();

        for (Inscripcion i : inscripciones) {
            String carrera = i.getCarrera().getNombre();

            // Si la carrera no existe en el mapa, la agregamos con un TreeMap para ordenar los años
            reporteMap.putIfAbsent(carrera, new TreeMap<>());
            Map<Integer, ReporteCarreraDTO> aniosMap = reporteMap.get(carrera);

            // A) Contabilizar la inscripción (Ingreso)
            int anioInscripcion = i.getAnioInscripcion();
            aniosMap.putIfAbsent(anioInscripcion, new ReporteCarreraDTO(carrera, anioInscripcion));
            aniosMap.get(anioInscripcion).sumarInscripto();

            // B) Contabilizar el egreso (solo si el estudiante se graduó)
            if (i.getAnioEgreso() != null) {
                int anioEgreso = i.getAnioEgreso();
                aniosMap.putIfAbsent(anioEgreso, new ReporteCarreraDTO(carrera, anioEgreso));
                aniosMap.get(anioEgreso).sumarEgresado();
            }
        }

        // 3. Aplanar los mapas a una sola lista para retornarla limpia
        List<ReporteCarreraDTO> resultadoFinal = new ArrayList<>();
        for (Map<Integer, ReporteCarreraDTO> anios : reporteMap.values()) {
            resultadoFinal.addAll(anios.values());
        }

        return resultadoFinal;
    }
}