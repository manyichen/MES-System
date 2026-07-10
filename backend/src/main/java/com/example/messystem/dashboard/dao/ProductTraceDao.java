package com.example.messystem.dashboard.dao;

import com.example.messystem.common.Db;
import com.example.messystem.dashboard.entity.MesProductTrace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductTraceDao {

    public long insert(MesProductTrace trace) throws SQLException {
        String sql = "INSERT INTO mes_product_trace (trace_code, order_id, task_id, work_order_id, product_id, batch_no, trace_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, trace.traceCode());
            ps.setLong(2, trace.orderId());
            ps.setLong(3, trace.taskId());
            ps.setLong(4, trace.workOrderId());
            ps.setLong(5, trace.productId() == null ? 1L : trace.productId());
            ps.setString(6, trace.batchNo());
            ps.setString(7, trace.traceStatus());
            ps.setObject(8, trace.createdAt());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                throw new SQLException("Insert product trace failed, no ID obtained.");
            }
        }
    }

    public Optional<MesProductTrace> findById(long id) throws SQLException {
        String sql = "SELECT product_trace_id, trace_code, order_id, task_id, work_order_id, product_id, batch_no, trace_status, created_at FROM mes_product_trace WHERE product_trace_id = ?";
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

    public Optional<MesProductTrace> findByTraceCode(String traceCode) throws SQLException {
        String sql = "SELECT product_trace_id, trace_code, order_id, task_id, work_order_id, product_id, batch_no, trace_status, created_at FROM mes_product_trace WHERE trace_code = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, traceCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<MesProductTrace> findByWorkOrderId(long workOrderId) throws SQLException {
        String sql = "SELECT product_trace_id, trace_code, order_id, task_id, work_order_id, product_id, batch_no, trace_status, created_at FROM mes_product_trace WHERE work_order_id = ?";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, workOrderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MesProductTrace> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
                return results;
            }
        }
    }

    public List<MesProductTrace> findAll() throws SQLException {
        String sql = "SELECT product_trace_id, trace_code, order_id, task_id, work_order_id, product_id, batch_no, trace_status, created_at FROM mes_product_trace";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<MesProductTrace> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        }
    }

    private MesProductTrace mapRow(ResultSet rs) throws SQLException {
        return new MesProductTrace(
                rs.getLong("product_trace_id"),
                rs.getString("trace_code"),
                rs.getLong("order_id"),
                rs.getLong("task_id"),
                rs.getLong("work_order_id"),
                rs.getLong("product_id"),
                rs.getString("batch_no"),
                rs.getString("trace_status"),
                rs.getObject("created_at", java.time.LocalDateTime.class)
        );
    }
}
