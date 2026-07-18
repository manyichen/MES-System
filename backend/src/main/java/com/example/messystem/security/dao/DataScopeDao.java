/*
 * 答辩定位：授权策略与数据范围 模块的 DataScopeDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.security.dao;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

/** 集中管理数据范围分配以及全部命名归属查询。 */
public class DataScopeDao {
    /**
     * 数据访问：查询匹配记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public ScopeAssignments findAssignments(long userId) throws SQLException {
        return new ScopeAssignments(
                queryIds("select line_id from mes_user_line_scope where user_id = ? order by line_id", userId, false),
                queryIds("select warehouse_id from mes_user_warehouse_scope where user_id = ? order by warehouse_id", userId, false));
    }

    /** 校验全部关联 ID 后，在一个事务中替换产线和仓库授权。 */
    public void replaceAssignments(long userId, Set<Long> lineIds, Set<Long> warehouseIds, long assignedBy)
            throws SQLException {
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureExists(connection, "select 1 from mes_user where user_id = ?", userId, "用户不存在");
                ensureIdsExist(connection, "mes_production_line", "line_id", lineIds, "产线");
                ensureIdsExist(connection, "mes_warehouse", "warehouse_id", warehouseIds, "仓库");
                replace(connection, "mes_user_line_scope", "line_id", userId, lineIds, assignedBy);
                replace(connection, "mes_user_warehouse_scope", "warehouse_id", userId, warehouseIds, assignedBy);
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * 数据访问：分配执行人员或资源。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public void assignWarehouse(long userId, long warehouseId, long assignedBy) throws SQLException {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into mes_user_warehouse_scope (user_id, warehouse_id, assigned_by)
                     values (?, ?, ?) on conflict (user_id, warehouse_id) do nothing
                     """)) {
            statement.setLong(1, userId);
            statement.setLong(2, warehouseId);
            statement.setLong(3, assignedBy);
            statement.executeUpdate();
        }
    }

    /** 根据稳定的查询名称，为请求级授权快照解析可见数据 ID。 */
    public Set<Long> findVisibleIds(String queryName, long userId) throws SQLException {
        NamedQuery query = switch (queryName) {
            case "lines" -> new NamedQuery("select line_id from mes_user_line_scope where user_id = ?", false);
            case "warehouses" -> new NamedQuery("select warehouse_id from mes_user_warehouse_scope where user_id = ?", false);
            case "lineTasks" -> new NamedQuery("""
                    select t.task_id from mes_production_task t join mes_user_line_scope s
                    on s.line_id = t.target_line_id where s.user_id = ?
                    """, false);
            case "lineReports" -> new NamedQuery("""
                    select wr.report_id from mes_work_report wr join mes_work_order wo on wo.work_order_id = wr.work_order_id
                    join mes_user_line_scope s on s.line_id = wo.line_id where s.user_id = ?
                    """, false);
            case "lineInspections" -> new NamedQuery("""
                    select q.inspection_id from mes_quality_inspection q join mes_work_order wo on wo.work_order_id = q.work_order_id
                    join mes_user_line_scope s on s.line_id = wo.line_id where s.user_id = ?
                    """, false);
            case "lineReworks" -> new NamedQuery("""
                    select r.rework_order_id from mes_rework_order r
                    left join mes_work_order wo on wo.work_order_id = r.source_work_order_id
                    join mes_user_line_scope s on s.user_id = ? and (s.line_id = wo.line_id or s.line_id = r.assigned_line_id)
                    """, false);
            case "lineRepairs" -> new NamedQuery("""
                    select r.repair_report_id from mes_equipment_repair_report r
                    join mes_equipment e on e.equipment_id = r.equipment_id
                    join mes_user_line_scope s on s.line_id = e.line_id where s.user_id = ?
                    """, false);
            case "lineMaintenance" -> new NamedQuery("""
                    select m.maintenance_order_id from mes_maintenance_order m
                    join mes_equipment e on e.equipment_id = m.equipment_id
                    join mes_user_line_scope s on s.line_id = e.line_id where s.user_id = ?
                    """, false);
            case "warehouseTransactions" -> new NamedQuery("""
                    select t.transaction_id from mes_inventory_transaction t
                    join mes_inventory i on i.inventory_id = t.inventory_id
                    join mes_user_warehouse_scope s on s.warehouse_id = i.warehouse_id where s.user_id = ?
                    """, false);
            case "warehouseRobots" -> new NamedQuery("""
                    select r.robot_id from mes_robot r join mes_user_warehouse_scope s
                    on s.warehouse_id = r.warehouse_id where s.user_id = ?
                    """, false);
            case "lineWages" -> new NamedQuery("""
                    select w.wage_id from mes_piecework_wage w join mes_work_report r on r.report_id = w.report_id
                    join mes_work_order wo on wo.work_order_id = r.work_order_id
                    join mes_user_line_scope s on s.line_id = wo.line_id where s.user_id = ?
                    """, false);
            case "warehouseTasks" -> new NamedQuery("""
                    select distinct wo.task_id from mes_work_order wo join mes_material_requisition r on r.work_order_id = wo.work_order_id
                    join mes_user_warehouse_scope s on s.warehouse_id = r.warehouse_id where s.user_id = ?
                    """, false);
            case "warehouseOrders" -> new NamedQuery("""
                    select distinct t.order_id from mes_production_task t join mes_work_order wo on wo.task_id = t.task_id
                    join mes_material_requisition r on r.work_order_id = wo.work_order_id
                    join mes_user_warehouse_scope s on s.warehouse_id = r.warehouse_id where s.user_id = ?
                    """, false);
            case "lineWorkOrders" -> new NamedQuery("""
                    select wo.work_order_id from mes_work_order wo join mes_user_line_scope s
                    on s.line_id = wo.line_id where s.user_id = ?
                    """, false);
            case "lineOrders" -> new NamedQuery("""
                    select distinct t.order_id from mes_production_task t
                    join mes_user_line_scope s on s.user_id = ? and s.line_id = t.target_line_id
                    union
                    select distinct t.order_id from mes_production_task t join mes_work_order wo on wo.task_id = t.task_id
                    join mes_user_line_scope s on s.user_id = ? and s.line_id = wo.line_id
                    """, true);
            case "lineEquipment" -> new NamedQuery("""
                    select e.equipment_id from mes_equipment e join mes_user_line_scope s
                    on s.line_id = e.line_id where s.user_id = ?
                    """, false);
            case "warehouseLocations" -> new NamedQuery("""
                    select l.location_id from mes_warehouse_location l join mes_user_warehouse_scope s
                    on s.warehouse_id = l.warehouse_id where s.user_id = ?
                    """, false);
            case "warehouseInventory" -> new NamedQuery("""
                    select i.inventory_id from mes_inventory i join mes_user_warehouse_scope s
                    on s.warehouse_id = i.warehouse_id where s.user_id = ?
                    """, false);
            case "warehouseRequisitions" -> new NamedQuery("""
                    select r.requisition_id from mes_material_requisition r join mes_user_warehouse_scope s
                    on s.warehouse_id = r.warehouse_id where s.user_id = ?
                    """, false);
            case "warehousePicking" -> new NamedQuery("""
                    select p.picking_task_id from mes_picking_task p join mes_user_warehouse_scope s
                    on s.warehouse_id = p.warehouse_id where s.user_id = ?
                    """, false);
            case "warehouseDelivery" -> new NamedQuery("""
                    select d.delivery_task_id from mes_robot_delivery_task d join mes_picking_task p on p.picking_task_id = d.picking_task_id
                    join mes_user_warehouse_scope s on s.warehouse_id = p.warehouse_id where s.user_id = ?
                    """, false);
            case "warehouseWorkOrders" -> new NamedQuery("""
                    select distinct r.work_order_id from mes_material_requisition r join mes_user_warehouse_scope s
                    on s.warehouse_id = r.warehouse_id where s.user_id = ?
                    """, false);
            default -> throw new IllegalArgumentException("unknown data-scope query: " + queryName);
        };
        return queryIds(query.sql(), userId, query.repeatedUserParameter());
    }

    /**
     * 数据访问：执行 queryIds 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static Set<Long> queryIds(String sql, long userId, boolean repeatedUserParameter) throws SQLException {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            if (repeatedUserParameter) statement.setLong(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                Set<Long> values = new LinkedHashSet<>();
                while (rs.next()) values.add(rs.getLong(1));
                return values;
            }
        }
    }

    /**
     * 数据访问：执行 ensureExists 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void ensureExists(Connection connection, String sql, long id, String message) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new BadRequestException(message);
            }
        }
    }

    /**
     * 数据访问：执行 ensureIdsExist 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void ensureIdsExist(Connection connection, String table, String column, Set<Long> ids,
            String label) throws SQLException {
        if (ids.isEmpty()) return;
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        try (PreparedStatement statement = connection.prepareStatement(
                "select count(*) from " + table + " where " + column + " in (" + placeholders + ")")) {
            int index = 1;
            for (Long id : ids) statement.setLong(index++, id);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                if (rs.getInt(1) != ids.size()) throw new BadRequestException("包含不存在的" + label + "ID");
            }
        }
    }

    /**
     * 数据访问：执行 replace 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void replace(Connection connection, String table, String column, long userId,
            Set<Long> ids, long assignedBy) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("delete from " + table + " where user_id = ?")) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
        if (ids.isEmpty()) return;
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into " + table + " (user_id, " + column + ", assigned_by) values (?, ?, ?)")) {
            for (Long id : ids) {
                statement.setLong(1, userId);
                statement.setLong(2, id);
                statement.setLong(3, assignedBy);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /**
     * 数据访问：执行 ScopeAssignments 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record ScopeAssignments(Set<Long> lineIds, Set<Long> warehouseIds) {
    }

    /**
     * 数据访问：执行 NamedQuery 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private record NamedQuery(String sql, boolean repeatedUserParameter) {
    }
}
