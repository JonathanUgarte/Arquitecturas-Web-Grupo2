package repository.MySql;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CSVLoader {

    public static void cargarDatos(Connection conn) {
        cargarProductos(conn);
        cargarClientes(conn);
        cargarFacturas(conn);
        cargarFacturaProductos(conn);
    }

    private static void cargarProductos(Connection conn) {
        String sql = "INSERT INTO producto (idProducto, nombre, valor) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), valor = VALUES(valor);";

        try (CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new FileReader("src/main/resources/data/productos.csv"));
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (CSVRecord row : parser) {
                pstmt.setInt(1, Integer.parseInt(row.get("idProducto")));
                pstmt.setString(2, row.get("nombre"));
                pstmt.setFloat(3, Float.parseFloat(row.get("valor")));
                pstmt.executeUpdate();
            }
            System.out.println("-> Productos cargados exitosamente.");
        } catch (IOException | SQLException e) {
            System.err.println("Error al cargar productos: " + e.getMessage());
        }
    }

    private static void cargarClientes(Connection conn) {
        String sql = "INSERT INTO cliente (idCliente, nombre, email) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE nombre = VALUES(nombre), email = VALUES(email);";

        try (CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new FileReader("src/main/resources/data/clientes.csv"));
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (CSVRecord row : parser) {
                pstmt.setInt(1, Integer.parseInt(row.get("idCliente")));
                pstmt.setString(2, row.get("nombre"));
                pstmt.setString(3, row.get("email"));
                pstmt.executeUpdate();
            }
            System.out.println("-> Clientes cargados exitosamente.");
        } catch (IOException | SQLException e) {
            System.err.println("Error al cargar clientes: " + e.getMessage());
        }
    }

    private static void cargarFacturas(Connection conn) {
        String sql = "INSERT INTO factura (idFactura, idCliente) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE idCliente = VALUES(idCliente);";

        try (CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new FileReader("src/main/resources/data/facturas.csv"));
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (CSVRecord row : parser) {
                pstmt.setInt(1, Integer.parseInt(row.get("idFactura")));
                pstmt.setInt(2, Integer.parseInt(row.get("idCliente")));
                pstmt.executeUpdate();
            }
            System.out.println("-> Facturas cargadas exitosamente.");
        } catch (IOException | SQLException e) {
            System.err.println("Error al cargar facturas: " + e.getMessage());
        }
    }

    private static void cargarFacturaProductos(Connection conn) {
        String sql = "INSERT INTO factura_producto (idFactura, idProducto, cantidad) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE cantidad = VALUES(cantidad);";

        try (CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new FileReader("src/main/resources/data/facturas-productos.csv"));
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (CSVRecord row : parser) {
                pstmt.setInt(1, Integer.parseInt(row.get("idFactura")));
                pstmt.setInt(2, Integer.parseInt(row.get("idProducto")));
                pstmt.setInt(3, Integer.parseInt(row.get("cantidad")));
                pstmt.executeUpdate();
            }
            System.out.println("-> Relación Factura-Producto cargada exitosamente.");
        } catch (IOException | SQLException e) {
            System.err.println("Error al cargar facturas-productos: " + e.getMessage());
        }
    }
}