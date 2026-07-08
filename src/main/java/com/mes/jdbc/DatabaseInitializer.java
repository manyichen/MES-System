package com.mes.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {
    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {
        loadDriver();
        createDatabase();
        createUserTable();
    }

    private static void createDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                DbConfig.serverUrl(), DbConfig.USER, DbConfig.PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + DbConfig.DATABASE + "` DEFAULT CHARACTER SET utf8mb4");
        }
    }

    private static void createUserTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS `user` (
                    `username` VARCHAR(50) PRIMARY KEY,
                    `password` VARCHAR(255) NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DbConfig.databaseUrl(), DbConfig.USER, DbConfig.PASSWORD);
    }

    private static void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver is missing from the classpath.", e);
        }
    }
}
