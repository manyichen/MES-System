package com.example.messystem.planning.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.planning.dao.PlanningDao;
import com.example.messystem.planning.entity.MesCustomerOrder;
import com.example.messystem.planning.entity.MesProductionTask;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class ProductionTaskService {
    private final PlanningDao dao = new PlanningDao();

    public List<MesProductionTask> listTasks() {
        return database(dao::listTasks);
    }

    public MesProductionTask getTask(long taskId) {
        return database(() -> dao.findTask(taskId))
                .orElseThrow(() -> new NotFoundException("production task not found"));
    }

    public MesProductionTask createTask(MesProductionTask task) {
        requireId(task.orderId, "orderId is required");
        MesCustomerOrder order = database(() -> dao.findOrder(task.orderId))
                .orElseThrow(() -> new BadRequestException("order not found"));
        task.taskNo = task.taskNo == null || task.taskNo.isBlank() ? IdGenerator.nextCode("TASK") : task.taskNo;
        task.productId = task.productId == null ? order.productId : task.productId;
        task.planQty = task.planQty == null || task.planQty <= 0 ? order.orderQty : task.planQty;
        task.targetLineId = task.targetLineId == null ? firstLineId() : task.targetLineId;
        task.plannerId = task.plannerId == null ? 1L : task.plannerId;
        task.plannedStartTime = task.plannedStartTime == null ? LocalDateTime.now() : task.plannedStartTime;
        task.plannedEndTime = task.plannedEndTime == null ? task.plannedStartTime.plusDays(3) : task.plannedEndTime;
        task.taskStatus = "CREATED";
        task.kittingStatus = "PENDING";
        MesProductionTask created = database(() -> dao.insertTask(task));
        database(() -> {
            dao.updateOrderStatus(order.orderId, "PLANNED");
            return null;
        });
        return created;
    }

    public MesProductionTask releaseTask(long taskId) {
        MesProductionTask task = getTask(taskId);
        if (!"READY".equals(task.kittingStatus)) {
            throw new BadRequestException("kitting analysis must be READY before release");
        }
        if (!"CREATED".equals(task.taskStatus) && !"READY".equals(task.taskStatus)) {
            throw new BadRequestException("only CREATED or READY tasks can be released");
        }
        return database(() -> dao.releaseTask(taskId))
                .orElseThrow(() -> new NotFoundException("production task not found"));
    }

    private Long firstLineId() {
        long id = database(dao::firstLineId);
        return id == 0L ? null : id;
    }

    private static void requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BadRequestException(message);
        }
    }

    private static <T> T database(SqlCall<T> call) {
        try {
            return call.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T execute() throws SQLException;
    }
}
