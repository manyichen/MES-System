package com.example.messystem.planning.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.planning.entity.MesCustomerOrder;
import com.example.messystem.planning.entity.MesProductionTask;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductionTaskService {
    public List<MesProductionTask> listTasks() {
        return new ArrayList<>(PlanningStore.tasks.values());
    }

    public MesProductionTask getTask(long taskId) {
        MesProductionTask task = PlanningStore.tasks.get(taskId);
        if (task == null) {
            throw new NotFoundException("production task not found");
        }
        return task;
    }

    public MesProductionTask createTask(MesProductionTask task) {
        requireId(task.orderId, "orderId is required");
        MesCustomerOrder order = PlanningStore.orders.get(task.orderId);
        if (order == null) {
            throw new BadRequestException("order not found");
        }
        task.taskId = PlanningStore.nextId();
        task.taskNo = task.taskNo == null || task.taskNo.isBlank() ? IdGenerator.nextCode("TASK") : task.taskNo;
        task.productId = task.productId == null ? order.productId : task.productId;
        task.planQty = task.planQty == null || task.planQty <= 0 ? order.orderQty : task.planQty;
        task.targetLineId = task.targetLineId == null ? firstLineId() : task.targetLineId;
        task.taskStatus = "CREATED";
        task.kittingStatus = "PENDING";
        task.createdAt = LocalDateTime.now();
        task.updatedAt = task.createdAt;
        PlanningStore.tasks.put(task.taskId, task);
        order.orderStatus = "PLANNED";
        order.updatedAt = LocalDateTime.now();
        return task;
    }

    public MesProductionTask releaseTask(long taskId) {
        MesProductionTask task = getTask(taskId);
        if (!"READY".equals(task.kittingStatus)) {
            throw new BadRequestException("kitting analysis must be READY before release");
        }
        if (!"CREATED".equals(task.taskStatus) && !"READY".equals(task.taskStatus)) {
            throw new BadRequestException("only CREATED or READY tasks can be released");
        }
        task.taskStatus = "RELEASED";
        task.releaseTime = LocalDateTime.now();
        task.updatedAt = task.releaseTime;
        return task;
    }

    private static Long firstLineId() {
        return PlanningStore.productionLines.keySet().stream().findFirst().orElse(null);
    }

    private static void requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BadRequestException(message);
        }
    }
}
