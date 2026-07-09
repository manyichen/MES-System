package com.example.messystem.common.resource;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.Db;
import com.example.messystem.common.DbConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/db")
@Produces(MediaType.APPLICATION_JSON)
public class DbPingResource {
    private static final List<String> B_TABLES = List.of(
            "mes_material",
            "mes_inventory",
            "mes_warehouse",
            "mes_warehouse_location",
            "mes_inventory_transaction",
            "mes_material_requisition",
            "mes_material_requisition_item",
            "mes_picking_task",
            "mes_robot",
            "mes_robot_delivery_task",
            "mes_work_report",
            "mes_piecework_wage"
    );

    @GET
    @Path("/ping")
    public Response ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("host", DbConfig.HOST);
        data.put("port", DbConfig.PORT);
        data.put("database", DbConfig.DATABASE);
        data.put("user", DbConfig.USER);

        try (Connection connection = Db.getConnection()) {
            data.put("connected", true);
            data.put("readOnly", connection.isReadOnly());
            data.put("autoCommit", connection.getAutoCommit());
            data.put("server", serverInfo(connection));
            data.put("current", currentInfo(connection));
            data.put("bTables", tableStatus(connection));
            return Response.ok(ApiResponse.ok("database connected", data)).build();
        } catch (SQLException | RuntimeException ex) {
            data.put("connected", false);
            data.put("errorType", ex.getClass().getSimpleName());
            data.put("error", ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.fail("database connection failed: " + ex.getMessage()))
                    .build();
        }
    }

    private Map<String, Object> serverInfo(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("productName", metaData.getDatabaseProductName());
        info.put("productVersion", metaData.getDatabaseProductVersion());
        info.put("driverName", metaData.getDriverName());
        info.put("driverVersion", metaData.getDriverVersion());
        return info;
    }

    private Map<String, Object> currentInfo(Connection connection) throws SQLException {
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

    private Map<String, Boolean> tableStatus(Connection connection) throws SQLException {
        Map<String, Boolean> status = new LinkedHashMap<>();
        String sql = "select to_regclass(?) is not null as exists_flag";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String tableName : B_TABLES) {
                statement.setString(1, "public." + tableName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    status.put(tableName, resultSet.getBoolean("exists_flag"));
                }
            }
        }
        return status;
    }
}
