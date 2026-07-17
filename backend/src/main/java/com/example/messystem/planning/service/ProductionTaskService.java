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
        requireId(task.plannerId, "plannerId is required");
        MesCustomerOrder order = database(() -> dao.findOrder(task.orderId))
                .orElseThrow(() -> new BadRequestException("order not found"));
        if (order.productId == null || order.productId <= 0) {
            throw new BadRequestException("客户订单未关联产品，不能创建生产任务");
        }
        task.taskNo = task.taskNo == null || task.taskNo.isBlank() ? IdGenerator.nextCode("TASK") : task.taskNo;
        task.productId = order.productId;
        task.planQty = task.planQty == null || task.planQty <= 0 ? order.orderQty : task.planQty;
        if (task.planQty == null || task.planQty <= 0) throw new BadRequestException("planQty must be greater than 0");
        task.plannedStartTime = task.plannedStartTime == null ? LocalDateTime.now() : task.plannedStartTime;
        task.plannedEndTime = task.plannedEndTime == null ? task.plannedStartTime.plusDays(3) : task.plannedEndTime;
        if (!task.plannedEndTime.isAfter(task.plannedStartTime)) {
            throw new BadRequestException("plannedEndTime must be after plannedStartTime");
        }
        task.taskStatus = "CREATED";
        task.kittingStatus = "PENDING";
        MesProductionTask created = database(() -> dao.insertTask(task));
        database(() -> {
            dao.updateOrderStatus(order.orderId, "PLANNED");
            return null;
        });
        return created;
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
