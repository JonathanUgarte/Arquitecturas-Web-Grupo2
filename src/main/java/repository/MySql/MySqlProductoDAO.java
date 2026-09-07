package repository.MySql;

import dao.IProductoDAO;
import entities.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySqlProductoDAO implements IProductoDAO {

    public Producto getProductoMasRecaudador() {
        Producto productoTop = null;
        String query = "SELECT p.idProducto, p.nombre, p.valor, SUM(fp.cantidad * p.valor) AS recaudacion " +
                "FROM producto p " +
                "JOIN factura_producto fp ON p.idProducto = fp.idProducto " +
                "GROUP BY p.idProducto, p.nombre, p.valor " +
                "ORDER BY recaudacion DESC " +
                "LIMIT 1";

        try (Connection conn = MySqlConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                productoTop = new Producto(
                        rs.getInt("idProducto"),
                        rs.getString("nombre"),
                        rs.getFloat("valor")

                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return productoTop;
    }
}
