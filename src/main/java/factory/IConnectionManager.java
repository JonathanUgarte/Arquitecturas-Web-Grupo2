package factory;

import java.sql.Connection;

public interface IConnectionManager {
    Connection getConnection();
    void shutdown();
}
