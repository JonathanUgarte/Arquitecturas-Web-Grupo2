package repository.MySql;

import factory.DAOFactory;
import dao.IClienteDAO;
import dao.IFacturaDAO;
import dao.IProductoDAO;

public class MySqlDAOFactory extends DAOFactory {

    @Override
    public IClienteDAO getClienteDAO() {
        return new MySqlClienteDAO();
    }

    @Override
    public IFacturaDAO getFacturaDAO() {
        return new MySqlFacturaDAO();
    }

    @Override
    public IProductoDAO getProductoDAO() {
        return new MySqlProductoDAO();
    }

    @Override
    public void createTables() {
        // Simplemente delega en el dbUtil que ya sabe abrir su propia conexión
        dbUtil.createTables();
    }
}