package com.example.messystem.planning.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.planning.dao.PlanningDao;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.MesWorkOrder;
import com.example.messystem.planning.entity.MesWorkOrderOperationLog;
import com.example.messystem.master.entity.MesUser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WorkOrderService {
    private final PlanningDao planningDao = new PlanningDao();

    public List<MesWorkOrder> listWorkOrders() {
        String sql = """
                select work_order_id, work_order_no, task_id, product_id, line_id, process_id,
                       planned_qty, actual_qty, priority_level, work_order_status, batch_no,
                       assigned_to, accepted_by,
                       dispatch_time, receive_time, completed_time, created_at, updated_at
                from mes_work_order
                order by work_order_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesWorkOrder> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapWorkOrder(rs));
            }
            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    public List<MesWorkOrder> listWorkOrdersForOperator(long userId) {
        String sql = """
                select work_order_id, work_order_no, task_id, product_id, line_id, process_id,
                       planned_qty, actual_qty, priority_level, work_order_status, batch_no,
                       assigned_to, accepted_by,
                       dispatch_time, receive_time, completed_time, created_at, updated_at
                from mes_work_order
                where assigned_to = ? or accepted_by = ?
                order by work_order_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MesWorkOrder> rows = new ArrayList<>();
                while (rs.next()) rows.add(mapWorkOrder(rs));
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    public List<MesUser> listDispatchableOperators() {
        String sql = """
                select distinct u.user_id, u.username, u.real_name, u.role_code, u.department, u.phone,
                       u.enabled, u.created_at, u.updated_at, u.last_login_at
                from mes_user u
                left join mes_user_role ur on ur.user_id = u.user_id
                left join mes_role r on r.role_id = ur.role_id and r.enabled = 1
                where u.enabled = 1
                  and (u.role_code = 'PRODUCTION_OPERATOR' or r.role_code = 'PRODUCTION_OPERATOR')
                order by u.user_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesUser> rows = new ArrayList<>();
            while (rs.next()) {
                MesUser user = new MesUser();
                user.userId = rs.getLong("user_id");
                user.username = rs.getString("username");
                user.realName = rs.getString("real_name");
                user.roleCode = rs.getString("role_code");
                user.department = rs.getString("department");
                user.phone = rs.getString("phone");
                user.enabled = rs.getInt("enabled");
                user.createdAt = getLocalDateTime(rs, "created_at");
                user.updatedAt = getLocalDateTime(rs, "updated_at");
                user.lastLoginAt = getLocalDateTime(rs, "last_login_at");
                rows.add(user);
            }
            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    public MesWorkOrder getWorkOrder(long workOrderId) {
        String sql = """
                select work_order_id, work_order_no, task_id, product_id, line_id, process_id,
                       planned_qty, actual_qty, priority_level, work_order_status, batch_no,
                       assigned_to, accepted_by,
                       dispatch_time, receive_time, completed_time, created_at, updated_at
                from mes_work_order
                where work_order_id = ?
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, workOrderId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("work order not found");
                }
                return mapWorkOrder(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    public MesWorkOrder getWorkOrderForOperator(long workOrderId, long userId) {
        String sql = """
                select work_order_id, work_order_no, task_id, product_id, line_id, process_id,
                       planned_qty, actual_qty, priority_level, work_order_status, batch_no,
                       assigned_to, accepted_by,
                       dispatch_time, receive_time, completed_time, created_at, updated_at
                from mes_work_order
                where work_order_id = ? and (assigned_to = ? or accepted_by = ?)
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, workOrderId);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("work order not found");
                return mapWorkOrder(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    public MesWorkOrder createWorkOrder(MesWorkOrder workOrder) {
        return createWorkOrder(workOrder, 1L);
    }

    public MesWorkOrder createWorkOrder(MesWorkOrder workOrder, Long actorId) {
        requireId(actorId, "actorId is required");
        requireId(workOrder.taskId, "taskId is required");
        MesProductionTask task = database(() -> planningDao.findTask(workOrder.taskId))
                .orElseThrow(() -> new BadRequestException("production task not found"));
        if (!"RELEASED".equals(task.taskStatus)) {
            throw new BadRequestException("production task must be RELEASED before creating work order");
        }
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
        String sql = """
                insert into mes_work_order
                    (work_order_no, task_id, product_id, line_id, process_id, planned_qty,
                     actual_qty, priority_level, work_order_status, batch_no)
                values (?, ?, ?, ?, ?, ?, ?, ?, 'CREATED', ?)
                returning work_order_id, work_order_no, task_id, product_id, line_id, process_id,
                          planned_qty, actual_qty, priority_level, work_order_status, batch_no,
                          assigned_to, accepted_by,
                          dispatch_time, receive_time, completed_time, created_at, updated_at
                """;
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, workOrder.workOrderNo);
                statement.setLong(2, workOrder.taskId);
                setLong(statement, 3, workOrder.productId);
                statement.setLong(4, workOrder.lineId);
                setLong(statement, 5, workOrder.processId);
                statement.setInt(6, workOrder.plannedQty);
                statement.setInt(7, workOrder.actualQty);
                statement.setInt(8, workOrder.priorityLevel);
                statement.setString(9, workOrder.batchNo);
                MesWorkOrder created;
                try (ResultSet rs = statement.executeQuery()) {
                    rs.next();
                    created = mapWorkOrder(rs);
                }
                addLog(connection, created.workOrderId, "CREATE", null, "CREATED", actorId, "创建生产工单");
                connection.commit();
                return created;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    public MesWorkOrder dispatch(long workOrderId, Long operatorId) {
        return changeStatus(workOrderId, "CREATED", "DISPATCHED", "DISPATCH", operatorId, operatorId,
                "生产工单已派发");
    }

    public MesWorkOrder dispatch(long workOrderId, Long assigneeId, Long actorId) {
        requireProductionOperator(assigneeId);
        return changeStatus(workOrderId, "CREATED", "DISPATCHED", "DISPATCH", assigneeId, actorId,
                "生产工单已派发给用户 " + assigneeId);
    }

    public MesWorkOrder receive(long workOrderId, Long operatorId) {
        return changeStatus(workOrderId, "DISPATCHED", "RECEIVED", "RECEIVE", operatorId, operatorId,
                "生产工单已接收");
    }

    public List<MesWorkOrderOperationLog> listLogs(long workOrderId) {
        getWorkOrder(workOrderId);
        String sql = """
                select operation_log_id, work_order_id, operation_type, before_status,
                       after_status, operator_id, operation_reason, operation_time
                from mes_work_order_operation_log
                where work_order_id = ?
                order by operation_time asc, operation_log_id asc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, workOrderId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MesWorkOrderOperationLog> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapLog(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    private MesWorkOrder changeStatus(long workOrderId, String expectedStatus, String nextStatus,
            String operationType, Long operatorId, Long actorId, String remark) {
        requireId(operatorId, "operatorId is required");
        requireId(actorId, "actorId is required");
        String timeColumn = "DISPATCHED".equals(nextStatus) ? "dispatch_time" : "receive_time";
        String actorColumn = "DISPATCHED".equals(nextStatus) ? "assigned_to" : "accepted_by";
        String ownershipCondition = "RECEIVED".equals(nextStatus) ? "and assigned_to = ?" : "";
        String sql = """
                update mes_work_order
                set work_order_status = ?,
                    %s = ?,
                    %s = current_timestamp,
                    updated_at = current_timestamp
                where work_order_id = ? and work_order_status = ?
                    %s
                returning work_order_id, work_order_no, task_id, product_id, line_id, process_id,
                          planned_qty, actual_qty, priority_level, work_order_status, batch_no,
                          assigned_to, accepted_by,
                          dispatch_time, receive_time, completed_time, created_at, updated_at
                """.formatted(actorColumn, timeColumn, ownershipCondition);
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, nextStatus);
                statement.setLong(2, operatorId);
                statement.setLong(3, workOrderId);
                statement.setString(4, expectedStatus);
                if ("RECEIVED".equals(nextStatus)) statement.setLong(5, operatorId);
                MesWorkOrder workOrder;
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        getWorkOrder(workOrderId);
                        throw new BadRequestException("only " + expectedStatus + " work orders can be changed to " + nextStatus);
                    }
                    workOrder = mapWorkOrder(rs);
                }
                addLog(connection, workOrderId, operationType, expectedStatus, nextStatus, actorId, remark);
                connection.commit();
                return workOrder;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    private static void requireProductionOperator(Long userId) {
        requireId(userId, "operatorId is required");
        String sql = """
                select 1
                from mes_user u
                left join mes_user_role ur on ur.user_id = u.user_id
                left join mes_role r on r.role_id = ur.role_id and r.enabled = 1
                where u.user_id = ? and u.enabled = 1
                  and (u.role_code = 'PRODUCTION_OPERATOR' or r.role_code = 'PRODUCTION_OPERATOR')
                limit 1
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new BadRequestException("operatorId must be an enabled production operator");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    private static Long firstProcessId(Long productId) {
        try {
            long processId = new PlanningDao().firstProcessId(productId);
            return processId == 0L ? null : processId;
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
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

    private static void addLog(Connection connection, long workOrderId, String operationType, String fromStatus, String toStatus,
            Long operatorId, String remark) throws SQLException {
        String sql = """
                insert into mes_work_order_operation_log
                    (work_order_id, operation_type, before_status, after_status, operator_id, operation_reason)
                values (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, workOrderId);
            statement.setString(2, operationType);
            statement.setString(3, fromStatus);
            statement.setString(4, toStatus);
            if (operatorId == null) statement.setNull(5, java.sql.Types.BIGINT);
            else statement.setLong(5, operatorId);
            statement.setString(6, remark);
            statement.executeUpdate();
        }
    }

    private static void requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BadRequestException(message);
        }
    }

    private static MesWorkOrder mapWorkOrder(ResultSet rs) throws SQLException {
        MesWorkOrder item = new MesWorkOrder();
        item.workOrderId = rs.getLong("work_order_id");
        item.workOrderNo = rs.getString("work_order_no");
        item.taskId = rs.getLong("task_id");
        item.productId = getLong(rs, "product_id");
        item.lineId = rs.getLong("line_id");
        item.processId = getLong(rs, "process_id");
        item.plannedQty = rs.getInt("planned_qty");
        item.actualQty = rs.getInt("actual_qty");
        item.priorityLevel = rs.getInt("priority_level");
        item.workOrderStatus = rs.getString("work_order_status");
        item.batchNo = rs.getString("batch_no");
        item.assignedTo = getLong(rs, "assigned_to");
        item.acceptedBy = getLong(rs, "accepted_by");
        item.dispatchTime = getLocalDateTime(rs, "dispatch_time");
        item.receiveTime = getLocalDateTime(rs, "receive_time");
        item.completedTime = getLocalDateTime(rs, "completed_time");
        item.createdAt = getLocalDateTime(rs, "created_at");
        item.updatedAt = getLocalDateTime(rs, "updated_at");
        return item;
    }

    private static MesWorkOrderOperationLog mapLog(ResultSet rs) throws SQLException {
        MesWorkOrderOperationLog item = new MesWorkOrderOperationLog();
        item.logId = rs.getLong("operation_log_id");
        item.workOrderId = rs.getLong("work_order_id");
        item.operationType = rs.getString("operation_type");
        item.fromStatus = rs.getString("before_status");
        item.toStatus = rs.getString("after_status");
        item.operatorId = rs.getLong("operator_id");
        item.remark = rs.getString("operation_reason");
        item.operatedAt = getLocalDateTime(rs, "operation_time");
        return item;
    }

    private static void setLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
