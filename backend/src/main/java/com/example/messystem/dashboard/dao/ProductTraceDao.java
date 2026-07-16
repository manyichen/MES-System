package com.example.messystem.dashboard.dao;

import com.example.messystem.common.Db;
import com.example.messystem.dashboard.entity.MesProductTrace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /** 查询产品追溯链接口展示的全部上下游记录。 */
    public Map<String, Object> findTraceChain(MesProductTrace trace) throws SQLException {
        Map<String, Object> chain = new LinkedHashMap<>();
        chain.put("trace", trace);
        chain.put("order", findOne("select * from mes_customer_order where order_id = ?", trace.orderId()));
        chain.put("task", findOne("select * from mes_production_task where task_id = ?", trace.taskId()));
        chain.put("workOrder", findOne("select * from mes_work_order where work_order_id = ?", trace.workOrderId()));
        chain.put("workReports", findMany(
                "select * from mes_work_report where work_order_id = ? order by report_id asc", trace.workOrderId()));
        chain.put("qualityInspections", findMany(
                "select * from mes_quality_inspection where work_order_id = ? order by inspection_id asc", trace.workOrderId()));
        chain.put("reworkOrders", findMany("""
                select rw.*
                from mes_rework_order rw
                join mes_quality_inspection qi on qi.inspection_id = rw.inspection_id
                where qi.work_order_id = ?
                order by rw.rework_order_id asc
                """, trace.workOrderId()));
        chain.put("qualityTraces", findMany(
                "select * from mes_quality_trace where work_order_id = ? order by trace_id asc", trace.workOrderId()));
        return chain;
    }

    private Map<String, Object> findOne(String sql, Long id) throws SQLException {
        List<Map<String, Object>> rows = findMany(sql, id);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private List<Map<String, Object>> findMany(String sql, Long id) throws SQLException {
        if (id == null) return List.of();
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int index = 1; index <= meta.getColumnCount(); index++) {
                        row.put(meta.getColumnLabel(index), rs.getObject(index));
                    }
                    rows.add(row);
                }
                return rows;
            }
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
