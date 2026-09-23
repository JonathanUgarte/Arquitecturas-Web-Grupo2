import repository.JPAUtil;

/**
 * Smoke test del punto 1: con solo arrancar JPA, Hibernate valida el mapeo
 * de las entidades y crea/actualiza las tablas (estudiante, carrera,
 * inscripcion) en la base apuntada por persistence.xml.
 */
public class Main {

    public static void main(String[] args) {
        try (var em = JPAUtil.getEntityManager()) {
            System.out.println("JPA inicializado correctamente.");
            System.out.println("Entidades mapeadas: Estudiante, Carrera, Inscripcion.");
        } catch (Exception e) {
            System.err.println("No se pudo inicializar JPA: " + e.getMessage());
            e.printStackTrace();
            return;
        } finally {
            JPAUtil.close();
        }
    }
}