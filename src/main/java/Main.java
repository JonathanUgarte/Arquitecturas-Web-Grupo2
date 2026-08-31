import java.sql.Connection;
import repository.MySql.MySqlConnectionManager;
import repository.MySql.CSVLoader;
import repository.MySql.dbUtil;

public class Main {
    public static void main(String[] args) {

        // 1. Primero creamos las tablas (ejecuta su propia conexión y cierra)
        dbUtil.createTables();

        // 2. Luego abrimos la conexión para cargar los datos de los CSVs
        try (Connection conn = MySqlConnectionManager.getInstance().getConnection()) {
            CSVLoader.cargarDatos(conn);
            System.out.println("¡Proceso de carga finalizado con éxito!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}