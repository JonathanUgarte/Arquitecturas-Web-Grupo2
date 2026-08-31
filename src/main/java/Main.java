import factory.DAOFactory;
import factory.DBType;
import repository.MySql.MySqlConnectionManager;

import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== PRUEBA DE CONEXIÓN Y CREACIÓN DE TABLAS ===");

        // 1. Probar la conexión física con el Singleton
        try {
            Connection conn = MySqlConnectionManager.getInstance().getConnection();
            if (conn != null && !conn.isClosed()) {
                System.out.println("✔ Conexión exitosa a la base de datos.");
            } else {
                System.err.println("❌ La conexión devolvió null o está cerrada.");
                return;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al verificar el estado de la conexión: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 2. Probar la Factory y la creación de tablas a través de DBUtil
        try {
            DAOFactory factory = DAOFactory.getDAOFactory(DBType.MYSQL);

            if (factory != null) {
                System.out.println("Creando tablas en el motor seleccionado...");
                factory.createTables();
                System.out.println("✔ Estructura de base de datos verificada correctamente.");
            } else {
                System.err.println("❌ No se pudo instanciar el DAOFactory.");
            }
        } catch (Exception e) {
            System.err.println("❌ Falló la creación de las tablas:");
            e.printStackTrace();
        }
    }
}
