/*
 * 答辩定位：公共基础设施 模块的 HealthDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common.dao;

import com.example.messystem.common.Db;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 读取健康检查接口所需的数据库元数据。 */
public class HealthDao {
    private static final List<String> CRITICAL_TABLES = List.of(
            "mes_material", "mes_inventory", "mes_warehouse", "mes_warehouse_location",
            "mes_inventory_transaction", "mes_material_requisition", "mes_material_requisition_item",
            "mes_picking_task", "mes_robot", "mes_robot_delivery_task", "mes_work_report",
            "mes_piecework_wage");

    /** 在同一个活动连接中完成检查，保证返回值来自同一数据库会话。 */
    public Map<String, Object> inspect() throws SQLException {
        try (Connection connection = Db.getConnection()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("readOnly", connection.isReadOnly());
            data.put("autoCommit", connection.getAutoCommit());
            data.put("server", serverInfo(connection));
            data.put("current", currentInfo(connection));
            data.put("criticalTables", tableStatus(connection));
            return data;
        }
    }

    /**
     * 数据访问：执行 serverInfo 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static Map<String, Object> serverInfo(Connection connection) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("productName", meta.getDatabaseProductName());
        info.put("productVersion", meta.getDatabaseProductVersion());
        info.put("driverName", meta.getDriverName());
        info.put("driverVersion", meta.getDriverVersion());
        return info;
    }

    /**
     * 数据访问：执行 currentInfo 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static Map<String, Object> currentInfo(Connection connection) throws SQLException {
        String sql = "select current_database() as database_name, current_user as user_name, now() as server_time";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("database", resultSet.getString("database_name"));
            info.put("user", resultSet.getString("user_name"));
            info.put("serverTime", resultSet.getObject("server_time", OffsetDateTime.class).toString());
            return info;
        }
    }

    /**
     * 数据访问：执行 tableStatus 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static Map<String, Boolean> tableStatus(Connection connection) throws SQLException {
        Map<String, Boolean> status = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("select to_regclass(?) is not null")) {
            for (String tableName : CRITICAL_TABLES) {
                statement.setString(1, "public." + tableName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    status.put(tableName, resultSet.getBoolean(1));
                }
            }
        }
        return status;
    }
}
