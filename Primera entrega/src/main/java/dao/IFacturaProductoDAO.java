package dao;

import entities.FacturaProducto;
import entities.FacturaProductoId;

import java.sql.SQLException;
import java.util.List;

public interface IFacturaProductoDAO
        extends DAO<FacturaProducto, FacturaProductoId> {

    List<FacturaProducto> obtenerProductosPorFactura(int idFactura)
            throws SQLException;
}