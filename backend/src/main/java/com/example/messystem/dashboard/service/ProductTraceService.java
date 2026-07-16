package com.example.messystem.dashboard.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.dashboard.dao.ProductTraceDao;
import com.example.messystem.dashboard.entity.MesProductTrace;

import java.sql.SQLException;
import java.time.LocalDateTime;
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

    /** 解析追溯标识，再由 DAO 查询完整的产品追溯链。 */
    public Map<String, Object> getProductTraceChain(String traceIdOrCode) throws SQLException {
        MesProductTrace trace = getProductTrace(traceIdOrCode)
                .orElseThrow(() -> new BadRequestException("产品追溯记录不存在"));
        return traceDao.findTraceChain(trace);
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

}
