package repository;

import dto.EstudianteDTO;
import entities.Estudiante;
import entities.Genero;
import jakarta.persistence.EntityManager;

import java.util.List;

public class EstudianteRepository {

    private final EntityManager em;

    public EstudianteRepository(EntityManager em) {
        this.em = em;
    }

    // Inciso a) Dar de alta un estudiante.
    // Recibe la entidad porque esta operacion debe persistirla.
    public void guardarEstudiante(Estudiante estudiante) {
        em.getTransaction().begin();
        em.persist(estudiante);
        em.getTransaction().commit();
    }

    // Inciso c) Recuperar todos los estudiantes ordenados por apellido y nombre.
    public List<EstudianteDTO> obtenerTodosOrdenados() {
        String jpql = "SELECT e FROM Estudiante e ORDER BY e.apellido ASC, e.nombres ASC";
        List<Estudiante> estudiantes = em.createQuery(jpql, Estudiante.class).getResultList();
        return estudiantes.stream().map(this::convertirADTO).toList();
    }

    // Inciso d) Recuperar un estudiante por numero de libreta universitaria.
    public EstudianteDTO obtenerPorLibreta(String libreta) {
        String jpql = "SELECT e FROM Estudiante e WHERE e.numeroLibretaUniversitaria = :libreta";
        Estudiante estudiante = em.createQuery(jpql, Estudiante.class)
                .setParameter("libreta", libreta)
                .getSingleResult();
        return convertirADTO(estudiante);
    }

    // Inciso e) Recuperar todos los estudiantes por genero.
    public List<EstudianteDTO> obtenerPorGenero(Genero genero) {
        String jpql = "SELECT e FROM Estudiante e WHERE e.genero = :genero " +
                "ORDER BY e.apellido ASC, e.nombres ASC";
        List<Estudiante> estudiantes = em.createQuery(jpql, Estudiante.class)
                .setParameter("genero", genero)
                .getResultList();
        return estudiantes.stream().map(this::convertirADTO).toList();
    }

    // Inciso g) Recuperar estudiantes de una carrera filtrados por ciudad.
    public List<EstudianteDTO> obtenerEstudiantesPorCarreraYCiudad(String nombreCarrera, String ciudad) {
        String jpql = "SELECT e FROM Estudiante e JOIN e.inscripciones i " +
                "WHERE i.carrera.nombre = :nombreCarrera " +
                "AND e.ciudadResidencia = :ciudad " +
                "ORDER BY e.apellido ASC, e.nombres ASC";

        List<Estudiante> estudiantes = em.createQuery(jpql, Estudiante.class)
                .setParameter("nombreCarrera", nombreCarrera)
                .setParameter("ciudad", ciudad)
                .getResultList();

        return estudiantes.stream().map(this::convertirADTO).toList();
    }

    // La conversion queda centralizada en el repository.
    private EstudianteDTO convertirADTO(Estudiante estudiante) {
        return new EstudianteDTO(
                estudiante.getNombres(),
                estudiante.getApellido(),
                estudiante.getEdad(),
                estudiante.getGenero().toString()
        );
    }
}
