package com.example.messystem.dashboard.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.dashboard.dao.ProductTraceDao;
import com.example.messystem.dashboard.entity.MesProductTrace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProductTraceService {

    private final ProductTraceDao traceDao = new ProductTraceDao();

    public long createProductTrace(MesProductTrace trace) throws SQLException {
        return traceDao.insert(trace);
    }

    public Optional<MesProductTrace> getProductTrace(String traceIdOrCode) throws SQLException {
        try {
            return traceDao.findById(Long.parseLong(traceIdOrCode));
        } catch (NumberFormatException e) {
            return traceDao.findByTraceCode(traceIdOrCode);
        }
    }

    public Map<String, Object> getProductTraceChain(String traceIdOrCode) throws SQLException {
        MesProductTrace trace = getProductTrace(traceIdOrCode)
                .orElseThrow(() -> new BadRequestException("产品追溯记录不存在"));
        Map<String, Object> chain = new LinkedHashMap<>();
        chain.put("trace", trace);
        chain.put("order", findOne("select * from mes_customer_order where order_id = ?", trace.orderId()));
        chain.put("task", findOne("select * from mes_production_task where task_id = ?", trace.taskId()));
        chain.put("workOrder", findOne("select * from mes_work_order where work_order_id = ?", trace.workOrderId()));
        chain.put("workReports", findMany("select * from mes_work_report where work_order_id = ? order by report_id asc", trace.workOrderId()));
        chain.put("qualityInspections", findMany("select * from mes_quality_inspection where work_order_id = ? order by inspection_id asc", trace.workOrderId()));
        chain.put("reworkOrders", findMany("""
                select rw.*
                from mes_rework_order rw
                join mes_quality_inspection qi on qi.inspection_id = rw.inspection_id
                where qi.work_order_id = ?
                order by rw.rework_order_id asc
                """, trace.workOrderId()));
        chain.put("qualityTraces", findMany("select * from mes_quality_trace where work_order_id = ? order by trace_id asc", trace.workOrderId()));
        return chain;
    }

    public List<MesProductTrace> listProductTraces() throws SQLException {
        return traceDao.findAll();
    }

    public List<MesProductTrace> listTracesByWorkOrder(long workOrderId) throws SQLException {
        return traceDao.findByWorkOrderId(workOrderId);
    }

    public long createDefaultProductTrace(long orderId, long taskId, long workOrderId, String batchNo, String status) throws SQLException {
        MesProductTrace trace = new MesProductTrace(
                null,
                "TRACE-" + System.currentTimeMillis(),
                orderId,
                taskId,
                workOrderId,
                null,
                batchNo,
                status,
                LocalDateTime.now()
        );
        return traceDao.insert(trace);
    }

    private Map<String, Object> findOne(String sql, Long id) throws SQLException {
        List<Map<String, Object>> rows = findMany(sql, id);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private List<Map<String, Object>> findMany(String sql, Long id) throws SQLException {
        if (id == null) {
            return List.of();
        }
        try (Connection conn = Db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }
}
