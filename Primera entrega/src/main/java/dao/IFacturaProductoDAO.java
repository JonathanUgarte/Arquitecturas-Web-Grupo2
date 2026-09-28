package dao;

import entities.FacturaProducto;

import java.sql.SQLException;

public interface IFacturaProductoDAO {
    void insertar(FacturaProducto facturaProducto) throws SQLException;
}
