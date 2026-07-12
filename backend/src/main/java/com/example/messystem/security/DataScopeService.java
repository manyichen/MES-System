package com.example.messystem.security;

import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.dashboard.entity.MesManagementFeedback;
import com.example.messystem.dashboard.entity.MesProductTrace;
import com.example.messystem.equipment.entity.MesEquipment;
import com.example.messystem.equipment.entity.MesEquipmentRepairReport;
import com.example.messystem.equipment.entity.MesMaintenanceOrder;
import com.example.messystem.equipment.entity.MesMaintenancePlan;
import com.example.messystem.planning.entity.MesCustomerOrder;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.MesWorkOrder;
import com.example.messystem.production.entity.MesPieceworkWage;
import com.example.messystem.production.entity.MesWorkReport;
import com.example.messystem.quality.entity.MesQualityInspection;
import com.example.messystem.quality.entity.MesQualityTrace;
import com.example.messystem.quality.entity.MesReworkOrder;
import com.example.messystem.warehouse.entity.MesInventory;
import com.example.messystem.warehouse.entity.MesInventoryTransaction;
import com.example.messystem.warehouse.entity.MesMaterialRequisition;
import com.example.messystem.warehouse.entity.MesPickingTask;
import com.example.messystem.warehouse.entity.MesRobot;
import com.example.messystem.warehouse.entity.MesRobotDeliveryTask;
import com.example.messystem.warehouse.entity.MesWarehouse;
import com.example.messystem.warehouse.entity.MesWarehouseLocation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataScopeService {
    public ScopeSnapshot snapshot(AuthenticatedUser user) {
        return new ScopeSnapshot(user);
    }

    public UserDataScopes getUserScopes(long userId) {
        return new UserDataScopes(loadAssignments("mes_user_line_scope", "line_id", userId),
                loadAssignments("mes_user_warehouse_scope", "warehouse_id", userId));
    }

    public UserDataScopes replaceUserScopes(long userId, List<Long> lineIds, List<Long> warehouseIds,
            long assignedBy) {
        Set<Long> lines = positiveIds(lineIds);
        Set<Long> warehouses = positiveIds(warehouseIds);
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureUserExists(connection, userId);
                ensureIdsExist(connection, "mes_production_line", "line_id", lines, "产线");
                ensureIdsExist(connection, "mes_warehouse", "warehouse_id", warehouses, "仓库");
                replaceAssignments(connection, "mes_user_line_scope", "line_id", userId, lines, assignedBy);
                replaceAssignments(connection, "mes_user_warehouse_scope", "warehouse_id", userId, warehouses, assignedBy);
                connection.commit();
                return new UserDataScopes(lines, warehouses);
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    public void assignWarehouse(long userId, long warehouseId, long assignedBy) {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into mes_user_warehouse_scope (user_id, warehouse_id, assigned_by)
                     values (?, ?, ?) on conflict (user_id, warehouse_id) do nothing
                     """)) {
            statement.setLong(1, userId);
            statement.setLong(2, warehouseId);
            statement.setLong(3, assignedBy);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    private static Set<Long> loadAssignments(String table, String column, long userId) {
        String sql = "select " + column + " from " + table + " where user_id = ? order by " + column;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                Set<Long> ids = new LinkedHashSet<>();
                while (rs.next()) ids.add(rs.getLong(1));
                return ids;
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    private static void ensureUserExists(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select 1 from mes_user where user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new BadRequestException("用户不存在");
            }
        }
    }

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

    private static void replaceAssignments(Connection connection, String table, String column, long userId,
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

    private static Set<Long> positiveIds(List<Long> ids) {
        Set<Long> result = new LinkedHashSet<>();
        if (ids != null) ids.stream().filter(id -> id != null && id > 0).forEach(result::add);
        return result;
    }

    private static IllegalStateException database(SQLException ex) {
        return new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
    }

    public record UserDataScopes(Set<Long> lineIds, Set<Long> warehouseIds) {
    }

    public static final class ScopeSnapshot {
        private final AuthenticatedUser user;
        private final boolean lineRestricted;
        private final boolean warehouseRestricted;
        private final Map<String, Set<Long>> cache = new LinkedHashMap<>();

        private ScopeSnapshot(AuthenticatedUser user) {
            this.user = user;
            this.lineRestricted = user.hasRole("WORKSHOP_MANAGER") && !user.hasRole("SYSTEM_ADMIN");
            this.warehouseRestricted = user.hasRole("WAREHOUSE_ADMIN") && !user.hasRole("SYSTEM_ADMIN");
        }

        public boolean restricted() {
            return lineRestricted || warehouseRestricted;
        }

        public boolean lineRestricted() {
            return lineRestricted;
        }

        public boolean warehouseRestricted() {
            return warehouseRestricted;
        }

        public Set<Long> lineIds() {
            return ids("lines", "select line_id from mes_user_line_scope where user_id = ?");
        }

        public Set<Long> warehouseIds() {
            return ids("warehouses", "select warehouse_id from mes_user_warehouse_scope where user_id = ?");
        }

        public boolean canView(Object item) {
            if (!restricted() || item == null) return true;
            if (lineRestricted && isLineBusiness(item)) return canViewLineBusiness(item);
            if (warehouseRestricted && isWarehouseBusiness(item)) return canViewWarehouseBusiness(item);
            if (warehouseRestricted && isProductionReference(item)) return canViewWarehouseRelatedProduction(item);
            return true;
        }

        public void requireLine(long lineId) {
            if (lineRestricted && !lineIds().contains(lineId)) throw denied("产线", lineId);
        }

        public void requireWarehouse(long warehouseId) {
            if (warehouseRestricted && !warehouseIds().contains(warehouseId)) throw denied("仓库", warehouseId);
        }

        public void requireWorkOrder(long id) {
            if (lineRestricted && !visibleWorkOrdersForLines().contains(id)) throw denied("生产工单", id);
            if (warehouseRestricted && !visibleWorkOrdersForWarehouses().contains(id)) throw denied("生产工单", id);
        }

        public void requireTask(long id) {
            if (lineRestricted && !ids("lineTasks", """
                    select t.task_id from mes_production_task t join mes_user_line_scope s
                    on s.line_id = t.target_line_id where s.user_id = ?
                    """).contains(id)) throw denied("生产任务", id);
        }

        public void requireOrder(long id) {
            if (lineRestricted && !visibleOrdersForLines().contains(id)) throw denied("客户订单", id);
        }

        public void requireReport(long id) {
            if (lineRestricted && !ids("lineReports", """
                    select wr.report_id from mes_work_report wr join mes_work_order wo on wo.work_order_id = wr.work_order_id
                    join mes_user_line_scope s on s.line_id = wo.line_id where s.user_id = ?
                    """).contains(id)) throw denied("报工单", id);
        }

        public void requireInspection(long id) {
            if (lineRestricted && !ids("lineInspections", """
                    select q.inspection_id from mes_quality_inspection q join mes_work_order wo on wo.work_order_id = q.work_order_id
                    join mes_user_line_scope s on s.line_id = wo.line_id where s.user_id = ?
                    """).contains(id)) throw denied("质检单", id);
        }

        public void requireRework(long id) {
            if (lineRestricted && !ids("lineReworks", """
                    select r.rework_order_id from mes_rework_order r
                    left join mes_work_order wo on wo.work_order_id = r.source_work_order_id
                    join mes_user_line_scope s on s.user_id = ? and (s.line_id = wo.line_id or s.line_id = r.assigned_line_id)
                    """).contains(id)) throw denied("返工单", id);
        }

        public void requireEquipment(long id) {
            if (lineRestricted && !visibleEquipment().contains(id)) throw denied("设备", id);
        }

        public void requireRepair(long id) {
            if (lineRestricted && !ids("lineRepairs", """
                    select r.repair_report_id from mes_equipment_repair_report r
                    join mes_equipment e on e.equipment_id = r.equipment_id
                    join mes_user_line_scope s on s.line_id = e.line_id where s.user_id = ?
                    """).contains(id)) throw denied("设备报修单", id);
        }

        public void requireMaintenance(long id) {
            if (lineRestricted && !ids("lineMaintenance", """
                    select m.maintenance_order_id from mes_maintenance_order m
                    join mes_equipment e on e.equipment_id = m.equipment_id
                    join mes_user_line_scope s on s.line_id = e.line_id where s.user_id = ?
                    """).contains(id)) throw denied("维修工单", id);
        }

        public void requireWarehouseEntity(String type, long id) {
            if (!warehouseRestricted) return;
            Set<Long> allowed = switch (type) {
                case "warehouse" -> warehouseIds();
                case "location" -> visibleLocations();
                case "inventory" -> visibleInventory();
                case "transaction" -> ids("warehouseTransactions", """
                        select t.transaction_id from mes_inventory_transaction t
                        join mes_inventory i on i.inventory_id = t.inventory_id
                        join mes_user_warehouse_scope s on s.warehouse_id = i.warehouse_id where s.user_id = ?
                        """);
                case "requisition" -> visibleRequisitions();
                case "picking" -> visiblePicking();
                case "delivery" -> visibleDelivery();
                case "robot" -> ids("warehouseRobots", """
                        select r.robot_id from mes_robot r join mes_user_warehouse_scope s
                        on s.warehouse_id = r.warehouse_id where s.user_id = ?
                        """);
                default -> Set.of();
            };
            if (!allowed.contains(id)) throw denied(type, id);
        }

        private boolean canViewLineBusiness(Object item) {
            if (item instanceof MesWorkOrder value) return value.lineId != null && lineIds().contains(value.lineId);
            if (item instanceof MesProductionTask value) return value.targetLineId != null && lineIds().contains(value.targetLineId);
            if (item instanceof MesCustomerOrder value) return value.orderId != null && visibleOrdersForLines().contains(value.orderId);
            if (item instanceof MesWorkReport value) return value.workOrderId != null && visibleWorkOrdersForLines().contains(value.workOrderId);
            if (item instanceof MesPieceworkWage value) return value.wageId != null && ids("lineWages", """
                    select w.wage_id from mes_piecework_wage w join mes_work_report r on r.report_id = w.report_id
                    join mes_work_order wo on wo.work_order_id = r.work_order_id
                    join mes_user_line_scope s on s.line_id = wo.line_id where s.user_id = ?
                    """).contains(value.wageId);
            if (item instanceof MesQualityInspection value) return value.workOrderId() != null && visibleWorkOrdersForLines().contains(value.workOrderId());
            if (item instanceof MesReworkOrder value) return value.reworkOrderId() != null && ids("lineReworks", """
                    select r.rework_order_id from mes_rework_order r left join mes_work_order wo on wo.work_order_id = r.source_work_order_id
                    join mes_user_line_scope s on s.user_id = ? and (s.line_id = wo.line_id or s.line_id = r.assigned_line_id)
                    """).contains(value.reworkOrderId());
            if (item instanceof MesQualityTrace value) return value.workOrderId() != null && visibleWorkOrdersForLines().contains(value.workOrderId());
            if (item instanceof MesProductTrace value) return value.workOrderId() != null && visibleWorkOrdersForLines().contains(value.workOrderId());
            if (item instanceof MesManagementFeedback value) return value.workOrderId() != null && visibleWorkOrdersForLines().contains(value.workOrderId());
            if (item instanceof MesEquipment value) return value.lineId() != null && lineIds().contains(value.lineId());
            if (item instanceof MesEquipmentRepairReport value) return value.equipmentId() != null && visibleEquipment().contains(value.equipmentId());
            if (item instanceof MesMaintenanceOrder value) return value.equipmentId() != null && visibleEquipment().contains(value.equipmentId());
            if (item instanceof MesMaintenancePlan value) return value.equipmentId() != null && visibleEquipment().contains(value.equipmentId());
            if (item instanceof MesMaterialRequisition value) return value.workOrderId != null && visibleWorkOrdersForLines().contains(value.workOrderId);
            return true;
        }

        private boolean canViewWarehouseBusiness(Object item) {
            if (item instanceof MesWarehouse value) return value.warehouseId != null && warehouseIds().contains(value.warehouseId);
            if (item instanceof MesWarehouseLocation value) return value.warehouseId != null && warehouseIds().contains(value.warehouseId);
            if (item instanceof MesInventory value) return value.warehouseId != null && warehouseIds().contains(value.warehouseId);
            if (item instanceof MesInventoryTransaction value) return value.inventoryId != null && visibleInventory().contains(value.inventoryId);
            if (item instanceof MesMaterialRequisition value) return value.warehouseId != null && warehouseIds().contains(value.warehouseId);
            if (item instanceof MesPickingTask value) return value.warehouseId != null && warehouseIds().contains(value.warehouseId);
            if (item instanceof MesRobotDeliveryTask value) return value.pickingTaskId != null && visiblePicking().contains(value.pickingTaskId);
            if (item instanceof MesRobot value) return value.warehouseId != null && warehouseIds().contains(value.warehouseId);
            return true;
        }

        private boolean canViewWarehouseRelatedProduction(Object item) {
            if (item instanceof MesWorkOrder value) return value.workOrderId != null && visibleWorkOrdersForWarehouses().contains(value.workOrderId);
            if (item instanceof MesWorkReport value) return value.workOrderId != null && visibleWorkOrdersForWarehouses().contains(value.workOrderId);
            if (item instanceof MesQualityInspection value) return value.workOrderId() != null && visibleWorkOrdersForWarehouses().contains(value.workOrderId());
            if (item instanceof MesProductTrace value) return value.workOrderId() != null && visibleWorkOrdersForWarehouses().contains(value.workOrderId());
            if (item instanceof MesQualityTrace value) return value.workOrderId() != null && visibleWorkOrdersForWarehouses().contains(value.workOrderId());
            if (item instanceof MesProductionTask value) return value.taskId != null && ids("warehouseTasks", """
                    select distinct wo.task_id from mes_work_order wo join mes_material_requisition r on r.work_order_id = wo.work_order_id
                    join mes_user_warehouse_scope s on s.warehouse_id = r.warehouse_id where s.user_id = ?
                    """).contains(value.taskId);
            if (item instanceof MesCustomerOrder value) return value.orderId != null && ids("warehouseOrders", """
                    select distinct t.order_id from mes_production_task t join mes_work_order wo on wo.task_id = t.task_id
                    join mes_material_requisition r on r.work_order_id = wo.work_order_id
                    join mes_user_warehouse_scope s on s.warehouse_id = r.warehouse_id where s.user_id = ?
                    """).contains(value.orderId);
            return true;
        }

        private boolean isLineBusiness(Object item) {
            return item instanceof MesWorkOrder || item instanceof MesProductionTask || item instanceof MesCustomerOrder
                    || item instanceof MesWorkReport || item instanceof MesPieceworkWage
                    || item instanceof MesQualityInspection || item instanceof MesReworkOrder
                    || item instanceof MesQualityTrace || item instanceof MesProductTrace
                    || item instanceof MesManagementFeedback || item instanceof MesEquipment
                    || item instanceof MesEquipmentRepairReport || item instanceof MesMaintenanceOrder
                    || item instanceof MesMaintenancePlan || item instanceof MesMaterialRequisition;
        }

        private boolean isWarehouseBusiness(Object item) {
            return item instanceof MesWarehouse || item instanceof MesWarehouseLocation || item instanceof MesInventory
                    || item instanceof MesInventoryTransaction || item instanceof MesMaterialRequisition
                    || item instanceof MesPickingTask || item instanceof MesRobotDeliveryTask || item instanceof MesRobot;
        }

        private boolean isProductionReference(Object item) {
            return item instanceof MesWorkOrder || item instanceof MesProductionTask || item instanceof MesCustomerOrder
                    || item instanceof MesWorkReport || item instanceof MesQualityInspection
                    || item instanceof MesProductTrace || item instanceof MesQualityTrace;
        }

        private Set<Long> visibleWorkOrdersForLines() {
            return ids("lineWorkOrders", """
                    select wo.work_order_id from mes_work_order wo join mes_user_line_scope s
                    on s.line_id = wo.line_id where s.user_id = ?
                    """);
        }

        private Set<Long> visibleOrdersForLines() {
            return ids("lineOrders", """
                    select distinct t.order_id from mes_production_task t
                    join mes_user_line_scope s on s.user_id = ? and s.line_id = t.target_line_id
                    union
                    select distinct t.order_id from mes_production_task t join mes_work_order wo on wo.task_id = t.task_id
                    join mes_user_line_scope s on s.user_id = ? and s.line_id = wo.line_id
                    """, true);
        }

        private Set<Long> visibleEquipment() {
            return ids("lineEquipment", """
                    select e.equipment_id from mes_equipment e join mes_user_line_scope s
                    on s.line_id = e.line_id where s.user_id = ?
                    """);
        }

        private Set<Long> visibleLocations() {
            return ids("warehouseLocations", """
                    select l.location_id from mes_warehouse_location l join mes_user_warehouse_scope s
                    on s.warehouse_id = l.warehouse_id where s.user_id = ?
                    """);
        }

        private Set<Long> visibleInventory() {
            return ids("warehouseInventory", """
                    select i.inventory_id from mes_inventory i join mes_user_warehouse_scope s
                    on s.warehouse_id = i.warehouse_id where s.user_id = ?
                    """);
        }

        private Set<Long> visibleRequisitions() {
            return ids("warehouseRequisitions", """
                    select r.requisition_id from mes_material_requisition r join mes_user_warehouse_scope s
                    on s.warehouse_id = r.warehouse_id where s.user_id = ?
                    """);
        }

        private Set<Long> visiblePicking() {
            return ids("warehousePicking", """
                    select p.picking_task_id from mes_picking_task p join mes_user_warehouse_scope s
                    on s.warehouse_id = p.warehouse_id where s.user_id = ?
                    """);
        }

        private Set<Long> visibleDelivery() {
            return ids("warehouseDelivery", """
                    select d.delivery_task_id from mes_robot_delivery_task d join mes_picking_task p on p.picking_task_id = d.picking_task_id
                    join mes_user_warehouse_scope s on s.warehouse_id = p.warehouse_id where s.user_id = ?
                    """);
        }

        private Set<Long> visibleWorkOrdersForWarehouses() {
            return ids("warehouseWorkOrders", """
                    select distinct r.work_order_id from mes_material_requisition r join mes_user_warehouse_scope s
                    on s.warehouse_id = r.warehouse_id where s.user_id = ?
                    """);
        }

        private Set<Long> ids(String key, String sql) {
            return ids(key, sql, false);
        }

        private Set<Long> ids(String key, String sql, boolean repeatedUserParameter) {
            return cache.computeIfAbsent(key, ignored -> {
                try (Connection connection = Db.getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, user.user.userId);
                    if (repeatedUserParameter) statement.setLong(2, user.user.userId);
                    try (ResultSet rs = statement.executeQuery()) {
                        Set<Long> values = new LinkedHashSet<>();
                        while (rs.next()) values.add(rs.getLong(1));
                        return values;
                    }
                } catch (SQLException ex) {
                    throw database(ex);
                }
            });
        }

        private static BadRequestException denied(String type, long id) {
            return new BadRequestException("无权访问该" + type + "数据：" + id);
        }
    }
}
