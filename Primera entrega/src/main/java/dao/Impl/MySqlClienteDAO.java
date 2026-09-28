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

    private Connection conn;

    public MySqlClienteDAO(Connection conn){
        this.conn = conn;
    }

    @Override
    public void insertar(Cliente cliente) throws SQLException {
        String sql = "INSERT INTO cliente (idCliente, nombre, email) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), email = VALUES(email);";

            try(PreparedStatement ps = conn.prepareStatement(sql)){
                ps.setInt(1, cliente.getIdCliente());
                ps.setString(2, cliente.getNombre());
                ps.setString(3, cliente.getEmail());

                ps.executeUpdate();
            }
    }
    //El mapeo por Clave funciona pero es mas sencillo trabajar con una lista de clienteDTO

//    public Map<ClienteDTO, Double> getClientesOrdenadosPorFacturacion() {
//        Map<ClienteDTO, Double> clientesConFacturacion = new LinkedHashMap<>();
//
//        String query = "SELECT c.idCliente, c.nombre, c.email, SUM(fp.cantidad * p.valor) AS totalFacturado " +
//                        "FROM cliente c " +
//                        "JOIN factura f ON c.idCliente = f.idCliente " +
//                        "JOIN factura_producto fp ON f.idFactura = fp.idFactura " +
//                        "JOIN producto p ON fp.idProducto = p.idProducto " +
//                        "GROUP BY c.idCliente, c.nombre, c.email " +
//                        "ORDER BY totalFacturado DESC";
//
//        try (PreparedStatement ps = conn.prepareStatement(query);
//             ResultSet rs = ps.executeQuery()) {
//
//            while (rs.next()) {
//                ClienteDTO c = new ClienteDTO(
//                        rs.getInt("idCliente"),
//                        rs.getString("nombre"),
//                        rs.getString("email"),
//                        rs.getDouble("totalFacturado")
//                );
////                double total = rs.getDouble("totalFacturado");
//
//                // Guardamos el cliente y su total en el Map
//                clientesConFacturacion.put(c, rs.getDouble("totalFacturado"));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        return clientesConFacturacion;
//    }

    @Override
    public List<ClienteDTO> getClientesOrdenadosPorFacturacion() throws SQLException{
        List<ClienteDTO> clienteConFacturacion = new ArrayList<>();

            String query = "SELECT c.idCliente, c.nombre, c.email, SUM(fp.cantidad * p.valor) AS totalFacturado " +
                            "FROM cliente c " +
                            "JOIN factura f ON c.idCliente = f.idCliente " +
                            "JOIN factura_producto fp ON f.idFactura = fp.idFactura " +
                            "JOIN producto p ON fp.idProducto = p.idProducto " +
                            "GROUP BY c.idCliente, c.nombre, c.email " +
                            "ORDER BY totalFacturado DESC";

            try(PreparedStatement ps = conn.prepareStatement(query)){
                    ResultSet rs = ps.executeQuery();

                    while(rs.next()){
                        ClienteDTO cliente = new ClienteDTO(
                            rs.getInt("idCliente"),
                            rs.getString("nombre"),
                            rs.getString("email"),
                            rs.getDouble("totalFacturado")
                        );

                        clienteConFacturacion.add(cliente);
                    }
            }
        return clienteConFacturacion;
    }
}