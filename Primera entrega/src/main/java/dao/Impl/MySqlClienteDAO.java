package dao.Impl;

import dao.IClienteDAO;
import entities.Cliente;
import entities.dto.ClienteDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MySqlClienteDAO implements IClienteDAO {

    private final Connection conn;

    public MySqlClienteDAO(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void insertar(Cliente cliente) throws SQLException {
        String sql = "INSERT INTO cliente (idCliente, nombre, email) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), email = VALUES(email)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cliente.getIdCliente());
            ps.setString(2, cliente.getNombre());
            ps.setString(3, cliente.getEmail());
            ps.executeUpdate();
        }
    }

    @Override
    public Cliente getById(Integer id) throws SQLException {
        String sql = "SELECT idCliente, nombre, email FROM cliente WHERE idCliente = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    @Override
    public List<Cliente> getAll() throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT idCliente, nombre, email FROM cliente";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clientes.add(mapear(rs));
            }
        }
        return clientes;
    }

    @Override
    public void actualizar(Cliente cliente) throws SQLException {
        String sql = "UPDATE cliente SET nombre = ?, email = ? WHERE idCliente = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getEmail());
            ps.setInt(3, cliente.getIdCliente());
            ps.executeUpdate();
        }
    }

    @Override
    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM cliente WHERE idCliente = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<ClienteDTO> getClientesOrdenadosPorFacturacion() throws SQLException {
        List<ClienteDTO> clientes = new ArrayList<>();

        String query = "SELECT c.idCliente, c.nombre, c.email, SUM(fp.cantidad * p.valor) AS totalFacturado " +
                       "FROM cliente c " +
                       "JOIN factura f ON c.idCliente = f.idCliente " +
                       "JOIN factura_producto fp ON f.idFactura = fp.idFactura " +
                       "JOIN producto p ON fp.idProducto = p.idProducto " +
                       "GROUP BY c.idCliente, c.nombre, c.email " +
                       "ORDER BY totalFacturado DESC";

        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clientes.add(new ClienteDTO(
                        rs.getInt("idCliente"),
                        rs.getString("nombre"),
                        rs.getString("email"),
                        rs.getDouble("totalFacturado")
                ));
            }
        }
        return clientes;
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(rs.getInt("idCliente"), rs.getString("nombre"), rs.getString("email"));
    }
}
