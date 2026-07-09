package com.example.messystem.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    private Db() {
    }

    public static Connection getConnection() throws SQLException {
        loadDriver();
        return DriverManager.getConnection(DbConfig.databaseUrl(), DbConfig.USER, DbConfig.PASSWORD);
    }

    public static Connection getServerConnection() throws SQLException {
        loadDriver();
        return DriverManager.getConnection(DbConfig.serverUrl(), DbConfig.USER, DbConfig.PASSWORD);
    }

    private static void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver is missing from the classpath.", e);
        }
    }
}
