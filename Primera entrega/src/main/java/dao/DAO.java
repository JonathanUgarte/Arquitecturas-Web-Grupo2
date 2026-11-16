package dao;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato CRUD genérico del patrón DAO.
 * Cada DAO concreto lo extiende y agrega solo sus consultas específicas.
 *
 * @param <T>  entidad que maneja el DAO
 * @param <ID> tipo de la clave primaria
 */
public interface DAO<T, ID> {
    void insertar(T entidad) throws SQLException;
    T getById(ID id) throws SQLException;
    List<T> getAll() throws SQLException;
    void actualizar(T entidad) throws SQLException;
    void eliminar(ID id) throws SQLException;
}
