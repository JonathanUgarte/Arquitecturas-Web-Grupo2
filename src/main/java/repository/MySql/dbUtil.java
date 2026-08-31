package repository.MySql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class dbUtil {

    public static void createTables() {
        Connection conn = MySqlConnectionManager.getInstance().getConnection();

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
                " FOREIGN KEY (idCliente) REFERENCES cliente(idCliente)" +
                ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createCliente);
            stmt.execute(createProducto);
            stmt.execute(createFactura);
            System.out.println("Tablas (Cliente, Producto, Factura) creadas correctamente.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
