package com.example.messystem.dashboard.dao;

import com.example.messystem.common.Db;
import com.example.messystem.dashboard.entity.MesDashboardMetric;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DashboardMetricDao {

    public long insert(MesDashboardMetric metric) throws SQLException {
        String sql = "INSERT INTO mes_dashboard_metric (metric_key, metric_name, metric_value, metric_type, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, metric.metricKey());
            ps.setString(2, metric.metricName());
            ps.setString(3, metric.metricValue());
            ps.setString(4, metric.metricType());
            ps.setObject(5, metric.createdAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert dashboard metric failed, no ID obtained.");
            }
        }
    }

    public Optional<MesDashboardMetric> findById(long id) throws SQLException {
        String sql = "SELECT metric_id, metric_key, metric_name, metric_value, metric_type, created_at FROM mes_dashboard_metric WHERE metric_id = ?";
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

    public List<MesDashboardMetric> findAll() throws SQLException {
        String sql = "SELECT metric_id, metric_key, metric_name, metric_value, metric_type, created_at FROM mes_dashboard_metric";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<MesDashboardMetric> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        }
    }

    private MesDashboardMetric mapRow(ResultSet rs) throws SQLException {
        return new MesDashboardMetric(
                rs.getLong("metric_id"),
                rs.getString("metric_key"),
                rs.getString("metric_name"),
                rs.getString("metric_value"),
                rs.getString("metric_type"),
                rs.getObject("created_at", java.time.LocalDateTime.class)
        );
    }
}
