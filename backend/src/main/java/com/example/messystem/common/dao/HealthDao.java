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

    private static Map<String, Object> serverInfo(Connection connection) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("productName", meta.getDatabaseProductName());
        info.put("productVersion", meta.getDatabaseProductVersion());
        info.put("driverName", meta.getDriverName());
        info.put("driverVersion", meta.getDriverVersion());
        return info;
    }

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
