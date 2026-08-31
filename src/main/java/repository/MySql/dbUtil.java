package repository.MySql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class dbUtil {

    public static void createTables() {
        String createCliente = "CREATE TABLE IF NOT EXISTS cliente (" +
                "idCliente INT AUTO_INCREMENT PRIMARY KEY, " +
                "nombre VARCHAR(50) NOT NULL, " +
                "email VARCHAR(80)" +
                ");";

        String createProducto = "CREATE TABLE IF NOT EXISTS producto (" +
                "idProducto INT AUTO_INCREMENT PRIMARY KEY, " +
                "nombre VARCHAR(50) NOT NULL, " +
                "valor FLOAT NOT NULL" +
                ");";

        String createFactura = "CREATE TABLE IF NOT EXISTS factura (" +
                "idFactura INT AUTO_INCREMENT PRIMARY KEY, " +
                "idCliente INT NOT NULL, " +
                "fecha DATE, " +
                "FOREIGN KEY (idCliente) REFERENCES cliente(idCliente)" +
                ");";

        String createFacturaProducto = "CREATE TABLE IF NOT EXISTS factura_producto (" +
                "idFactura INT NOT NULL, " +
                "idProducto INT NOT NULL, " +
                "cantidad INT NOT NULL, " +
                "PRIMARY KEY (idFactura, idProducto), " +
                "FOREIGN KEY (idFactura) REFERENCES factura(idFactura), " +
                "FOREIGN KEY (idProducto) REFERENCES producto(idProducto)" +
                ");";

        // Obtenemos la conexión aquí mismo dentro de un bloque try-with-resources para que se cierre bien
        try (Connection conn = MySqlConnectionManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createCliente);
            stmt.execute(createProducto);
            stmt.execute(createFactura);
            stmt.execute(createFacturaProducto);
            System.out.println("Tablas creadas correctamente.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
