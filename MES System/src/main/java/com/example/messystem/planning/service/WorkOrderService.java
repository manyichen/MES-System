package com.example.messystem.planning.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.MesWorkOrder;
import com.example.messystem.planning.entity.MesWorkOrderOperationLog;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WorkOrderService {
    public List<MesWorkOrder> listWorkOrders() {
        return new ArrayList<>(PlanningStore.workOrders.values());
    }

    public MesWorkOrder getWorkOrder(long workOrderId) {
        MesWorkOrder workOrder = PlanningStore.workOrders.get(workOrderId);
        if (workOrder == null) {
            throw new NotFoundException("work order not found");
        }
        return workOrder;
    }

    public MesWorkOrder createWorkOrder(MesWorkOrder workOrder) {
        requireId(workOrder.taskId, "taskId is required");
        MesProductionTask task = PlanningStore.tasks.get(workOrder.taskId);
        if (task == null) {
            throw new BadRequestException("production task not found");
        }
        if (!"RELEASED".equals(task.taskStatus)) {
            throw new BadRequestException("production task must be RELEASED before creating work order");
        }
        workOrder.workOrderId = PlanningStore.nextId();
        workOrder.workOrderNo = workOrder.workOrderNo == null || workOrder.workOrderNo.isBlank()
                ? IdGenerator.nextCode("WO")
                : workOrder.workOrderNo;
        workOrder.productId = workOrder.productId == null ? task.productId : workOrder.productId;
        workOrder.lineId = workOrder.lineId == null ? task.targetLineId : workOrder.lineId;
        workOrder.processId = workOrder.processId == null ? firstProcessId(task.productId) : workOrder.processId;
        requireId(workOrder.lineId, "lineId is required");
        requireId(workOrder.processId, "processId is required");
        workOrder.plannedQty = workOrder.plannedQty == null || workOrder.plannedQty <= 0 ? task.planQty : workOrder.plannedQty;
        workOrder.actualQty = workOrder.actualQty == null ? 0 : workOrder.actualQty;
        workOrder.priorityLevel = workOrder.priorityLevel == null ? 3 : workOrder.priorityLevel;
        workOrder.workOrderStatus = "CREATED";
        workOrder.batchNo = workOrder.batchNo == null || workOrder.batchNo.isBlank() ? IdGenerator.nextCode("BATCH") : workOrder.batchNo;
        workOrder.createdAt = LocalDateTime.now();
        workOrder.updatedAt = workOrder.createdAt;
        PlanningStore.workOrders.put(workOrder.workOrderId, workOrder);
        addLog(workOrder, "CREATE", null, "CREATED", null, "work order created");
        return workOrder;
    }

    public MesWorkOrder dispatch(long workOrderId, Long operatorId) {
        MesWorkOrder workOrder = getWorkOrder(workOrderId);
        if (!"CREATED".equals(workOrder.workOrderStatus)) {
            throw new BadRequestException("only CREATED work orders can be dispatched");
        }
        String fromStatus = workOrder.workOrderStatus;
        workOrder.workOrderStatus = "DISPATCHED";
        workOrder.dispatchTime = LocalDateTime.now();
        workOrder.updatedAt = workOrder.dispatchTime;
        addLog(workOrder, "DISPATCH", fromStatus, workOrder.workOrderStatus, operatorId, "work order dispatched");
        return workOrder;
    }

    public MesWorkOrder receive(long workOrderId, Long operatorId) {
        MesWorkOrder workOrder = getWorkOrder(workOrderId);
        if (!"DISPATCHED".equals(workOrder.workOrderStatus)) {
            throw new BadRequestException("only DISPATCHED work orders can be received");
        }
        String fromStatus = workOrder.workOrderStatus;
        workOrder.workOrderStatus = "RECEIVED";
        workOrder.receiveTime = LocalDateTime.now();
        workOrder.updatedAt = workOrder.receiveTime;
        addLog(workOrder, "RECEIVE", fromStatus, workOrder.workOrderStatus, operatorId, "work order received");
        return workOrder;
    }

    public List<MesWorkOrderOperationLog> listLogs(long workOrderId) {
        getWorkOrder(workOrderId);
        return PlanningStore.operationLogs.values().stream()
                .filter(item -> item.workOrderId != null && item.workOrderId == workOrderId)
                .toList();
    }

    private static Long firstProcessId(Long productId) {
        return PlanningStore.processRoutes.values().stream()
                .filter(item -> productId == null || item.productId == null || productId.equals(item.productId))
                .map(item -> item.processId)
                .findFirst()
                .orElse(null);
    }

    private static void addLog(MesWorkOrder workOrder, String operationType, String fromStatus, String toStatus,
            Long operatorId, String remark) {
        MesWorkOrderOperationLog log = new MesWorkOrderOperationLog();
        log.logId = PlanningStore.nextId();
        log.workOrderId = workOrder.workOrderId;
        log.operationType = operationType;
        log.operatorId = operatorId;
        log.fromStatus = fromStatus;
        log.toStatus = toStatus;
        log.remark = remark;
        log.operatedAt = LocalDateTime.now();
        PlanningStore.operationLogs.put(log.logId, log);
    }

    private static void requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BadRequestException(message);
        }
    }
}
