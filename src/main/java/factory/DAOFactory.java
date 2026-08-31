package factory;

import dao.IClienteDAO;
import dao.IFacturaDAO;
import dao.IProductoDAO;

public abstract class DAOFactory {

    // Métodos Factory para los DAO
    public abstract IClienteDAO getClienteDAO();
    public abstract IFacturaDAO getFacturaDAO();
    public abstract IProductoDAO getProductoDAO();

    // Método para inicializar el esquema de tablas en la base de datos
    public abstract void createTables();

    // Selector de Factory
    public static DAOFactory getDAOFactory(DBType whichFactory) {
        switch (whichFactory) {
            case MYSQL:
                return new repository.MySql.MySqlDAOFactory();
            // Puedes agregar otros motores como POSTGRESQL, H2, etc.
            default:
                return null;
        }
    }
}
