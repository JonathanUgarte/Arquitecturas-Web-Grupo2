package repository.MySql;

import factory.IConnectionManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class MySqlConnectionManager implements IConnectionManager {

    private static final String URL = "jdbc:mysql://localhost:3306/sistema_facturacion";

    private static final String USER = "root";

    private static final String PASSWORD = "password";

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                URL,
                USER,
                PASSWORD
        );

    }

    @Override
    public void shutdown() {

    }
}
