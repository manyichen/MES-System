/*
 * 答辩定位：驾驶舱、反馈与产品追溯 模块的 ManagementFeedbackDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.dashboard.dao;

import com.example.messystem.common.Db;
import com.example.messystem.dashboard.entity.MesManagementFeedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 驾驶舱、反馈与产品追溯 的 ManagementFeedbackDao，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class ManagementFeedbackDao {

    /**
     * 数据访问：写入业务记录并返回主键。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public long insert(MesManagementFeedback feedback, long createdBy) throws SQLException {
        String sql = "INSERT INTO mes_management_feedback (feedback_no, feedback_type, related_doc_type, related_doc_id, feedback_content, decision_action, feedback_status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, feedback.feedbackNo());
            ps.setString(2, feedback.feedbackType());
            ps.setString(3, "WORK_ORDER");
            ps.setLong(4, feedback.workOrderId() == null ? 0L : feedback.workOrderId());
            ps.setString(5, feedback.feedbackContent());
            ps.setString(6, feedback.decisionAction() == null ? "" : feedback.decisionAction());
            ps.setString(7, feedback.feedbackStatus());
            ps.setLong(8, createdBy);
            ps.setObject(9, feedback.createdAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert management feedback failed, no ID obtained.");
            }
        }
    }

    /**
     * 数据访问：按主键查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public Optional<MesManagementFeedback> findById(long id) throws SQLException {
        String sql = "SELECT feedback_id, feedback_no, related_doc_id AS work_order_id, feedback_type, feedback_content, decision_action, feedback_status, created_at FROM mes_management_feedback WHERE feedback_id = ?";
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
    public List<MesManagementFeedback> findByWorkOrderId(long workOrderId) throws SQLException {
        String sql = "SELECT feedback_id, feedback_no, related_doc_id AS work_order_id, feedback_type, feedback_content, decision_action, feedback_status, created_at FROM mes_management_feedback WHERE related_doc_type = 'WORK_ORDER' AND related_doc_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, workOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MesManagementFeedback> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
                return results;
            }
        }
    }

    /**
     * 数据访问：按业务条件查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<MesManagementFeedback> findByWorkOrderIdAndCreator(long workOrderId, long createdBy) throws SQLException {
        String sql = "SELECT feedback_id, feedback_no, related_doc_id AS work_order_id, feedback_type, feedback_content, decision_action, feedback_status, created_at FROM mes_management_feedback WHERE related_doc_type = 'WORK_ORDER' AND related_doc_id = ? AND created_by = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, workOrderId);
            ps.setLong(2, createdBy);
            try (ResultSet rs = ps.executeQuery()) {
                List<MesManagementFeedback> results = new ArrayList<>();
                while (rs.next()) results.add(mapRow(rs));
                return results;
            }
        }
    }

    /**
     * 数据访问：按主键查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public Optional<MesManagementFeedback> findByIdAndCreator(long id, long createdBy) throws SQLException {
        String sql = "SELECT feedback_id, feedback_no, related_doc_id AS work_order_id, feedback_type, feedback_content, decision_action, feedback_status, created_at FROM mes_management_feedback WHERE feedback_id = ? AND created_by = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, createdBy);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /**
     * 数据访问：关闭业务事项。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public boolean close(long id) throws SQLException {
        String sql = "UPDATE mes_management_feedback SET feedback_status = 'CLOSED' WHERE feedback_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private MesManagementFeedback mapRow(ResultSet rs) throws SQLException {
        return new MesManagementFeedback(
                rs.getLong("feedback_id"),
                rs.getString("feedback_no"),
                null,
                null,
                rs.getLong("work_order_id"),
                rs.getString("feedback_type"),
                rs.getString("feedback_content"),
                rs.getString("decision_action"),
                rs.getString("feedback_status"),
                rs.getObject("created_at", java.time.LocalDateTime.class),
                null
        );
    }
}
