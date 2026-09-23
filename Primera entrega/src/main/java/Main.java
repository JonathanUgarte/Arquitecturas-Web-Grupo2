import java.sql.Connection;
import java.util.List;
import java.util.Map;

import entities.Cliente;
import entities.dto.ProductoDTO;
import repository.MySql.*;
import entities.Producto;

public class Main {
    public static void main(String[] args) {

        dbUtil.createTables();

        try (Connection conn = MySqlConnectionManager.getInstance().getConnection()) {
            System.out.println("Iniciando carga de datos...");
            CSVLoader.cargarDatos(conn);
            System.out.println("¡Proceso de carga finalizado con éxito!\n");
        } catch (Exception e) {
            System.err.println("Ocurrió un error en la carga de datos: " + e.getMessage());
            e.printStackTrace();
        }


        System.out.println("--- 3. Producto que más recaudó ---");
        MySqlProductoDAO productoDAO = new MySqlProductoDAO();
        ProductoDTO productoTop = productoDAO.getProductoMasRecaudador();

        if (productoTop != null) {
            System.out.println("ID: " + productoTop.getIdProducto() +
                    " | Producto: " + productoTop.getNombre() +
                    " | Valor unitario: $" + productoTop.getValor()+
                    " | Valor recaudado: $" + productoTop.getRecaudacion());
        } else {
            System.out.println("No se encontraron productos o ventas.");
        }
        System.out.println("\n--- 4. Clientes ordenados por facturación ---");
        MySqlClienteDAO clienteDAO = new MySqlClienteDAO();
        Map<Cliente, Double> clientesTop = clienteDAO.getClientesOrdenadosPorFacturacion();

        if (clientesTop != null && !clientesTop.isEmpty()) {
            for (Map.Entry<Cliente, Double> entry : clientesTop.entrySet()) {
                Cliente c = entry.getKey();
                Double totalFacturado = entry.getValue();

                System.out.println("ID: " + c.getIdCliente() +
                        " | Nombre: " + c.getNombre() +
                        " | Email: " + c.getEmail() +
                        " | Total Facturado: $" + totalFacturado);
            }
        } else {
            System.out.println("No se encontraron clientes con facturación.");
        }
    }
}