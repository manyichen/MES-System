package com.mes.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MES 系统数据库初始化工具类。
 * <p>
 * 负责加载 MySQL JDBC 驱动、创建数据库以及创建系统所需的数据表。
 */
public final class DatabaseInitializer {
    /**
     * 工具类不需要被实例化，因此将构造方法私有化。
     */
    private DatabaseInitializer() {
    }

    /**
     * 执行数据库初始化流程。
     *
     * @throws SQLException 数据库连接或 SQL 执行失败时抛出
     */
    public static void initialize() throws SQLException {
        loadDriver();
        createDatabase();
        createUserTable();
    }

    /**
     * 如果数据库不存在，则根据配置创建数据库。
     *
     * @throws SQLException 创建数据库失败时抛出
     */
    private static void createDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                DbConfig.serverUrl(), DbConfig.USER, DbConfig.PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + DbConfig.DATABASE + "` DEFAULT CHARACTER SET utf8mb4");
        }
    }

    /**
     * 如果用户表不存在，则创建保存登录账号和密码的 user 表。
     *
     * @throws SQLException 创建数据表失败时抛出
     */
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

    /**
     * 获取已经指定数据库名称的 JDBC 连接。
     *
     * @return 数据库连接对象
     * @throws SQLException 获取连接失败时抛出
     */
    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DbConfig.databaseUrl(), DbConfig.USER, DbConfig.PASSWORD);
    }

    /**
     * 加载 MySQL JDBC 驱动。
     * <p>
     * 如果项目依赖中缺少 MySQL 驱动，则抛出运行时异常提示配置问题。
     */
    private static void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver is missing from the classpath.", e);
        }
    }
}
