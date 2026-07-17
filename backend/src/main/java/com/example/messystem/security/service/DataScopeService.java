package com.example.messystem.security.service;

import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.security.dao.DataScopeDao;
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
import com.example.messystem.trace.entity.TireTraceItem;
import com.example.messystem.warehouse.entity.MesInventory;
import com.example.messystem.warehouse.entity.MesInventoryTransaction;
import com.example.messystem.warehouse.entity.MesMaterialRequisition;
import com.example.messystem.warehouse.entity.MesPickingTask;
import com.example.messystem.warehouse.entity.MesRobot;
import com.example.messystem.warehouse.entity.MesRobotDeliveryTask;
import com.example.messystem.warehouse.entity.MesWarehouse;
import com.example.messystem.warehouse.entity.MesWarehouseLocation;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataScopeService {
    private final DataScopeDao dao = new DataScopeDao();

    /** 创建请求级数据范围快照，并缓存授权检查所需的归属查询。 */
    public ScopeSnapshot snapshot(AuthenticatedUser user) {
        return new ScopeSnapshot(user, dao);
    }

    public UserDataScopes getUserScopes(long userId) {
        try {
            DataScopeDao.ScopeAssignments scopes = dao.findAssignments(userId);
            return new UserDataScopes(scopes.lineIds(), scopes.warehouseIds());
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    /** 在一个原子操作中替换用户全部显式产线和仓库授权。 */
    public UserDataScopes replaceUserScopes(long userId, List<Long> lineIds, List<Long> warehouseIds,
            long assignedBy) {
        Set<Long> lines = positiveIds(lineIds);
        Set<Long> warehouses = positiveIds(warehouseIds);
        try {
            dao.replaceAssignments(userId, lines, warehouses, assignedBy);
            return new UserDataScopes(lines, warehouses);
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    public void assignWarehouse(long userId, long warehouseId, long assignedBy) {
        try {
            dao.assignWarehouse(userId, warehouseId, assignedBy);
        } catch (SQLException ex) {
            throw database(ex);
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
        private final DataScopeDao dao;
        private final boolean lineRestricted;
        private final boolean warehouseRestricted;
        private final Map<String, Set<Long>> cache = new LinkedHashMap<>();

        private ScopeSnapshot(AuthenticatedUser user, DataScopeDao dao) {
            this.user = user;
            this.dao = dao;
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
            return ids("lines");
        }

        public Set<Long> warehouseIds() {
            return ids("warehouses");
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
            if (lineRestricted && !ids("lineTasks").contains(id)) throw denied("生产任务", id);
        }

        public void requireOrder(long id) {
            if (lineRestricted && !visibleOrdersForLines().contains(id)) throw denied("客户订单", id);
        }

        public void requireReport(long id) {
            if (lineRestricted && !ids("lineReports").contains(id)) throw denied("报工单", id);
        }

        public void requireInspection(long id) {
            if (lineRestricted && !ids("lineInspections").contains(id)) throw denied("质检单", id);
        }

        public void requireRework(long id) {
            if (lineRestricted && !ids("lineReworks").contains(id)) throw denied("返工单", id);
        }

        public void requireEquipment(long id) {
            if (lineRestricted && !visibleEquipment().contains(id)) throw denied("设备", id);
        }

        public void requireRepair(long id) {
            if (lineRestricted && !ids("lineRepairs").contains(id)) throw denied("设备报修单", id);
        }

        public void requireMaintenance(long id) {
            if (lineRestricted && !ids("lineMaintenance").contains(id)) throw denied("维修工单", id);
        }

        public void requireWarehouseEntity(String type, long id) {
            if (!warehouseRestricted) return;
            Set<Long> allowed = switch (type) {
                case "warehouse" -> warehouseIds();
                case "location" -> visibleLocations();
                case "inventory" -> visibleInventory();
                case "transaction" -> ids("warehouseTransactions");
                case "requisition" -> visibleRequisitions();
                case "picking" -> visiblePicking();
                case "delivery" -> visibleDelivery();
                case "robot" -> ids("warehouseRobots");
                default -> Set.of();
            };
            if (!allowed.contains(id)) throw denied(type, id);
        }

        private boolean canViewLineBusiness(Object item) {
            if (item instanceof MesWorkOrder value) return value.lineId != null && lineIds().contains(value.lineId);
            if (item instanceof MesProductionTask value) return value.targetLineId != null && lineIds().contains(value.targetLineId);
            if (item instanceof MesCustomerOrder value) return value.orderId != null && visibleOrdersForLines().contains(value.orderId);
            if (item instanceof MesWorkReport value) return value.workOrderId != null && visibleWorkOrdersForLines().contains(value.workOrderId);
            if (item instanceof MesPieceworkWage value) return value.wageId != null
                    && ids("lineWages").contains(value.wageId);
            if (item instanceof MesQualityInspection value) return value.workOrderId() != null && visibleWorkOrdersForLines().contains(value.workOrderId());
            if (item instanceof MesReworkOrder value) return value.reworkOrderId() != null
                    && ids("lineReworks").contains(value.reworkOrderId());
            if (item instanceof MesQualityTrace value) return value.workOrderId() != null && visibleWorkOrdersForLines().contains(value.workOrderId());
            if (item instanceof MesProductTrace value) return value.workOrderId() != null && visibleWorkOrdersForLines().contains(value.workOrderId());
            if (item instanceof TireTraceItem value) return value.workOrderId() != null && visibleWorkOrdersForLines().contains(value.workOrderId());
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
            if (item instanceof TireTraceItem value) return value.warehouseId() != null && warehouseIds().contains(value.warehouseId());
            return true;
        }

        private boolean canViewWarehouseRelatedProduction(Object item) {
            if (item instanceof MesWorkOrder value) return value.workOrderId != null && visibleWorkOrdersForWarehouses().contains(value.workOrderId);
            if (item instanceof MesWorkReport value) return value.workOrderId != null && visibleWorkOrdersForWarehouses().contains(value.workOrderId);
            if (item instanceof MesQualityInspection value) return value.workOrderId() != null && visibleWorkOrdersForWarehouses().contains(value.workOrderId());
            if (item instanceof MesProductTrace value) return value.workOrderId() != null && visibleWorkOrdersForWarehouses().contains(value.workOrderId());
            if (item instanceof MesQualityTrace value) return value.workOrderId() != null && visibleWorkOrdersForWarehouses().contains(value.workOrderId());
            if (item instanceof MesProductionTask value) return value.taskId != null
                    && ids("warehouseTasks").contains(value.taskId);
            if (item instanceof MesCustomerOrder value) return value.orderId != null
                    && ids("warehouseOrders").contains(value.orderId);
            return true;
        }

        private boolean isLineBusiness(Object item) {
            return item instanceof MesWorkOrder || item instanceof MesProductionTask || item instanceof MesCustomerOrder
                    || item instanceof MesWorkReport || item instanceof MesPieceworkWage
                    || item instanceof MesQualityInspection || item instanceof MesReworkOrder
                    || item instanceof MesQualityTrace || item instanceof MesProductTrace
                    || item instanceof TireTraceItem
                    || item instanceof MesManagementFeedback || item instanceof MesEquipment
                    || item instanceof MesEquipmentRepairReport || item instanceof MesMaintenanceOrder
                    || item instanceof MesMaintenancePlan || item instanceof MesMaterialRequisition;
        }

        private boolean isWarehouseBusiness(Object item) {
            return item instanceof MesWarehouse || item instanceof MesWarehouseLocation || item instanceof MesInventory
                    || item instanceof MesInventoryTransaction || item instanceof MesMaterialRequisition
                    || item instanceof MesPickingTask || item instanceof MesRobotDeliveryTask || item instanceof MesRobot
                    || item instanceof TireTraceItem;
        }

        private boolean isProductionReference(Object item) {
            return item instanceof MesWorkOrder || item instanceof MesProductionTask || item instanceof MesCustomerOrder
                    || item instanceof MesWorkReport || item instanceof MesQualityInspection
                    || item instanceof MesProductTrace || item instanceof MesQualityTrace;
        }

        private Set<Long> visibleWorkOrdersForLines() {
            return ids("lineWorkOrders");
        }

        private Set<Long> visibleOrdersForLines() {
            return ids("lineOrders");
        }

        private Set<Long> visibleEquipment() {
            return ids("lineEquipment");
        }

        private Set<Long> visibleLocations() {
            return ids("warehouseLocations");
        }

        private Set<Long> visibleInventory() {
            return ids("warehouseInventory");
        }

        private Set<Long> visibleRequisitions() {
            return ids("warehouseRequisitions");
        }

        private Set<Long> visiblePicking() {
            return ids("warehousePicking");
        }

        private Set<Long> visibleDelivery() {
            return ids("warehouseDelivery");
        }

        private Set<Long> visibleWorkOrdersForWarehouses() {
            return ids("warehouseWorkOrders");
        }

        private Set<Long> ids(String key) {
            return cache.computeIfAbsent(key, ignored -> {
                try {
                    return dao.findVisibleIds(key, user.user.userId);
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
