package repository.MySql;

import factory.IConnectionManager;

import java.sql.Connection;

public final class MySqlConnectionManager implements IConnectionManager {

    @Override
    public Connection getConnection() {
        return null;
    }

    @Override
    public void shutdown() {

    }
}
