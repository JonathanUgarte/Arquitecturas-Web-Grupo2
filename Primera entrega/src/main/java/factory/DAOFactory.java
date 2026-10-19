package factory;

import dao.IClienteDAO;
import dao.IFacturaDAO;
import dao.IFacturaProductoDAO;
import dao.IProductoDAO;

import java.sql.SQLException;

public abstract class DAOFactory {

    // Métodos Factory para los DAO
    public abstract IClienteDAO getClienteDAO() throws SQLException;
    public abstract IFacturaDAO getFacturaDAO() throws SQLException;
    public abstract IProductoDAO getProductoDAO() throws SQLException;
    public abstract IFacturaProductoDAO getFacturaProductoDAO() throws SQLException;

    // El esquema (DDL) depende del motor, por eso lo resuelve cada factory concreta
    public abstract void dropTables() throws SQLException;
    public abstract void createTables() throws SQLException;

    public abstract void shutdown();

    // Selector de Factory
    public static DAOFactory getDAOFactory(DBType whichFactory) {
        switch (whichFactory) {
            case MYSQL:
                return MySQLDAOFactory.getInstance();
            // Se pueden agregar otros motores como POSTGRESQL, H2, etc.
            default:
                throw new IllegalArgumentException("Motor de base de datos no soportado: " + whichFactory);
        }
    }
}
