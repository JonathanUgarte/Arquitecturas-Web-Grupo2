import java.util.List;

import Helpers.ExecutorCSV;
import dao.IClienteDAO;
import dao.IProductoDAO;

import entities.dto.ClienteDTO;
import factory.DAOFactory;
import factory.DBType;

public class Main {
    public static void main(String[] args) {
        try{
            //Intanciamos nuestra factory de la cual usaremos la conexion
            DAOFactory myslqFactory = DAOFactory.getDAOFactory(DBType.MYSQL);

            //Executor funciona para crear lo primordial de la DB
            ExecutorCSV executor = new ExecutorCSV(myslqFactory);

            //Aseguramos de que la base este solo con el contenido necesario entonces borramos las tablas y las cargamos
            executor.dropTables();
            executor.createTables();

            //Rellenamos las tablas con los CSVs
            executor.fillDB();


            // 3. Programa JDBC que retorne el producto que más recaudó

            System.out.println("\n ---Producto que mas recaudo---");
            IProductoDAO pDAO = myslqFactory.getProductoDAO();

            if(pDAO != null) System.out.println(pDAO.getProductoMasRecaudador());
            else System.out.println("No se encontraron productos o ventas.");


            //4. Programa JDBC que imprima una lista de clientes, ordenada por a cuál se le facturó más

            System.out.println("\n ---Clientes ordenados por facturación---");
                IClienteDAO cDAO = myslqFactory.getClienteDAO();
                List<ClienteDTO> clientesTop = cDAO.getClientesOrdenadosPorFacturacion();
            if (clientesTop != null && !clientesTop.isEmpty()) {

                clientesTop.forEach(cliente -> System.out.println(cliente));

            } else {
                System.out.println("No se encontraron clientes con facturación.");
            }

            System.out.println("Finalizado Saludos!!!");

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}