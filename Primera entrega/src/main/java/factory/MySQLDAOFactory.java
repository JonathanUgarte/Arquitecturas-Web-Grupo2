package factory;

import dao.IClienteDAO;
import dao.IFacturaDAO;
import dao.IFacturaProductoDAO;
import dao.IProductoDAO;
import dao.Impl.MySqlFacturaProductoDAO;
import dao.Impl.MySqlClienteDAO;
import dao.Impl.MySqlFacturaDAO;
import dao.Impl.MySqlProductoDAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


//Movemos a factory para usar el patron dao, instanciamos casos concretos y les pasamos la conexion
public class MySQLDAOFactory extends DAOFactory {

    private static MySQLDAOFactory instance;
    private static Connection connection;

    //cambiar password a password y tudai por sistema_facturacion
    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String URI = "jdbc:mysql://localhost:3306/tudai";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // constructor privado para que solo sea accedida la instancia (Singleton).
    private MySQLDAOFactory() {
    }

    public static synchronized MySQLDAOFactory getInstance() {
        if (instance == null) {
            instance = new MySQLDAOFactory();
        }
        return instance;
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName(DRIVER);
                connection = DriverManager.getConnection(URI, USER, PASSWORD);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    //@Override
    public void shutdown() {
        try {
            if (connection != null || connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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
