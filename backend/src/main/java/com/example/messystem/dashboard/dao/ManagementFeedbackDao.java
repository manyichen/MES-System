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
        String sql = "INSERT INTO mes_management_feedback (feedback_no, feedback_type, related_doc_type, related_doc_id, feedback_content, decision_action, feedback_status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, feedback.feedbackNo());
            ps.setString(2, feedback.feedbackType());
            ps.setString(3, "WORK_ORDER");
            ps.setLong(4, feedback.workOrderId() == null ? 0L : feedback.workOrderId());
            ps.setString(5, feedback.feedbackContent());
            ps.setString(6, "");
            ps.setString(7, feedback.feedbackStatus());
            ps.setLong(8, 1L);
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

    public Optional<MesManagementFeedback> findById(long id) throws SQLException {
        String sql = "SELECT feedback_id, feedback_no, related_doc_id AS work_order_id, feedback_type, feedback_content, feedback_status, created_at FROM mes_management_feedback WHERE feedback_id = ?";
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
        String sql = "SELECT feedback_id, feedback_no, related_doc_id AS work_order_id, feedback_type, feedback_content, feedback_status, created_at FROM mes_management_feedback WHERE related_doc_type = 'WORK_ORDER' AND related_doc_id = ?";
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
        String sql = "UPDATE mes_management_feedback SET feedback_status = 'CLOSED' WHERE feedback_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private MesManagementFeedback mapRow(ResultSet rs) throws SQLException {
        return new MesManagementFeedback(
                rs.getLong("feedback_id"),
                rs.getString("feedback_no"),
                null,
                null,
                rs.getLong("work_order_id"),
                rs.getString("feedback_type"),
                rs.getString("feedback_content"),
                rs.getString("feedback_status"),
                rs.getObject("created_at", java.time.LocalDateTime.class),
                null
        );
    }
}
