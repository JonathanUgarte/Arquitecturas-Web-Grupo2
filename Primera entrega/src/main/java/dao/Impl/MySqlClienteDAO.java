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


    public MySqlClienteDAO(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void insertar(Cliente cliente) throws SQLException {
        String sql = "INSERT INTO cliente (idCliente, nombre, email) VALUES (?, ?, ?) " +

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cliente.getIdCliente());
            ps.setString(2, cliente.getNombre());
            ps.setString(3, cliente.getEmail());

            ps.executeUpdate();
        }
    }

    @Override
    public List<ClienteDTO> getClientesOrdenadosPorFacturacion() throws SQLException {

        String query = "SELECT c.idCliente, c.nombre, c.email, SUM(fp.cantidad * p.valor) AS totalFacturado " +
                       "FROM cliente c " +
                       "JOIN factura f ON c.idCliente = f.idCliente " +
                       "JOIN factura_producto fp ON f.idFactura = fp.idFactura " +
                       "JOIN producto p ON fp.idProducto = p.idProducto " +
                       "GROUP BY c.idCliente, c.nombre, c.email " +
                       "ORDER BY totalFacturado DESC";

            while (rs.next()) {
                        rs.getInt("idCliente"),
                        rs.getString("nombre"),
                        rs.getString("email"),
                        rs.getDouble("totalFacturado")
        }
    }
    }
}