package repository;
import entities.Carrera;
import entities.Estudiante;
import entities.Inscripcion;
import entities.InscripcionId;
import jakarta.persistence.EntityManager;
import java.time.Year;

public class InscripcionRepository {

    private EntityManager em;

    public InscripcionRepository(EntityManager em) {
        this.em = em;
    }

    // Inciso b) Matricular un estudiante en una carrera
    public void matricularEstudiante(Estudiante estudiante, Carrera carrera) {
        // 1. Obtenemos el año actual
        int anioActual = Year.now().getValue();

        // 2. Usamos el constructor de tu clase, pasando 'null' al año de egreso
        Inscripcion inscripcion = new Inscripcion(estudiante, carrera, anioActual, null);

        // 3. Persistimos en la base de datos
        em.getTransaction().begin();
        em.persist(inscripcion);
        em.getTransaction().commit();
    }

}