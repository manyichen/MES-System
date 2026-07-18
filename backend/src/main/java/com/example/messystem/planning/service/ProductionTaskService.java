/*
 * 答辩定位：订单、计划、齐套与工单 模块的 ProductionTaskService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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

/**
 * 订单、计划、齐套与工单 的 ProductionTaskService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class ProductionTaskService {
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final PlanningDao dao = new PlanningDao();

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesProductionTask> listTasks() {
        return database(dao::listTasks);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesProductionTask getTask(long taskId) {
        return database(() -> dao.findTask(taskId))
                .orElseThrow(() -> new NotFoundException("production task not found"));
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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

    /**
     * 业务用例：执行 requireId 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static void requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BadRequestException(message);
        }
    }

    /**
     * 业务用例：执行 database 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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
