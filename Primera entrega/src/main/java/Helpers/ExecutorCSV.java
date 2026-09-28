package Helpers;

import dao.IClienteDAO;
import dao.IFacturaDAO;
import dao.IFacturaProductoDAO;
import dao.IProductoDAO;
import entities.Cliente;
import entities.Factura;
import entities.FacturaProducto;
import entities.Producto;
import factory.DAOFactory;
import factory.MySQLDAOFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// DBUtil y CSVLoader unidos. (Ambos trabajaban con las tablas).

public class ExecutorCSV {

    private DAOFactory daofactory;

    public ExecutorCSV(DAOFactory daofactory){
        this.daofactory = daofactory;
    }

    //Optimizacion de codigo funcion que ejecuta las queries
    private void executeDB(String sql) throws SQLException {
        Connection conn = MySQLDAOFactory.getConnection();
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Error en la conexion!");
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }

        public void dropTables() throws SQLException{
            executeDB("DROP TABLE IF EXISTS factura_producto");
            executeDB("DROP TABLE IF EXISTS factura");
            executeDB("DROP TABLE IF EXISTS producto");
            executeDB("DROP TABLE IF EXISTS cliente");

            System.out.println("Borrado de tablas exitoso!");
        }


        public void createTables() throws SQLException{
            executeDB("CREATE TABLE IF NOT EXISTS cliente (" +
                        "idCliente INT AUTO_INCREMENT PRIMARY KEY, " +
                        "nombre VARCHAR(500) NOT NULL, " +
                        "email VARCHAR(150)" +
                        ");");

            executeDB("CREATE TABLE IF NOT EXISTS producto (" +
                        "idProducto INT AUTO_INCREMENT PRIMARY KEY, " +
                        "nombre VARCHAR(45) NOT NULL, " +
                        "valor FLOAT NOT NULL" +
                        ");");

            executeDB("CREATE TABLE IF NOT EXISTS factura (" +
                        "idFactura INT AUTO_INCREMENT PRIMARY KEY, " +
                        "idCliente INT NOT NULL, " +
                        "fecha DATE, " +
                        "FOREIGN KEY (idCliente) REFERENCES cliente(idCliente)" +
                        ");");

            executeDB("CREATE TABLE IF NOT EXISTS factura_producto (" +
                        "idFactura INT NOT NULL, " +
                        "idProducto INT NOT NULL, " +
                        "cantidad INT NOT NULL, " +
                        "PRIMARY KEY (idFactura, idProducto), " +
                        "FOREIGN KEY (idFactura) REFERENCES factura(idFactura), " +
                        "FOREIGN KEY (idProducto) REFERENCES producto(idProducto)" +
                        ");");


            System.out.println("Las tablas fueron creadas!");
        }


        public void fillDB() throws Exception {
            fillProductos("data/productos.csv");
            fillClientes("data/clientes.csv");
            fillFacturas("data/facturas.csv");
            fillFacturasProductos("data/facturas-productos.csv");

            System.out.println("Datos cargados exitosamente!");
        }


        public void fillClientes(String path) throws Exception{
            IClienteDAO cDAO = daofactory.getClienteDAO();
            InputStream input = getClass()
                    .getClassLoader()
                    .getResourceAsStream(path);

            if (input == null) {
                throw new Exception("No se encontró " + path);
            }
            try(CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new InputStreamReader(input))){
                for(CSVRecord row : parser){
                    int id = Integer.parseInt(row.get("idCliente").trim());
                    String nombre = row.get("nombre").trim();
                    String email = row.get("email").trim();

                    cDAO.insertar(new Cliente(id, nombre, email));
                }

                 System.out.println("-> Clientes cargados exitosamente.");

            } catch (IOException | SQLException e) {
                System.err.println("Error al cargar clientes: " + e.getMessage());
            }
        }


        public void fillProductos(String path) throws Exception{
            IProductoDAO pDAO = daofactory.getProductoDAO();

            InputStream input = getClass()
                    .getClassLoader()
                    .getResourceAsStream(path);

            if (input == null) {
                throw new Exception("No se encontró " + path);
            }
            try(CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new InputStreamReader(input))){
                for(CSVRecord row : parser){
                    int id = Integer.parseInt(row.get("idProducto").trim());
                    String nombre = row.get("nombre").trim();
                    float valor = Float.parseFloat(row.get("valor").trim().replace(",","."));

                    pDAO.insertar(new Producto(id,nombre,valor));
                }

                System.out.println("-> Productos cargados exitosamente.");

            } catch (IOException e) {
                System.err.println("Error al cargar productos: " + e.getMessage());
            }
        }

        public void fillFacturas(String path) throws Exception{
            IFacturaDAO fDAO = daofactory.getFacturaDAO();
            InputStream input = getClass()
                    .getClassLoader()
                    .getResourceAsStream(path);

            if (input == null) {
                throw new Exception("No se encontró " + path);
            }
            try(CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new InputStreamReader(input))){
                for(CSVRecord row : parser){
                    int idFactura = Integer.parseInt(row.get("idFactura").trim());
                    int idProducto = Integer.parseInt(row.get("idCliente").trim());

                    fDAO.insertar(new Factura(idFactura, idProducto));
                }

                System.out.println("-> Facturas cargadas exitosamente.");

            } catch (IOException e) {
                System.err.println("Error al cargar facturas: " + e.getMessage());
            }
        }

        public void fillFacturasProductos(String path) throws Exception{
            IFacturaProductoDAO fpDAO = daofactory.getFacturaProductoDAO();

            IFacturaDAO fDAO = daofactory.getFacturaDAO();
            InputStream input = getClass()
                    .getClassLoader()
                    .getResourceAsStream(path);

            if (input == null) {
                throw new Exception("No se encontró " + path);
            }

            try(CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new InputStreamReader(input))){
                for(CSVRecord row : parser){
                    int idFactura = Integer.parseInt(row.get("idFactura").trim());
                    int idProducto = Integer.parseInt(row.get("idProducto").trim());
                    int cantidad = Integer.parseInt(row.get("cantidad").trim());

                    fpDAO.insertar(new FacturaProducto(idFactura, idProducto, cantidad));
                }

            System.out.println("-> Relación Factura-Producto cargada exitosamente.");

            } catch (IOException e) {
                System.err.println("Error al cargar facturas-productos: " + e.getMessage());
            }
        }

    }
