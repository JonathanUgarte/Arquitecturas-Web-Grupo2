package repository.MySql;

import factory.IConnectionManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlConnectionManager implements IConnectionManager {

    // 1. Instancia única privada y estática
    private static MySqlConnectionManager instance;
    private Connection connection;

    private static final String URI = "jdbc:mysql://localhost:3306/sistema_facturacion";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    // 2. Constructor privado para evitar 'new' desde fuera
    private MySqlConnectionManager() {
        try {
            // Cargar el driver de MySQL (opcional en versiones recientes de JDBC)
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URI, USER, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    // 3. Método ESTÁTICO y PÚBLICO para acceder a la instancia
    public static synchronized MySqlConnectionManager getInstance() {
        if (instance == null) {
            instance = new MySqlConnectionManager();
        }
        return instance;
    }

    // 4. Método para devolver la conexión activa
    public Connection getConnection() {
        try {
            if (this.connection == null || this.connection.isClosed()) {
                this.connection = DriverManager.getConnection(URI, USER, PASSWORD);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return this.connection;
    }


    @Override
    public void shutdown() {

    }
}
