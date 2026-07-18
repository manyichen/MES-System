/*
 * 答辩定位：订单、计划、齐套与工单 模块的 ReworkPlanningDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.dao;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.planning.entity.ReworkPlanningDemand;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 持久化 PMC 返工重排查询及原子建任务用例。 */
public class ReworkPlanningDao {
    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<ReworkPlanningDemand> listDemands() throws SQLException {
        String sql = """
                select rw.rework_order_id, rw.rework_order_no, rw.source_work_order_id,
                       rw.rework_reason, rw.rework_status, rw.assigned_line_id, rw.created_at,
                       wo.planned_qty as source_planned_qty, source_task.order_id, o.order_no,
                       o.product_id, link.production_task_id as planned_task_id, planned_task.task_no as planned_task_no
                from mes_rework_order rw
                join mes_work_order wo on wo.work_order_id = rw.source_work_order_id
                join mes_production_task source_task on source_task.task_id = wo.task_id
                join mes_customer_order o on o.order_id = source_task.order_id
                left join mes_rework_plan_link link on link.rework_order_id = rw.rework_order_id
                left join mes_production_task planned_task on planned_task.task_id = link.production_task_id
                where rw.rework_status not in ('FINISHED', 'CLOSED')
                order by rw.created_at asc, rw.rework_order_id asc
                """;
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            List<ReworkPlanningDemand> rows = new ArrayList<>();
            while (rs.next()) {
                Timestamp createdAt = rs.getTimestamp("created_at");
                rows.add(new ReworkPlanningDemand(
                        rs.getLong("rework_order_id"), rs.getString("rework_order_no"),
                        rs.getLong("source_work_order_id"), rs.getString("rework_reason"),
                        rs.getString("rework_status"), nullableLong(rs, "assigned_line_id"),
                        rs.getInt("source_planned_qty"), rs.getLong("order_id"), rs.getString("order_no"),
                        rs.getLong("product_id"), nullableLong(rs, "planned_task_id"),
                        rs.getString("planned_task_no"), createdAt == null ? null : createdAt.toLocalDateTime()));
            }
            return rows;
        }
    }

    /** 锁定返工需求并在同一事务内创建任务、关联记录和状态变更。 */
    public long createTask(long reworkOrderId, long plannerId, Integer requestedQty,
            LocalDateTime start, LocalDateTime end, Long requestedLineId) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ReworkSource source = lockSource(connection, reworkOrderId);
                if (!"CREATED".equals(source.status())) {
                    throw new BadRequestException("只有待排产的返工需求才能生成生产任务");
                }
                ensureNotPlanned(connection, reworkOrderId);
                int qty = requestedQty == null || requestedQty <= 0 ? source.sourcePlannedQty() : requestedQty;
                Long lineId = requestedLineId == null ? source.assignedLineId() : requestedLineId;
                ensureAvailableLine(connection, lineId);
                long taskId = insertTask(connection, source, reworkOrderId, plannerId, qty, start, end, lineId);
                insertLink(connection, reworkOrderId, taskId, plannerId);
                markReworkPlanned(connection, reworkOrderId, lineId);
                updateOrderForRework(connection, source.orderId());
                connection.commit();
                return taskId;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * 数据访问：执行 lockSource 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static ReworkSource lockSource(Connection connection, long reworkOrderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select rw.rework_status, rw.assigned_line_id, rw.rework_order_no,
                       wo.planned_qty, source_task.order_id
                from mes_rework_order rw
                join mes_work_order wo on wo.work_order_id = rw.source_work_order_id
                join mes_production_task source_task on source_task.task_id = wo.task_id
                where rw.rework_order_id = ?
                for update
                """)) {
            statement.setLong(1, reworkOrderId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("返工需求不存在");
                return new ReworkSource(rs.getString("rework_status"), nullableLong(rs, "assigned_line_id"),
                        rs.getString("rework_order_no"), rs.getInt("planned_qty"), rs.getLong("order_id"));
            }
        }
    }

    /**
     * 数据访问：执行 ensureNotPlanned 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void ensureNotPlanned(Connection connection, long reworkOrderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select 1 from mes_rework_plan_link where rework_order_id = ?")) {
            statement.setLong(1, reworkOrderId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) throw new BadRequestException("该返工需求已经生成生产任务");
            }
        }
    }

    /**
     * 数据访问：执行 ensureAvailableLine 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void ensureAvailableLine(Connection connection, Long lineId) throws SQLException {
        if (lineId == null) return;
        try (PreparedStatement statement = connection.prepareStatement("""
                select 1 from mes_production_line
                where line_id = ? and enabled = 1 and line_status not in ('FAULT', 'DISABLED')
                """)) {
            statement.setLong(1, lineId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new BadRequestException("返工建议产线当前不可排产");
            }
        }
    }

    /**
     * 数据访问：写入业务记录并返回主键。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static long insertTask(Connection connection, ReworkSource source, long reworkOrderId,
            long plannerId, int qty, LocalDateTime start, LocalDateTime end, Long lineId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into mes_production_task
                    (task_no, order_id, planner_id, plan_qty, planned_start_time, planned_end_time,
                     target_line_id, task_status, kitting_status, remark)
                values (?, ?, ?, ?, ?, ?, ?, 'CREATED', 'PENDING', ?)
                returning task_id
                """)) {
            statement.setString(1, IdGenerator.nextCode("REWORK-TASK"));
            statement.setLong(2, source.orderId());
            statement.setLong(3, plannerId);
            statement.setInt(4, qty);
            statement.setTimestamp(5, Timestamp.valueOf(start));
            statement.setTimestamp(6, Timestamp.valueOf(end));
            if (lineId == null) statement.setNull(7, java.sql.Types.BIGINT); else statement.setLong(7, lineId);
            statement.setString(8, "来源返工单 " + source.reworkOrderNo() + "（ID " + reworkOrderId + "）");
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /**
     * 数据访问：写入业务记录并返回主键。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void insertLink(Connection connection, long reworkOrderId, long taskId, long plannerId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into mes_rework_plan_link (rework_order_id, production_task_id, planned_by) values (?, ?, ?)")) {
            statement.setLong(1, reworkOrderId);
            statement.setLong(2, taskId);
            statement.setLong(3, plannerId);
            statement.executeUpdate();
        }
    }

    /**
     * 数据访问：执行 markReworkPlanned 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void markReworkPlanned(Connection connection, long reworkOrderId, Long lineId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "update mes_rework_order set rework_status = 'PLANNED', assigned_line_id = coalesce(?, assigned_line_id) where rework_order_id = ?")) {
            if (lineId == null) statement.setNull(1, java.sql.Types.BIGINT); else statement.setLong(1, lineId);
            statement.setLong(2, reworkOrderId);
            statement.executeUpdate();
        }
    }

    /**
     * 数据访问：更新业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void updateOrderForRework(Connection connection, long orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "update mes_customer_order set order_status = 'PLANNED', updated_at = current_timestamp where order_id = ?")) {
            statement.setLong(1, orderId);
            statement.executeUpdate();
        }
    }

    /**
     * 数据访问：执行 nullableLong 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * 数据访问：执行 ReworkSource 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private record ReworkSource(String status, Long assignedLineId, String reworkOrderNo,
            int sourcePlannedQty, long orderId) {
    }
}
