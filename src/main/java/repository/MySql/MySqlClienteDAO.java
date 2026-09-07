package repository.MySql;

import dao.IClienteDAO;
import entities.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MySqlClienteDAO implements IClienteDAO {

    public Map<Cliente, Double> getClientesOrdenadosPorFacturacion() {
        Map<Cliente, Double> clientesConFacturacion = new LinkedHashMap<>();

        String query = "SELECT c.idCliente, c.nombre, c.email, SUM(fp.cantidad * p.valor) AS totalFacturado " +
                "FROM cliente c " +
                "JOIN factura f ON c.idCliente = f.idCliente " +
                "JOIN factura_producto fp ON f.idFactura = fp.idFactura " +
                "JOIN producto p ON fp.idProducto = p.idProducto " +
                "GROUP BY c.idCliente, c.nombre, c.email " +
                "ORDER BY totalFacturado DESC";

        try (Connection conn = MySqlConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Cliente c = new Cliente(
                        rs.getInt("idCliente"),
                        rs.getString("nombre"),
                        rs.getString("email")
                );
                double total = rs.getDouble("totalFacturado");

                // Guardamos el cliente y su total en el Map
                clientesConFacturacion.put(c, total);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return clientesConFacturacion;
    }
}