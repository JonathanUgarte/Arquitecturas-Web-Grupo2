package repository.MySql;

import factory.IConnectionManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySqlConnectionManager implements IConnectionManager {

    private static MySqlConnectionManager instance;
    private Connection connection;

    private static final String URI = "jdbc:mysql://localhost:3306/sistema_facturacion";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    private MySqlConnectionManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URI, USER, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized MySqlConnectionManager getInstance() {
        if (instance == null) {
            instance = new MySqlConnectionManager();
        }
        return instance;
    }

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
