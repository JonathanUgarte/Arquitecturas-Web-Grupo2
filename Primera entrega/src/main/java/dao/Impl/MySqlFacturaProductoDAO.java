package dao.Impl;

import dao.IFacturaProductoDAO;
import entities.FacturaProducto;
import entities.FacturaProductoId;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MySqlFacturaProductoDAO implements IFacturaProductoDAO {

    private final Connection conn;

    public MySqlFacturaProductoDAO(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void insertar(FacturaProducto fp) throws SQLException {
        String sql = "INSERT INTO factura_producto (idFactura, idProducto, cantidad) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE cantidad = VALUES(cantidad)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fp.getIdFactura());
            ps.setInt(2, fp.getIdProducto());
            ps.setInt(3, fp.getCantidad());
            ps.executeUpdate();
        }
    }

    @Override
    public FacturaProducto getById(FacturaProductoId id) throws SQLException {
        String sql = "SELECT idFactura, idProducto, cantidad FROM factura_producto " +
                     "WHERE idFactura = ? AND idProducto = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id.idFactura());
            ps.setInt(2, id.idProducto());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    @Override
    public List<FacturaProducto> getAll() throws SQLException {
        List<FacturaProducto> lista = new ArrayList<>();
        String sql = "SELECT idFactura, idProducto, cantidad FROM factura_producto";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    @Override
    public void actualizar(FacturaProducto fp) throws SQLException {
        String sql = "UPDATE factura_producto SET cantidad = ? WHERE idFactura = ? AND idProducto = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fp.getCantidad());
            ps.setInt(2, fp.getIdFactura());
            ps.setInt(3, fp.getIdProducto());
            ps.executeUpdate();
        }
    }

    @Override
    public void eliminar(FacturaProductoId id) throws SQLException {
        String sql = "DELETE FROM factura_producto WHERE idFactura = ? AND idProducto = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id.idFactura());
            ps.setInt(2, id.idProducto());
            ps.executeUpdate();
        }
    }

    @Override
    public List<FacturaProducto> obtenerProductosPorFactura(int idFactura)
            throws SQLException {

        List<FacturaProducto> productos = new ArrayList<>();

        String sql = "SELECT idFactura, idProducto, cantidad " +
                "FROM factura_producto WHERE idFactura = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idFactura);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapear(rs));
                }
            }
        }

        return productos;
    }

    private FacturaProducto mapear(ResultSet rs) throws SQLException {
        return new FacturaProducto(rs.getInt("idFactura"), rs.getInt("idProducto"), rs.getInt("cantidad"));
    }
}
