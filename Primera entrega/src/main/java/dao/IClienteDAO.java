package dao;

import entities.Cliente;
import entities.dto.ClienteDTO;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface IClienteDAO {
    void insertar(Cliente cliente) throws SQLException;
//    Map<ClienteDTO, Double> getClientesOrdenadosPorFacturacion();
    List<ClienteDTO> getClientesOrdenadosPorFacturacion() throws SQLException;
}
