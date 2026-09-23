package repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Punto unico de acceso al EntityManagerFactory del proyecto.
 * Se crea una sola vez por aplicacion y se expone para todas las capas de persistencia.
 */
public final class JPAUtil {

    private static final EntityManagerFactory EMF =
            Persistence.createEntityManagerFactory("mysqlPU");

    private JPAUtil() {
    }

    public static EntityManager getEntityManager() {
        return EMF.createEntityManager();
    }

    public static void close() {
        EMF.close();
    }
}