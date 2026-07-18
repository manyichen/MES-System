/*
 * 答辩定位：质检、质量追溯与返工 模块的 QualityTraceDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.quality.dao;

import com.example.messystem.common.Db;
import com.example.messystem.quality.entity.MesQualityTrace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 质检、质量追溯与返工 的 QualityTraceDao，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class QualityTraceDao {

    /**
     * 数据访问：写入业务记录并返回主键。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public long insert(MesQualityTrace trace) throws SQLException {
        String sql = "INSERT INTO mes_quality_trace (trace_no, order_id, task_id, work_order_id, batch_no, inspection_id, rework_order_id, trace_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, trace.traceNo());
            ps.setLong(2, trace.orderId());
            ps.setLong(3, trace.taskId());
            ps.setLong(4, trace.workOrderId());
            ps.setString(5, trace.batchNo());
            if (trace.inspectionId() == null) {
                ps.setNull(6, java.sql.Types.BIGINT);
            } else {
                ps.setLong(6, trace.inspectionId());
            }
            if (trace.reworkOrderId() == null) {
                ps.setNull(7, java.sql.Types.BIGINT);
            } else {
                ps.setLong(7, trace.reworkOrderId());
            }
            ps.setString(8, trace.traceStatus());
            ps.setObject(9, trace.createdAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert quality trace failed, no ID obtained.");
            }
        }
    }

    /**
     * 数据访问：按主键查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public Optional<MesQualityTrace> findById(long id) throws SQLException {
        String sql = "SELECT trace_id, trace_no, order_id, task_id, work_order_id, batch_no, inspection_id, rework_order_id, trace_status, created_at FROM mes_quality_trace WHERE trace_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 数据访问：按业务条件查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesQualityTrace> findByWorkOrderId(long workOrderId) throws SQLException {
        String sql = "SELECT trace_id, trace_no, order_id, task_id, work_order_id, batch_no, inspection_id, rework_order_id, trace_status, created_at FROM mes_quality_trace WHERE work_order_id = ? ORDER BY created_at DESC, trace_id DESC";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, workOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MesQualityTrace> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    /**
     * 数据访问：按业务条件查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesQualityTrace> findByInspectionId(long inspectionId) throws SQLException {
        String sql = "SELECT trace_id, trace_no, order_id, task_id, work_order_id, batch_no, inspection_id, rework_order_id, trace_status, created_at FROM mes_quality_trace WHERE inspection_id = ? ORDER BY created_at DESC, trace_id DESC";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, inspectionId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MesQualityTrace> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
                return results;
            }
        }
    }

    /**
     * 数据访问：查询全部可见记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesQualityTrace> findAll() throws SQLException {
        String sql = "SELECT trace_id, trace_no, order_id, task_id, work_order_id, batch_no, inspection_id, rework_order_id, trace_status, created_at FROM mes_quality_trace ORDER BY created_at DESC, trace_id DESC";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<MesQualityTrace> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private MesQualityTrace mapRow(ResultSet rs) throws SQLException {
        return new MesQualityTrace(
                rs.getLong("trace_id"),
                rs.getString("trace_no"),
                rs.getLong("order_id"),
                rs.getLong("task_id"),
                rs.getLong("work_order_id"),
                rs.getString("batch_no"),
                rs.getObject("inspection_id") == null ? null : rs.getLong("inspection_id"),
                rs.getObject("rework_order_id") == null ? null : rs.getLong("rework_order_id"),
                rs.getString("trace_status"),
                rs.getObject("created_at", java.time.LocalDateTime.class)
        );
    }
}
