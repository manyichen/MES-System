package com.example.messystem.planning.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
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
    private final PlanningDao planningDao = new PlanningDao();
    private final WorkOrderDao workOrderDao = new WorkOrderDao();

    public List<MesWorkOrder> listWorkOrders() {
        return workOrderDao.listWorkOrders();
    }

    /** 仅返回该操作工已接收或当前被派发的工单。 */
    public List<MesWorkOrder> listWorkOrdersForOperator(long userId) {
        return workOrderDao.listWorkOrdersForOperator(userId);
    }

    public List<MesUser> listDispatchableOperators() {
        return workOrderDao.listDispatchableOperators();
    }

    public MesWorkOrder getWorkOrder(long workOrderId) {
        return workOrderDao.getWorkOrder(workOrderId);
    }

    public MesWorkOrder getWorkOrderForOperator(long workOrderId, long userId) {
        return workOrderDao.getWorkOrderForOperator(workOrderId, userId);
    }

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

        requireId(workOrder.lineId, "lineId is required");
        requireId(workOrder.processId, "processId is required");
        requireAvailableLine(workOrder.lineId);
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
        requireId(operatorId, "operatorId is required");
        return workOrderDao.changeStatus(workOrderId, "DISPATCHED", "RECEIVED", "RECEIVE",
                operatorId, operatorId, "生产工单已接收");
    }

    public MesWorkOrder reject(long workOrderId, Long operatorId) {
        requireId(operatorId, "operatorId is required");
        return workOrderDao.reject(workOrderId, operatorId);
    }

    public List<MesWorkOrderOperationLog> listLogsForOperator(long userId) {
        return workOrderDao.listLogsForOperator(userId);
    }

    public List<MesWorkOrderOperationLog> listAllLogs() {
        return workOrderDao.listAllLogs();
    }

    public List<MesWorkOrderOperationLog> listLogs(long workOrderId) {
        return workOrderDao.listLogs(workOrderId);
    }

    private void requireAvailableLine(Long lineId) {
        boolean available = database(planningDao::listProductionLines).stream()
                .anyMatch(line -> lineId.equals(line.lineId)
                        && line.enabled != null && line.enabled == 1
                        && !"FAULT".equals(line.lineStatus)
                        && !"DISABLED".equals(line.lineStatus));
        if (!available) throw new BadRequestException("lineId must be an available production line");
    }

    private void requireProcessForProduct(Long processId, Long productId) {
        boolean matched = database(planningDao::listProcessRoutes).stream()
                .anyMatch(route -> processId.equals(route.processId)
                        && route.enabled != null && route.enabled == 1
                        && (route.productId == null || route.productId.equals(productId)));
        if (!matched) throw new BadRequestException("processId must match the production task product");
    }

    private static void requireId(Long id, String message) {
        if (id == null || id <= 0) throw new BadRequestException(message);
    }

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
