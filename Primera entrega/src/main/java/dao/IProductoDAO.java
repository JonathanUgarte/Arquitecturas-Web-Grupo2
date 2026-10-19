package dao;

import entities.Producto;
import entities.dto.ProductoDTO;

import java.sql.SQLException;

public interface IProductoDAO extends DAO<Producto, Integer> {
    // Consulta específica: producto que más recaudó
    ProductoDTO getProductoMasRecaudador() throws SQLException;
}
