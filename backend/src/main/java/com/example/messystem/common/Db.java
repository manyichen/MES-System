package com.example.messystem.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    private Db() {
    }

    public static Connection getConnection() throws SQLException {
        loadDriver();
        return DriverManager.getConnection(DbConfig.getJdbcUrl(), DbConfig.getUser(), DbConfig.getPassword());
    }

    private static void loadDriver() throws SQLException {
        String driverClassName = DbConfig.getDriverClassName();
        if (driverClassName == null || driverClassName.isBlank()) {
            return;
        }
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException ex) {
            throw new SQLException("JDBC driver not found: " + driverClassName, ex);
        }
    }

    public static void initializeDatabase() throws SQLException {
        try (Connection ignored = getConnection()) {
            // 启动时验证配置的数据库连接是否可用。
        }
    }

    
}
