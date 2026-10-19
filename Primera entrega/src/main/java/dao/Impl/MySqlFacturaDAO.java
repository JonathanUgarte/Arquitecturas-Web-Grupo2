package dao.Impl;

import dao.IFacturaDAO;
import entities.Factura;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MySqlFacturaDAO implements IFacturaDAO {

    private final Connection conn;

    public MySqlFacturaDAO(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void insertar(Factura factura) throws SQLException {
        String sql = "INSERT INTO factura (idFactura, idCliente) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE idCliente = VALUES(idCliente)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, factura.getIdFactura());
            ps.setInt(2, factura.getIdCliente());
            ps.executeUpdate();
        }
    }

    @Override
    public Factura getById(Integer id) throws SQLException {
        String sql = "SELECT idFactura, idCliente FROM factura WHERE idFactura = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    @Override
    public List<Factura> getAll() throws SQLException {
        List<Factura> facturas = new ArrayList<>();
        String sql = "SELECT idFactura, idCliente FROM factura";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                facturas.add(mapear(rs));
            }
        }
        return facturas;
    }

    @Override
    public void actualizar(Factura factura) throws SQLException {
        String sql = "UPDATE factura SET idCliente = ? WHERE idFactura = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, factura.getIdCliente());
            ps.setInt(2, factura.getIdFactura());
            ps.executeUpdate();
        }
    }

    @Override
    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM factura WHERE idFactura = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Factura> obtenerFacturasPorCliente(int idCliente)
            throws SQLException {

        List<Factura> facturas = new ArrayList<>();

        String sql = "SELECT idFactura, idCliente " +
                "FROM factura WHERE idCliente = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    facturas.add(mapear(rs));
                }
            }
        }

        return facturas;
    }

    private Factura mapear(ResultSet rs) throws SQLException {
        return new Factura(rs.getInt("idFactura"), rs.getInt("idCliente"));
    }
}
