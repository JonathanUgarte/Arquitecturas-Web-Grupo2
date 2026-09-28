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

    // Selector de Factory
    public static DAOFactory getDAOFactory(DBType whichFactory) {
        switch (whichFactory) {
            case MYSQL:
                return MySQLDAOFactory.getInstance();
            // Puedes agregar otros motores como POSTGRESQL, H2, etc.
            default:
                return null;
        }
    }
}
