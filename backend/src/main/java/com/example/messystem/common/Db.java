/*
 * 答辩定位：公共基础设施 模块的 Db。
 * 分层职责：运行基础设施：负责应用注册、服务器启动、配置读取或数据库连接，是业务模块共享的外部依赖边界。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC 连接工厂，是所有 DAO 访问 PostgreSQL 的唯一底层入口。
 * 当前实现按 DAO 操作获取连接，并依赖 try-with-resources 及时关闭；跨多条写操作的 DAO
 * 会在同一 Connection 上关闭 autoCommit、提交或回滚，从而保证原子性。
 */
public final class Db {
    /**
     * 内部实现步骤：执行 Db 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private Db() {
    }

    /** 加载 JDBC 驱动后，用 DbConfig 提供的 URL、用户和密码打开真实数据库连接。 */
    public static Connection getConnection() throws SQLException {
        loadDriver();
        return DriverManager.getConnection(DbConfig.getJdbcUrl(), DbConfig.getUser(), DbConfig.getPassword());
    }

    /**
     * 显式加载驱动以兼容不同运行/打包环境；驱动缺失时转为 SQLException，
     * 让上层保留“数据库依赖不可用”的根因。
     */
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

    /** 启动/诊断时建立并立即关闭一次连接，用于验证配置和网络连通性，不执行建表。 */
    public static void initializeDatabase() throws SQLException {
        try (Connection ignored = getConnection()) {
            // 启动时验证配置的数据库连接是否可用。
        }
    }

}
