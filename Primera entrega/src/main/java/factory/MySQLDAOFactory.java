package factory;

import dao.IClienteDAO;
import dao.IFacturaDAO;
import dao.IFacturaProductoDAO;
import dao.IProductoDAO;
import dao.Impl.MySqlClienteDAO;
import dao.Impl.MySqlFacturaDAO;
import dao.Impl.MySqlFacturaProductoDAO;
import dao.Impl.MySqlProductoDAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Factory concreta para MySQL: crea los DAO concretos y les pasa la conexión.
// Es además un Singleton, para que haya una única factory (y una única conexión).
public class MySQLDAOFactory extends DAOFactory {

    private static MySQLDAOFactory instance;
    private Connection connection;

    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String URI = "jdbc:mysql://localhost:3306/tudai";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    // Constructor privado: solo se accede mediante getInstance()
    private MySQLDAOFactory() {
    }

    public static synchronized MySQLDAOFactory getInstance() {
        if (instance == null) {
            instance = new MySQLDAOFactory();
        }
        return instance;
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName(DRIVER);
            } catch (ClassNotFoundException e) {
                throw new SQLException("No se encontró el driver de MySQL", e);
            }
            connection = DriverManager.getConnection(URI, USER, PASSWORD);
        }
        return connection;
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ----- Esquema -----

    private void execute(String sql) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.execute();
        }
    }

    @Override
    public void dropTables() throws SQLException {
        execute("DROP TABLE IF EXISTS factura_producto");
        execute("DROP TABLE IF EXISTS factura");
        execute("DROP TABLE IF EXISTS producto");
        execute("DROP TABLE IF EXISTS cliente");
    }

    @Override
    public void createTables() throws SQLException {
        execute("CREATE TABLE IF NOT EXISTS cliente (" +
                "idCliente INT AUTO_INCREMENT PRIMARY KEY, " +
                "nombre VARCHAR(500) NOT NULL, " +
                "email VARCHAR(150))");

        execute("CREATE TABLE IF NOT EXISTS producto (" +
                "idProducto INT AUTO_INCREMENT PRIMARY KEY, " +
                "nombre VARCHAR(45) NOT NULL, " +
                "valor FLOAT NOT NULL)");

        execute("CREATE TABLE IF NOT EXISTS factura (" +
                "idFactura INT AUTO_INCREMENT PRIMARY KEY, " +
                "idCliente INT NOT NULL, " +
                "fecha DATE, " +
                "FOREIGN KEY (idCliente) REFERENCES cliente(idCliente))");

        execute("CREATE TABLE IF NOT EXISTS factura_producto (" +
                "idFactura INT NOT NULL, " +
                "idProducto INT NOT NULL, " +
                "cantidad INT NOT NULL, " +
                "PRIMARY KEY (idFactura, idProducto), " +
                "FOREIGN KEY (idFactura) REFERENCES factura(idFactura), " +
                "FOREIGN KEY (idProducto) REFERENCES producto(idProducto))");
    }

    // ----- DAOs -----

    @Override
    public IClienteDAO getClienteDAO() throws SQLException {
        return new MySqlClienteDAO(getConnection());
    }

    @Override
    public IProductoDAO getProductoDAO() throws SQLException {
        return new MySqlProductoDAO(getConnection());
    }

    @Override
    public IFacturaDAO getFacturaDAO() throws SQLException {
        return new MySqlFacturaDAO(getConnection());
    }

    @Override
    public IFacturaProductoDAO getFacturaProductoDAO() throws SQLException {
        return new MySqlFacturaProductoDAO(getConnection());
    }
}
