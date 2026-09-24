package repository;
import entities.Estudiante;
import entities.Genero;
import jakarta.persistence.EntityManager;
import java.util.List;

public class EstudianteRepository {

    private EntityManager em;

    public EstudianteRepository(EntityManager em) {
        this.em = em;
    }

    // Inciso a) Dar de alta un estudiante
    public void guardarEstudiante(Estudiante estudiante) {
        em.getTransaction().begin();
        em.persist(estudiante);
        em.getTransaction().commit();
    }

    // Inciso c) Recuperar todos los estudiantes y aplicar algún criterio de ordenamiento simple
    public List<Estudiante> obtenerTodosOrdenados() {
        String jpql = "SELECT e FROM Estudiante e ORDER BY e.apellido ASC, e.nombres ASC";
        return em.createQuery(jpql, Estudiante.class).getResultList();
    }

    // Inciso d) Recuperar un estudiante en base a su número de libreta universitaria
    public Estudiante obtenerPorLibreta(int libreta) { // Ajustá el tipo de dato si en tu clase es String
        String jpql = "SELECT e FROM Estudiante e WHERE e.numeroLibretaUniversitaria = :libreta";
        return em.createQuery(jpql, Estudiante.class)
                .setParameter("libreta", libreta)
                .getSingleResult();
    }

    // Inciso e) Recuperar todos los estudiantes en base a su género
    public List<Estudiante> obtenerPorGenero(Genero genero) { // Ajustá el tipo si Genero es un String y no un Enum
        String jpql = "SELECT e FROM Estudiante e WHERE e.genero = :genero";
        return em.createQuery(jpql, Estudiante.class)
                .setParameter("genero", genero)
                .getResultList();
    }

    // Inciso g) Recuperar los estudiantes de una determinada carrera, filtrados por ciudad de residencia
    public List<Estudiante> obtenerEstudiantesPorCarreraYCiudad(String nombreCarrera, String ciudad) {
        String jpql = "SELECT e FROM Estudiante e JOIN e.inscripciones i " +
                "WHERE i.carrera.nombre = :nombreCarrera AND e.ciudadResidencia = :ciudad";
        return em.createQuery(jpql, Estudiante.class)
                .setParameter("nombreCarrera", nombreCarrera)
                .setParameter("ciudad", ciudad)
                .getResultList();
    }
}