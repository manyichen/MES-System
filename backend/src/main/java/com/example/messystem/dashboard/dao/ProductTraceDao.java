package com.example.messystem.dashboard.dao;

import com.example.messystem.common.Db;
import com.example.messystem.dashboard.entity.MesProductTrace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProductTraceDao {

    private static final String COLUMNS = "product_trace_id, trace_code, order_id, task_id, work_order_id, "
            + "product_id, batch_no, trace_status, created_at";

    public long insert(MesProductTrace trace) throws SQLException {
        String sql = "INSERT INTO mes_product_trace (trace_code, order_id, task_id, work_order_id, product_id, batch_no, trace_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, trace.traceCode());
            setLong(ps, 2, trace.orderId());
            setLong(ps, 3, trace.taskId());
            setLong(ps, 4, trace.workOrderId());
            setLong(ps, 5, trace.productId());
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
        String sql = "SELECT " + COLUMNS + " FROM mes_product_trace WHERE product_trace_id = ?";
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
        String sql = "SELECT " + COLUMNS + " FROM mes_product_trace WHERE trace_code = ?";
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
        String sql = "SELECT " + COLUMNS + " FROM mes_product_trace WHERE work_order_id = ? "
                + "ORDER BY created_at DESC, product_trace_id DESC";
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
        String sql = "SELECT " + COLUMNS + " FROM mes_product_trace "
                + "ORDER BY created_at DESC, product_trace_id DESC";
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<MesProductTrace> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        }
    }

    public Optional<TraceContext> findContext(long orderId, long taskId, long workOrderId) throws SQLException {
        String sql = """
                select co.product_id as order_product_id, wo.product_id as work_order_product_id, wo.batch_no
                from mes_customer_order co
                join mes_production_task pt on pt.order_id = co.order_id
                join mes_work_order wo on wo.task_id = pt.task_id
                where co.order_id = ? and pt.task_id = ? and wo.work_order_id = ?
                """;
        try (Connection connection = Db.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setLong(2, taskId);
            statement.setLong(3, workOrderId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Long workOrderProductId = getLong(rs, "work_order_product_id");
                return Optional.of(new TraceContext(
                        workOrderProductId == null ? getLong(rs, "order_product_id") : workOrderProductId,
                        rs.getString("batch_no")));
            }
        }
    }

    /** 查询产品追溯链接口展示的全部上下游记录。 */
    public Map<String, Object> findTraceChain(MesProductTrace trace) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            Map<String, Object> chain = new LinkedHashMap<>();
            chain.put("trace", trace);
            chain.put("order", findOne(connection,
                    "select * from mes_customer_order where order_id = ?", trace.orderId()));
            chain.put("task", findOne(connection,
                    "select * from mes_production_task where task_id = ?", trace.taskId()));
            chain.put("workOrder", findOne(connection,
                    "select * from mes_work_order where work_order_id = ?", trace.workOrderId()));
            chain.put("product", findOne(connection,
                    "select * from mes_product where product_id = ?", trace.productId()));
            chain.put("workReports", findMany(connection,
                    "select * from mes_work_report where work_order_id = ? order by report_id asc", trace.workOrderId()));
            chain.put("qualityInspections", findMany(connection,
                    "select * from mes_quality_inspection where work_order_id = ? order by inspection_id asc", trace.workOrderId()));
            chain.put("reworkOrders", findMany(connection,
                    "select * from mes_rework_order where source_work_order_id = ? order by rework_order_id asc",
                    trace.workOrderId()));
            chain.put("qualityTraces", findMany(connection,
                    "select * from mes_quality_trace where work_order_id = ? order by trace_id asc", trace.workOrderId()));
            return chain;
        }
    }

    private Map<String, Object> findOne(Connection connection, String sql, Long id) throws SQLException {
        List<Map<String, Object>> rows = findMany(connection, sql, id);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private List<Map<String, Object>> findMany(Connection connection, String sql, Long id) throws SQLException {
        if (id == null) return List.of();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
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
                getLong(rs, "product_trace_id"),
                rs.getString("trace_code"),
                getLong(rs, "order_id"),
                getLong(rs, "task_id"),
                getLong(rs, "work_order_id"),
                getLong(rs, "product_id"),
                rs.getString("batch_no"),
                rs.getString("trace_status"),
                rs.getObject("created_at", java.time.LocalDateTime.class)
        );
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static void setLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) statement.setNull(index, Types.BIGINT);
        else statement.setLong(index, value);
    }

    public record TraceContext(Long productId, String batchNo) { }
}
