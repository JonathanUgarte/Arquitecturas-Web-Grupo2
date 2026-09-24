package repository;
import entities.Carrera;
import jakarta.persistence.EntityManager;
import java.util.List;

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
}