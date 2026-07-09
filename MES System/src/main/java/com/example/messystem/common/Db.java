package com.example.messystem.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {

    private Db() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DbConfig.getJdbcUrl(), DbConfig.getUser(), DbConfig.getPassword());
    }

    public static void initializeDatabase() throws SQLException {
        try (Connection ignored = getConnection()) {
            // Verifies the configured database connection.
        }
    }
}
