package dao.Impl;

import dao.IProductoDAO;
import entities.Producto;
import entities.dto.ProductoDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MySqlProductoDAO implements IProductoDAO {

    private final Connection conn;

    public MySqlProductoDAO(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void insertar(Producto producto) throws SQLException {
        String sql = "INSERT INTO producto (idProducto, nombre, valor) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), valor = VALUES(valor)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, producto.getIdProducto());
            ps.setString(2, producto.getNombre());
            ps.setFloat(3, producto.getValor());
            ps.executeUpdate();
        }
    }

    @Override
    public Producto getById(Integer id) throws SQLException {
        String sql = "SELECT idProducto, nombre, valor FROM producto WHERE idProducto = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    @Override
    public List<Producto> getAll() throws SQLException {
        List<Producto> productos = new ArrayList<>();
        String sql = "SELECT idProducto, nombre, valor FROM producto";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(mapear(rs));
            }
        }
        return productos;
    }

    @Override
    public void actualizar(Producto producto) throws SQLException {
        String sql = "UPDATE producto SET nombre = ?, valor = ? WHERE idProducto = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, producto.getNombre());
            ps.setFloat(2, producto.getValor());
            ps.setInt(3, producto.getIdProducto());
            ps.executeUpdate();
        }
    }

    @Override
    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM producto WHERE idProducto = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public ProductoDTO getProductoMasRecaudador() throws SQLException {
        String query = "SELECT p.idProducto, p.nombre, p.valor, SUM(fp.cantidad * p.valor) AS recaudacion " +
                       "FROM producto p " +
                       "JOIN factura_producto fp ON p.idProducto = fp.idProducto " +
                       "GROUP BY p.idProducto, p.nombre, p.valor " +
                       "ORDER BY recaudacion DESC " +
                       "LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new ProductoDTO(
                        rs.getInt("idProducto"),
                        rs.getString("nombre"),
                        rs.getFloat("valor"),
                        rs.getDouble("recaudacion")
                );
            }
        }
        return null;
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(rs.getInt("idProducto"), rs.getString("nombre"), rs.getFloat("valor"));
    }
}
