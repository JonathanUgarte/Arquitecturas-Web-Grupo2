package dao;

import entities.Factura;
import java.sql.SQLException;
import java.util.List;

public interface IFacturaDAO extends DAO<Factura, Integer> {

    List<Factura> obtenerFacturasPorCliente(int idCliente)
            throws SQLException;
}