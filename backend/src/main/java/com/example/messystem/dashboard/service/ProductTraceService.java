/*
 * 答辩定位：驾驶舱、反馈与产品追溯 模块的 ProductTraceService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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

/**
 * 驾驶舱、反馈与产品追溯 的 ProductTraceService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class ProductTraceService {

    private static final Set<String> TRACE_STATUSES = Set.of("NORMAL", "QUALITY_RISK", "REWORKED");
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final ProductTraceDao traceDao;

    /**
     * 业务用例：执行 ProductTraceService 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public ProductTraceService() {
        this(new ProductTraceDao());
    }

    ProductTraceService(ProductTraceDao traceDao) {
        this.traceDao = traceDao;
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public long createProductTrace(MesProductTrace trace) throws SQLException {
        return traceDao.insert(normalize(trace));
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesProductTrace> listProductTraces() throws SQLException {
        return traceDao.findAll();
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesProductTrace> listTracesByWorkOrder(long workOrderId) throws SQLException {
        return traceDao.findByWorkOrderId(workOrderId);
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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

    /**
     * 业务用例：规范化输入并补齐默认值。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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

    /**
     * 业务用例：执行 requirePositive 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) throw new BadRequestException(fieldName + "不能为空");
    }

    /**
     * 业务用例：执行 firstNonBlank 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

}
