import java.sql.Connection;
import repository.MySql.MySqlConnectionManager;
import repository.MySql.CSVLoader;
import repository.MySql.dbUtil;

import java.sql.Connection;
import java.util.List;
import repository.MySql.MySqlConnectionManager;
import repository.MySql.CSVLoader;
import repository.MySql.dbUtil;
import repository.MySql.MySqlProductoDAO;
import repository.MySql.MySqlClienteDAO;
import entities.Producto;
import entities.Cliente;

public class Main {
    public static void main(String[] args) {

        // 1. Primero creamos las tablas
        dbUtil.createTables();

        // 2. Abrimos la conexión para cargar los datos y ejecutar las consultas
        try (Connection conn = MySqlConnectionManager.getInstance().getConnection()) {

            // Punto 2: Carga de CSVs
            System.out.println("Iniciando carga de datos...");
            CSVLoader.cargarDatos(conn);
            System.out.println("¡Proceso de carga finalizado con éxito!\n");

            // --- PUNTO 3: Producto que más recaudó ---
            System.out.println("--- 3. Producto que más recaudó ---");
            MySqlProductoDAO productoDAO = new MySqlProductoDAO();
            Producto productoTop = productoDAO.getProductoMasRecaudador();

            if (productoTop != null) {
                System.out.println("ID: " + productoTop.getIdProducto() +
                        " | Producto: " + productoTop.getNombre() +
                        " | Valor unitario: $" + productoTop.getValor());
            } else {
                System.out.println("No se encontraron productos o ventas.");
            }



        } catch (Exception e) {
            System.err.println("Ocurrió un error en la ejecución: " + e.getMessage());
            e.printStackTrace();
        }
    }
}