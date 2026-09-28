package dao;

import entities.Producto;
import entities.dto.ProductoDTO;

import java.sql.SQLException;

public interface IProductoDAO {
    void insertar(Producto producto) throws SQLException;
    ProductoDTO getProductoMasRecaudador();
}
