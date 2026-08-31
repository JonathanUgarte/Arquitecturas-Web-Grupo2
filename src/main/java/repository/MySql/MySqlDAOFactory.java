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
        // Delegamos la creación física a DBUtil
        dbUtil.createTables();
    }
}
