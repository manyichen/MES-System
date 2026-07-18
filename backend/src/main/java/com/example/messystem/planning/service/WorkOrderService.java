/*
 * 答辩定位：订单、计划、齐套与工单 模块的 WorkOrderService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.master.entity.MesProcessRoute;
import com.example.messystem.planning.dao.PlanningDao;
import com.example.messystem.planning.dao.WorkOrderDao;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.MesWorkOrder;
import com.example.messystem.planning.entity.MesWorkOrderOperationLog;
import com.example.messystem.master.entity.MesUser;
import java.sql.SQLException;
import java.util.List;

/**
 * 编排生产工单业务规则，全部持久化操作交给 DAO。
 * 主状态流为 CREATED -> DISPATCHED -> RECEIVED；被派工人在接收前可拒绝 DISPATCHED 工单。
 */
public class WorkOrderService {
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final PlanningDao planningDao = new PlanningDao();
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final WorkOrderDao workOrderDao = new WorkOrderDao();

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesWorkOrder> listWorkOrders() {
        return workOrderDao.listWorkOrders();
    }

    /** 仅返回该操作工已接收或当前被派发的工单。 */
    public List<MesWorkOrder> listWorkOrdersForOperator(long userId) {
        return workOrderDao.listWorkOrdersForOperator(userId);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesUser> listDispatchableOperators() {
        return workOrderDao.listDispatchableOperators();
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWorkOrder getWorkOrder(long workOrderId) {
        return workOrderDao.getWorkOrder(workOrderId);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWorkOrder getWorkOrderForOperator(long workOrderId, long userId) {
        return workOrderDao.getWorkOrderForOperator(workOrderId, userId);
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWorkOrder createWorkOrder(MesWorkOrder workOrder) {
        return createWorkOrder(workOrder, 1L);
    }

    /**
     * 校验齐套状态、产线和工艺路线并补齐默认值，再由 DAO 原子完成工单创建、
     * 生产任务下达和操作日志写入。
     */
    public MesWorkOrder createWorkOrder(MesWorkOrder workOrder, Long actorId) {
        if (workOrder == null) throw new BadRequestException("work order is required");
        requireId(actorId, "actorId is required");
        requireId(workOrder.taskId, "taskId is required");

        MesProductionTask task = database(() -> planningDao.findTask(workOrder.taskId))
                .orElseThrow(() -> new BadRequestException("production task not found"));
        if (!"READY".equals(task.taskStatus) || !"READY".equals(task.kittingStatus)) {
            throw new BadRequestException("production task must pass kitting analysis before creating work order");
        }

        workOrder.lineId = workOrder.lineId == null || workOrder.lineId <= 0
                ? task.targetLineId
                : workOrder.lineId;
        requireId(workOrder.lineId, "lineId is required");
        requireAvailableLine(workOrder.lineId);
        workOrder.processId = workOrder.processId == null || workOrder.processId <= 0
                ? firstEnabledProcessForProduct(task.productId)
                : workOrder.processId;
        requireProcessForProduct(workOrder.processId, task.productId);

        workOrder.workOrderNo = workOrder.workOrderNo == null || workOrder.workOrderNo.isBlank()
                ? IdGenerator.nextCode("WO") : workOrder.workOrderNo;
        workOrder.productId = workOrder.productId == null ? task.productId : workOrder.productId;
        workOrder.plannedQty = workOrder.plannedQty == null || workOrder.plannedQty <= 0
                ? task.planQty : workOrder.plannedQty;
        workOrder.actualQty = workOrder.actualQty == null ? 0 : workOrder.actualQty;
        workOrder.priorityLevel = workOrder.priorityLevel == null ? 3 : workOrder.priorityLevel;
        workOrder.workOrderStatus = "CREATED";
        workOrder.batchNo = workOrder.batchNo == null || workOrder.batchNo.isBlank()
                ? IdGenerator.nextCode("BATCH") : workOrder.batchNo;
        return workOrderDao.createWorkOrder(workOrder, actorId);
    }

    /** 兼容派工人将工单派给自己的调用方式。 */
    public MesWorkOrder dispatch(long workOrderId, Long operatorId) {
        requireId(operatorId, "operatorId is required");
        return workOrderDao.changeStatus(workOrderId, "CREATED", "DISPATCHED", "DISPATCH",
                operatorId, operatorId, "生产工单已派发");
    }

    /** 将 CREATED 工单派发给已启用的生产操作工。 */
    public MesWorkOrder dispatch(long workOrderId, Long assigneeId, Long actorId) {
        requireId(assigneeId, "operatorId is required");
        requireId(actorId, "actorId is required");
        if (!workOrderDao.isEnabledProductionOperator(assigneeId)) {
            throw new BadRequestException("operatorId must be an enabled production operator");
        }
        return workOrderDao.changeStatus(workOrderId, "CREATED", "DISPATCHED", "DISPATCH",
                assigneeId, actorId, "生产工单已派发给用户 " + assigneeId);
    }

    /** 仅允许当前操作工接收已派给本人的 DISPATCHED 工单。 */
    public MesWorkOrder receive(long workOrderId, Long operatorId) {
        return receive(workOrderId, operatorId, false);
    }

    /**
     * 业务用例：接收已派发任务。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesWorkOrder receive(long workOrderId, Long operatorId, boolean allowAdministrativeTakeover) {
        requireId(operatorId, "operatorId is required");
        return workOrderDao.changeStatus(workOrderId, "DISPATCHED", "RECEIVED", "RECEIVE",
                operatorId, operatorId, allowAdministrativeTakeover ? "超级管理员接管生产工单" : "生产工单已接收",
                !allowAdministrativeTakeover);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesWorkOrderOperationLog> listLogsForOperator(long userId) {
        return workOrderDao.listLogsForOperator(userId);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesWorkOrderOperationLog> listAllLogs() {
        return workOrderDao.listAllLogs();
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesWorkOrderOperationLog> listLogs(long workOrderId) {
        return workOrderDao.listLogs(workOrderId);
    }

    /**
     * 业务用例：执行 requireAvailableLine 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private void requireAvailableLine(Long lineId) {
        boolean available = database(planningDao::listProductionLines).stream()
                .anyMatch(line -> lineId.equals(line.lineId)
                        && line.enabled != null && line.enabled == 1
                        && !"FAULT".equals(line.lineStatus)
                        && !"DISABLED".equals(line.lineStatus));
        if (!available) throw new BadRequestException("lineId must be an available production line");
    }

    /**
     * 业务用例：执行 requireProcessForProduct 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private void requireProcessForProduct(Long processId, Long productId) {
        boolean matched = database(planningDao::listProcessRoutes).stream()
                .anyMatch(route -> processId.equals(route.processId)
                        && route.enabled != null && route.enabled == 1
                        && (route.productId == null || route.productId.equals(productId)));
        if (!matched) throw new BadRequestException("processId must match the production task product");
    }

    /**
     * 业务用例：执行 firstEnabledProcessForProduct 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private Long firstEnabledProcessForProduct(Long productId) {
        List<MesProcessRoute> routes = database(planningDao::listProcessRoutes);
        return routes.stream()
                .filter(route -> route.enabled != null && route.enabled == 1)
                .filter(route -> route.productId != null && route.productId.equals(productId))
                .findFirst()
                .or(() -> routes.stream()
                        .filter(route -> route.enabled != null && route.enabled == 1)
                        .filter(route -> route.productId == null)
                        .findFirst())
                .map(route -> route.processId)
                .orElseThrow(() -> new BadRequestException("production task product has no enabled process route"));
    }

    /**
     * 业务用例：执行 requireId 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static void requireId(Long id, String message) {
        if (id == null || id <= 0) throw new BadRequestException(message);
    }

    /**
     * 业务用例：执行 database 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static <T> T database(SqlCall<T> call) {
        try {
            return call.execute();
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T execute() throws SQLException;
    }
}
