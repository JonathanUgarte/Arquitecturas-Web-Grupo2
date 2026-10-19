package dao.Impl;

import dao.IFacturaProductoDAO;
import entities.FacturaProducto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySqlFacturaProductoDAO implements IFacturaProductoDAO {

    private Connection conn;

    public MySqlFacturaProductoDAO(Connection conn){
        this.conn = conn;
    }

    @Override
    public void insertar(FacturaProducto facProd) throws SQLException {
        String sql = "INSERT INTO factura_producto (idFactura, idProducto, cantidad) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE cantidad = VALUES(cantidad)";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, facProd.getIdFactura());
            stmt.setInt(2, facProd.getIdProducto());
            stmt.setInt(3, facProd.getCantidad());
            stmt.executeUpdate();
        }
    }
}
