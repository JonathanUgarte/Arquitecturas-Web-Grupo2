import java.sql.Connection;
import repository.MySql.MySqlConnectionManager;
import repository.MySql.CSVLoader;
import repository.MySql.dbUtil;
import repository.MySql.MySqlProductoDAO;
import entities.Producto;

public class Main {
    public static void main(String[] args) {

        // 1. Primero creamos las tablas
        dbUtil.createTables();

        // 2. Abrimos la conexión SOLAMENTE para cargar los datos de los CSVs
        try (Connection conn = MySqlConnectionManager.getInstance().getConnection()) {
            System.out.println("Iniciando carga de datos...");
            CSVLoader.cargarDatos(conn);
            System.out.println("¡Proceso de carga finalizado con éxito!\n");
        } catch (Exception e) {
            System.err.println("Ocurrió un error en la carga de datos: " + e.getMessage());
            e.printStackTrace();
        }

        // --- PUNTO 3: Producto que más recaudó ---
        // (El DAO ahora maneja su propia conexión internamente)
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

    }
}