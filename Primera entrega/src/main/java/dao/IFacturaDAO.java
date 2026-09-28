package dao;

import entities.Factura;

import java.sql.SQLException;

public interface IFacturaDAO {
    void insertar(Factura ff) throws SQLException;
}
