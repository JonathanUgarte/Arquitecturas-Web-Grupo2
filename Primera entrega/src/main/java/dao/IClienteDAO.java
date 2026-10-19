package dao;

import entities.Cliente;
import entities.dto.ClienteDTO;

import java.sql.SQLException;
import java.util.List;

public interface IClienteDAO extends DAO<Cliente, Integer> {
    // Consulta específica: clientes ordenados por total facturado (mayor a menor)
    List<ClienteDTO> getClientesOrdenadosPorFacturacion() throws SQLException;
}
