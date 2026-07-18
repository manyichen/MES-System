/*
 * 答辩定位：授权策略与数据范围 模块的 DataScopeService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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

/**
 * 授权策略与数据范围 的 DataScopeService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class DataScopeService {
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final DataScopeDao dao = new DataScopeDao();

    /** 创建请求级数据范围快照，并缓存授权检查所需的归属查询。 */
    public ScopeSnapshot snapshot(AuthenticatedUser user) {
        return new ScopeSnapshot(user, dao);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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

    /**
     * 业务用例：分配执行人员或资源。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public void assignWarehouse(long userId, long warehouseId, long assignedBy) {
        try {
            dao.assignWarehouse(userId, warehouseId, assignedBy);
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    /**
     * 业务用例：执行 positiveIds 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static Set<Long> positiveIds(List<Long> ids) {
        Set<Long> result = new LinkedHashSet<>();
        if (ids != null) ids.stream().filter(id -> id != null && id > 0).forEach(result::add);
        return result;
    }

    /**
     * 业务用例：执行 database 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static IllegalStateException database(SQLException ex) {
        return new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
    }

    /**
     * 业务用例：执行 UserDataScopes 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public record UserDataScopes(Set<Long> lineIds, Set<Long> warehouseIds) {
    }

    public static final class ScopeSnapshot {
        private final AuthenticatedUser user;
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
        private final DataScopeDao dao;
        private final boolean lineRestricted;
        private final boolean warehouseRestricted;
        private final Map<String, Set<Long>> cache = new LinkedHashMap<>();

        /**
         * 业务用例：执行 ScopeSnapshot 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private ScopeSnapshot(AuthenticatedUser user, DataScopeDao dao) {
            this.user = user;
            this.dao = dao;
            this.lineRestricted = user.hasRole("WORKSHOP_MANAGER") && !user.hasRole("SYSTEM_ADMIN");
            this.warehouseRestricted = user.hasRole("WAREHOUSE_ADMIN") && !user.hasRole("SYSTEM_ADMIN");
        }

        /**
         * 业务用例：执行 restricted 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public boolean restricted() {
            return lineRestricted || warehouseRestricted;
        }

        /**
         * 业务用例：执行 lineRestricted 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public boolean lineRestricted() {
            return lineRestricted;
        }

        /**
         * 业务用例：执行 warehouseRestricted 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public boolean warehouseRestricted() {
            return warehouseRestricted;
        }

        /**
         * 业务用例：执行 lineIds 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public Set<Long> lineIds() {
            return ids("lines");
        }

        /**
         * 业务用例：执行 warehouseIds 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public Set<Long> warehouseIds() {
            return ids("warehouses");
        }

        /**
         * 业务用例：执行 canView 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public boolean canView(Object item) {
            if (!restricted() || item == null) return true;
            if (lineRestricted && isLineBusiness(item)) return canViewLineBusiness(item);
            if (warehouseRestricted && isWarehouseBusiness(item)) return canViewWarehouseBusiness(item);
            if (warehouseRestricted && isProductionReference(item)) return canViewWarehouseRelatedProduction(item);
            return true;
        }

        /**
         * 业务用例：执行 requireLine 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public void requireLine(long lineId) {
            if (lineRestricted && !lineIds().contains(lineId)) throw denied("产线", lineId);
        }

        /**
         * 业务用例：执行 requireWarehouse 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public void requireWarehouse(long warehouseId) {
            if (warehouseRestricted && !warehouseIds().contains(warehouseId)) throw denied("仓库", warehouseId);
        }

        /**
         * 业务用例：执行 requireWorkOrder 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public void requireWorkOrder(long id) {
            if (lineRestricted && !visibleWorkOrdersForLines().contains(id)) throw denied("生产工单", id);
            if (warehouseRestricted && !visibleWorkOrdersForWarehouses().contains(id)) throw denied("生产工单", id);
        }

        /**
         * 业务用例：执行 requireTask 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public void requireTask(long id) {
            if (lineRestricted && !ids("lineTasks").contains(id)) throw denied("生产任务", id);
        }

        /**
         * 业务用例：执行 requireOrder 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public void requireOrder(long id) {
            if (lineRestricted && !visibleOrdersForLines().contains(id)) throw denied("客户订单", id);
        }

        /**
         * 业务用例：执行 requireReport 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public void requireReport(long id) {
            if (lineRestricted && !ids("lineReports").contains(id)) throw denied("报工单", id);
        }

        /**
         * 业务用例：执行 requireInspection 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public void requireInspection(long id) {
            if (lineRestricted && !ids("lineInspections").contains(id)) throw denied("质检单", id);
        }

        /**
         * 业务用例：执行 requireRework 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public void requireRework(long id) {
            if (lineRestricted && !ids("lineReworks").contains(id)) throw denied("返工单", id);
        }

        /**
         * 业务用例：执行 requireEquipment 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public void requireEquipment(long id) {
            if (lineRestricted && !visibleEquipment().contains(id)) throw denied("设备", id);
        }

        /**
         * 业务用例：执行 requireRepair 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public void requireRepair(long id) {
            if (lineRestricted && !ids("lineRepairs").contains(id)) throw denied("设备报修单", id);
        }

        /**
         * 业务用例：执行 requireMaintenance 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        public void requireMaintenance(long id) {
            if (lineRestricted && !ids("lineMaintenance").contains(id)) throw denied("维修工单", id);
        }

        /**
         * 业务用例：执行 requireWarehouseEntity 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
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

        /**
         * 业务用例：执行 canViewLineBusiness 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
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

        /**
         * 业务用例：执行 canViewWarehouseBusiness 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
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

        /**
         * 业务用例：执行 canViewWarehouseRelatedProduction 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
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

        /**
         * 业务用例：执行 isLineBusiness 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
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

        /**
         * 业务用例：执行 isWarehouseBusiness 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private boolean isWarehouseBusiness(Object item) {
            return item instanceof MesWarehouse || item instanceof MesWarehouseLocation || item instanceof MesInventory
                    || item instanceof MesInventoryTransaction || item instanceof MesMaterialRequisition
                    || item instanceof MesPickingTask || item instanceof MesRobotDeliveryTask || item instanceof MesRobot
                    || item instanceof TireTraceItem;
        }

        /**
         * 业务用例：执行 isProductionReference 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private boolean isProductionReference(Object item) {
            return item instanceof MesWorkOrder || item instanceof MesProductionTask || item instanceof MesCustomerOrder
                    || item instanceof MesWorkReport || item instanceof MesQualityInspection
                    || item instanceof MesProductTrace || item instanceof MesQualityTrace;
        }

        /**
         * 业务用例：执行 visibleWorkOrdersForLines 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private Set<Long> visibleWorkOrdersForLines() {
            return ids("lineWorkOrders");
        }

        /**
         * 业务用例：执行 visibleOrdersForLines 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private Set<Long> visibleOrdersForLines() {
            return ids("lineOrders");
        }

        /**
         * 业务用例：执行 visibleEquipment 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private Set<Long> visibleEquipment() {
            return ids("lineEquipment");
        }

        /**
         * 业务用例：执行 visibleLocations 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private Set<Long> visibleLocations() {
            return ids("warehouseLocations");
        }

        /**
         * 业务用例：执行 visibleInventory 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private Set<Long> visibleInventory() {
            return ids("warehouseInventory");
        }

        /**
         * 业务用例：执行 visibleRequisitions 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private Set<Long> visibleRequisitions() {
            return ids("warehouseRequisitions");
        }

        /**
         * 业务用例：执行 visiblePicking 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private Set<Long> visiblePicking() {
            return ids("warehousePicking");
        }

        /**
         * 业务用例：执行 visibleDelivery 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private Set<Long> visibleDelivery() {
            return ids("warehouseDelivery");
        }

        /**
         * 业务用例：执行 visibleWorkOrdersForWarehouses 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private Set<Long> visibleWorkOrdersForWarehouses() {
            return ids("warehouseWorkOrders");
        }

        /**
         * 业务用例：执行 ids 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private Set<Long> ids(String key) {
            return cache.computeIfAbsent(key, ignored -> {
                try {
                    return dao.findVisibleIds(key, user.user.userId);
                } catch (SQLException ex) {
                    throw database(ex);
                }
            });
        }

        /**
         * 业务用例：执行 denied 对应的业务步骤。
         * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
         * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
         */
        private static BadRequestException denied(String type, long id) {
            return new BadRequestException("无权访问该" + type + "数据：" + id);
        }
    }
}
