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

public class ManagementFeedbackDao {

    public long insert(MesManagementFeedback feedback) throws SQLException {
        String sql = "INSERT INTO mes_management_feedback (feedback_no, order_id, task_id, work_order_id, feedback_type, feedback_content, feedback_status, created_at, closed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, feedback.feedbackNo());
            ps.setLong(2, feedback.orderId());
            ps.setLong(3, feedback.taskId());
            ps.setLong(4, feedback.workOrderId());
            ps.setString(5, feedback.feedbackType());
            ps.setString(6, feedback.feedbackContent());
            ps.setString(7, feedback.feedbackStatus());
            ps.setObject(8, feedback.createdAt());
            ps.setObject(9, feedback.closedAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert management feedback failed, no ID obtained.");
            }
        }
    }

    public Optional<MesManagementFeedback> findById(long id) throws SQLException {
        String sql = "SELECT feedback_id, feedback_no, order_id, task_id, work_order_id, feedback_type, feedback_content, feedback_status, created_at, closed_at FROM mes_management_feedback WHERE feedback_id = ?";
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

    public List<MesManagementFeedback> findByWorkOrderId(long workOrderId) throws SQLException {
        String sql = "SELECT feedback_id, feedback_no, order_id, task_id, work_order_id, feedback_type, feedback_content, feedback_status, created_at, closed_at FROM mes_management_feedback WHERE work_order_id = ?";
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

    public boolean close(long id) throws SQLException {
        String sql = "UPDATE mes_management_feedback SET feedback_status = 'CLOSED', closed_at = NOW() WHERE feedback_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private MesManagementFeedback mapRow(ResultSet rs) throws SQLException {
        return new MesManagementFeedback(
                rs.getLong("feedback_id"),
                rs.getString("feedback_no"),
                rs.getLong("order_id"),
                rs.getLong("task_id"),
                rs.getLong("work_order_id"),
                rs.getString("feedback_type"),
                rs.getString("feedback_content"),
                rs.getString("feedback_status"),
                rs.getObject("created_at", java.time.LocalDateTime.class),
                rs.getObject("closed_at", java.time.LocalDateTime.class)
        );
    }
}
