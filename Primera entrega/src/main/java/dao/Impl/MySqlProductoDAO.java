package dao.Impl;

import dao.IProductoDAO;
import entities.Producto;
import entities.dto.ProductoDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySqlProductoDAO implements IProductoDAO {


    private Connection conn;

    public MySqlProductoDAO(Connection conn){
        this.conn = conn;
    }

    public void insertar(Producto producto) throws SQLException{
        String sql = "INSERT INTO producto (idProducto, nombre, valor) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), valor = VALUES(valor);";

        try(PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, producto.getIdProducto());
            ps.setString(2, producto.getNombre());
            ps.setFloat(3, producto.getValor());

            ps.executeUpdate();
        }
    }

    public ProductoDTO getProductoMasRecaudador(){
        ProductoDTO productoTop = null;
        String query = "SELECT p.idProducto, p.nombre, p.valor, SUM(fp.cantidad * p.valor) AS recaudacion " +
                        "FROM producto p " +
                        "JOIN factura_producto fp ON p.idProducto = fp.idProducto " +
                        "GROUP BY p.idProducto, p.nombre, p.valor " +
                        "ORDER BY recaudacion DESC " +
                        "LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                productoTop = new ProductoDTO(
                        rs.getInt("idProducto"),
                        rs.getString("nombre"),
                        rs.getFloat("valor"),
                        rs.getDouble("recaudacion")

                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return productoTop;
    }
}
