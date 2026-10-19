package dao.Impl;

import dao.IFacturaDAO;
import entities.Factura;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySqlFacturaDAO implements IFacturaDAO {

    private Connection conn;

    public MySqlFacturaDAO(Connection conn){
        this.conn = conn;
    }

    @Override
    public void insertar(Factura factura) throws SQLException {

    String sql= "INSERT INTO factura (idFactura, idCliente) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE idCliente = VALUES(idCliente);";

        try(PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, factura.getIdFactura());
            stmt.setInt(2, factura.getIdCliente());
            
            stmt.executeUpdate();
        }
    }
}
