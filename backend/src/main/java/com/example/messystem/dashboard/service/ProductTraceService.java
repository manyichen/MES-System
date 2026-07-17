package com.example.messystem.dashboard.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.dashboard.dao.ProductTraceDao;
import com.example.messystem.dashboard.dao.ProductTraceDao.TraceContext;
import com.example.messystem.dashboard.entity.MesProductTrace;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ProductTraceService {

    private static final Set<String> TRACE_STATUSES = Set.of("NORMAL", "QUALITY_RISK", "REWORKED");
    private final ProductTraceDao traceDao;

    public ProductTraceService() {
        this(new ProductTraceDao());
    }

    ProductTraceService(ProductTraceDao traceDao) {
        this.traceDao = traceDao;
    }

    public long createProductTrace(MesProductTrace trace) throws SQLException {
        return traceDao.insert(normalize(trace));
    }

    public Optional<MesProductTrace> getProductTrace(String traceIdOrCode) throws SQLException {
        if (traceIdOrCode == null || traceIdOrCode.isBlank()) {
            throw new BadRequestException("产品追溯标识不能为空");
        }
        String normalized = traceIdOrCode.trim();
        try {
            long traceId = Long.parseLong(normalized);
            if (traceId <= 0) throw new BadRequestException("产品追溯ID必须大于0");
            return traceDao.findById(traceId);
        } catch (NumberFormatException e) {
            return traceDao.findByTraceCode(normalized);
        }
    }

    /** 解析追溯标识，再由 DAO 查询完整的产品追溯链。 */
    public Map<String, Object> getProductTraceChain(String traceIdOrCode) throws SQLException {
        MesProductTrace trace = getProductTrace(traceIdOrCode)
                .orElseThrow(() -> new NotFoundException("产品追溯记录不存在"));
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
        return createProductTrace(trace);
    }

    private MesProductTrace normalize(MesProductTrace trace) throws SQLException {
        if (trace == null) throw new BadRequestException("产品追溯信息不能为空");
        requirePositive(trace.orderId(), "客户订单ID");
        requirePositive(trace.taskId(), "生产任务ID");
        requirePositive(trace.workOrderId(), "制造工单ID");

        TraceContext context = traceDao.findContext(trace.orderId(), trace.taskId(), trace.workOrderId())
                .orElseThrow(() -> new BadRequestException("订单、生产任务与制造工单的关联关系不正确"));
        if (context.productId() == null || context.productId() <= 0) {
            throw new BadRequestException("制造工单未关联产品，无法创建产品追溯记录");
        }
        if (trace.productId() != null && !trace.productId().equals(context.productId())) {
            throw new BadRequestException("产品与制造工单关联的产品不一致");
        }

        String batchNo = firstNonBlank(trace.batchNo(), context.batchNo());
        if (batchNo == null) throw new BadRequestException("生产批次不能为空");
        String status = firstNonBlank(trace.traceStatus(), "NORMAL").toUpperCase();
        if (!TRACE_STATUSES.contains(status)) throw new BadRequestException("产品追溯状态不合法");
        String traceCode = firstNonBlank(trace.traceCode(), "TRACE-" + UUID.randomUUID());

        return new MesProductTrace(
                null, traceCode, trace.orderId(), trace.taskId(), trace.workOrderId(), context.productId(),
                batchNo, status, trace.createdAt() == null ? LocalDateTime.now() : trace.createdAt());
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) throw new BadRequestException(fieldName + "不能为空");
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

}
