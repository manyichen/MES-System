package com.example.messystem.planning.dao;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.NotFoundException;
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

/**
 * 封装生产工单持久化，包括原子状态变更和操作日志。
 * 业务校验与流程决策由 {@code WorkOrderService} 负责。
 */
public class WorkOrderDao {

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
                where accepted_by = ?
                   or (work_order_status in ('DISPATCHED', 'REJECTED')
                       and accepted_by is null
                       and assigned_to = ?)
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
                where work_order_id = ?
                  and (
                      accepted_by = ?
                      or (work_order_status in ('DISPATCHED', 'REJECTED')
                          and accepted_by is null
                          and assigned_to = ?)
                  )
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

    /** 原子完成工单创建、生产任务下达和审计日志写入。 */
    public MesWorkOrder createWorkOrder(MesWorkOrder workOrder, Long actorId) {
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
                releaseTaskForConfirmedWorkOrder(connection, workOrder.taskId);
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

    public List<MesWorkOrderOperationLog> listLogsForOperator(long userId) {
        String sql = """
                select l.operation_log_id, l.work_order_id, l.operation_type, l.before_status,
                       l.after_status, l.operator_id, l.operation_reason, l.operation_time,
                       w.work_order_no
                from mes_work_order_operation_log l
                left join mes_work_order w on w.work_order_id = l.work_order_id
                where l.operator_id = ?
                order by l.operation_time desc, l.operation_log_id desc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                List<MesWorkOrderOperationLog> rows = new ArrayList<>();
                while (rs.next()) rows.add(mapLogWithWorkOrder(rs));
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    public List<MesWorkOrderOperationLog> listAllLogs() {
        String sql = """
                select l.operation_log_id, l.work_order_id, l.operation_type, l.before_status,
                       l.after_status, l.operator_id, l.operation_reason, l.operation_time,
                       w.work_order_no
                from mes_work_order_operation_log l
                left join mes_work_order w on w.work_order_id = l.work_order_id
                order by l.operation_time desc, l.operation_log_id desc
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesWorkOrderOperationLog> rows = new ArrayList<>();
            while (rs.next()) rows.add(mapLogWithWorkOrder(rs));
            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
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

    /** 以比较并更新方式变更工单状态，并在同一事务中记录操作日志。 */
    public MesWorkOrder changeStatus(long workOrderId, String expectedStatus, String nextStatus,
            String operationType, Long operatorId, Long actorId, String remark) {
        return changeStatus(workOrderId, expectedStatus, nextStatus, operationType,
                operatorId, actorId, remark, true);
    }

    /**
     * 超级管理员接管派工单时可以跳过“必须是原被派工人”的限制；普通操作工仍严格校验归属。
     */
    public MesWorkOrder changeStatus(long workOrderId, String expectedStatus, String nextStatus,
            String operationType, Long operatorId, Long actorId, String remark,
            boolean requireAssignedOperator) {
        requireId(operatorId, "operatorId is required");
        requireId(actorId, "actorId is required");
        String timeColumn = "DISPATCHED".equals(nextStatus) ? "dispatch_time" : "receive_time";
        String actorColumn = "DISPATCHED".equals(nextStatus) ? "assigned_to" : "accepted_by";
        boolean checkOwnership = "RECEIVED".equals(nextStatus) && requireAssignedOperator;
        String ownershipCondition = checkOwnership ? "and assigned_to = ?" : "";
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
                if (checkOwnership) statement.setLong(5, operatorId);
                MesWorkOrder workOrder;
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        MesWorkOrder current = getWorkOrder(workOrderId);
                        if (!expectedStatus.equals(current.workOrderStatus)) {
                            throw new BadRequestException("工单当前状态为 " + current.workOrderStatus
                                    + "，只有 " + expectedStatus + " 状态才能执行该操作");
                        }
                        if (checkOwnership && !operatorId.equals(current.assignedTo)) {
                            throw new BadRequestException("该工单已派给其他操作工，当前用户不能接单");
                        }
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

    public boolean isEnabledProductionOperator(Long userId) {
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
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    private static void releaseTaskForConfirmedWorkOrder(Connection connection, Long taskId) throws SQLException {
        String sql = """
                update mes_production_task
                set task_status = 'RELEASED', release_time = current_timestamp, updated_at = current_timestamp
                where task_id = ? and task_status = 'READY' and kitting_status = 'READY'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            if (statement.executeUpdate() != 1) {
                throw new BadRequestException("production task status changed; please refresh and confirm again");
            }
        }
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

    private static MesWorkOrderOperationLog mapLogWithWorkOrder(ResultSet rs) throws SQLException {
        MesWorkOrderOperationLog item = mapLog(rs);
        item.workOrderNo = rs.getString("work_order_no");
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
