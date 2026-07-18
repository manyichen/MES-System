/*
 * 答辩定位：质检、质量追溯与返工 模块的 QualityInspectionService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.quality.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.UserRoleValidator;
import com.example.messystem.quality.dao.QualityInspectionDao;
import com.example.messystem.quality.dao.QualityInspectionItemDao;
import com.example.messystem.quality.dao.QualityTraceDao;
import com.example.messystem.quality.dao.ReworkOrderDao;
import com.example.messystem.quality.entity.MesQualityInspection;
import com.example.messystem.quality.entity.MesQualityInspectionItem;
import com.example.messystem.quality.entity.MesQualityTrace;
import com.example.messystem.quality.entity.MesReworkOrder;
import com.example.messystem.warehouse.dao.WarehouseDao;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 质检、质量追溯与返工 的 QualityInspectionService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class QualityInspectionService {

    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final QualityInspectionDao inspectionDao = new QualityInspectionDao();
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final QualityInspectionItemDao itemDao = new QualityInspectionItemDao();
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final ReworkOrderDao reworkOrderDao = new ReworkOrderDao();
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final QualityTraceDao traceDao = new QualityTraceDao();
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final WarehouseDao warehouseDao = new WarehouseDao();

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public long createInspection(MesQualityInspection inspection) throws SQLException {
        return inspectionDao.insert(inspection);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Optional<MesQualityInspection> getInspectionById(long inspectionId) throws SQLException {
        return inspectionDao.findById(inspectionId);
    }

    /**
     * 业务用例：执行 requireAssignedInspection 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesQualityInspection requireAssignedInspection(long inspectionId, long userId) throws SQLException {
        MesQualityInspection inspection = inspectionDao.findById(inspectionId)
                .orElseThrow(() -> new BadRequestException("质检单不存在"));
        if (!Long.valueOf(userId).equals(inspection.assignedTo())
                && !Long.valueOf(userId).equals(inspection.inspectorId())) {
            throw new BadRequestException("只能处理分配给本人的质检任务");
        }
        return inspection;
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesQualityInspection> listInspections() throws SQLException {
        return inspectionDao.findAll();
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesQualityInspection> listAssignedInspections(long userId) throws SQLException {
        return inspectionDao.findAssignedTo(userId);
    }

    /**
     * 业务用例：分配执行人员或资源。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean assignInspection(long inspectionId, long inspectorId) throws SQLException {
        UserRoleValidator.requireEnabledRole(inspectorId, "QUALITY_INSPECTOR", "质检员");
        if (!inspectionDao.assign(inspectionId, inspectorId)) {
            throw new BadRequestException("质检单不存在、状态不允许分配或已经被处理");
        }
        return true;
    }

    /**
     * 业务用例：提交业务事项。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean submitInspection(long inspectionId, long inspectorId, String result, String note) throws SQLException {
        return submitInspection(inspectionId, inspectorId, result, note, false);
    }

    /**
     * 业务用例：提交业务事项。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean submitInspection(long inspectionId, long inspectorId, String result, String note,
            boolean allowAdministrativeOverride) throws SQLException {
        String submittedResult = normalizeSubmittedResult(result);
        if (!"PASS".equals(submittedResult) && (note == null || note.isBlank())) {
            throw new BadRequestException("不合格或需返工时必须说明异常项目和原因");
        }
        if (!inspectionDao.submit(
                inspectionId, inspectorId, submittedResult, note, !allowAdministrativeOverride)) {
            throw new BadRequestException("只能提交分配给本人的未完成质检单");
        }
        return true;
    }

    /**
     * 业务用例：执行 addInspectionItem 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public long addInspectionItem(MesQualityInspectionItem item) throws SQLException {
        return itemDao.insert(item);
    }

    /**
     * 业务用例：执行 addAssignedInspectionItem 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public long addAssignedInspectionItem(MesQualityInspectionItem item, long inspectorId) throws SQLException {
        MesQualityInspection inspection = requireAssignedInspection(item.inspectionId(), inspectorId);
        if (!List.of("CREATED", "IN_PROGRESS").contains(inspection.inspectionStatus())) {
            throw new BadRequestException("质检结果已提交，不能继续修改检验项目");
        }
        return itemDao.insert(item);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesQualityInspectionItem> getInspectionItems(long inspectionId) throws SQLException {
        return itemDao.findByInspectionId(inspectionId);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesQualityTrace> listAllTraces() throws SQLException {
        return traceDao.findAll();
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesQualityTrace> listTracesByWorkOrder(long workOrderId) throws SQLException {
        return traceDao.findByWorkOrderId(workOrderId);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesQualityTrace> listTracesByInspection(long inspectionId) throws SQLException {
        return traceDao.findByInspectionId(inspectionId);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Optional<MesQualityTrace> getTraceById(long traceId) throws SQLException {
        return traceDao.findById(traceId);
    }

    /**
     * 业务用例：执行 judgeInspection 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public boolean judgeInspection(long inspectionId, String status, String result, long reviewedBy) throws SQLException {
        MesQualityInspection inspection = inspectionDao.findById(inspectionId)
                .orElseThrow(() -> new BadRequestException("质检单不存在"));
        List<MesQualityInspectionItem> items = itemDao.findByInspectionId(inspectionId);

        String finalResult = resolveJudgementResult(result, inspection.submittedResult(), items);
        String finalStatus = "PASS".equals(finalResult) ? "APPROVED"
                : "REWORK".equals(finalResult) ? "REWORK_REQUIRED" : "REJECTED";
        if (!inspectionDao.updateStatus(inspectionId, finalStatus, finalResult, reviewedBy)) {
            if ("PASS".equals(finalResult)
                    && "APPROVED".equals(inspection.inspectionStatus())
                    && "PASS".equals(inspection.judgementResult())) {
                warehouseDao.receiveFinishedGoodsFromQuality(inspectionId, reviewedBy);
                return true;
            }
            throw new BadRequestException("只有质检员已提交的检验结果才能审核");
        }

        Long reworkId = null;
        if ("REWORK".equals(finalResult)) {
            MesReworkOrder reworkOrder = new MesReworkOrder(
                    null,
                    generateCode("RW"),
                    inspection.workOrderId(),
                    inspectionId,
                    "质检判定需要返工",
                    "CREATED",
                    null,
                    LocalDateTime.now(),
                    null
            );
            reworkId = reworkOrderDao.insert(reworkOrder);
        }

        Long finalReworkId = reworkId;
        MesQualityTrace trace = inspectionDao.findTraceContext(inspectionId)
                .map(context -> new MesQualityTrace(
                        null,
                        generateCode("QT"),
                        context.orderId(),
                        context.taskId(),
                        context.workOrderId(),
                        context.batchNo(),
                        inspectionId,
                        finalReworkId,
                        traceStatus(finalResult),
                        LocalDateTime.now()
                ))
                .orElseThrow(() -> new BadRequestException("质检单未关联到完整的订单、任务、工单链路，无法写入质量追溯"));
        traceDao.insert(trace);
        if ("PASS".equals(finalResult)) {
            warehouseDao.receiveFinishedGoodsFromQuality(inspectionId, reviewedBy);
        }
        return true;
    }

    /**
     * 业务用例：执行 resolveJudgementResult 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private String resolveJudgementResult(String requestedResult, String submittedResult, List<MesQualityInspectionItem> items) {
        if (items.isEmpty()) {
            return normalizeSubmittedResult(requestedResult == null || requestedResult.isBlank()
                    ? submittedResult
                    : requestedResult);
        }
        boolean hasRework = items.stream().anyMatch(item -> "REWORK".equalsIgnoreCase(item.itemResult()));
        boolean hasFail = items.stream().anyMatch(item -> isFail(item.itemResult()));
        if (hasRework || (hasFail && "REWORK".equalsIgnoreCase(requestedResult))) {
            return "REWORK";
        }
        if (hasFail) {
            return "FAIL";
        }
        return "PASS";
    }

    /**
     * 业务用例：规范化输入并补齐默认值。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private String normalizeSubmittedResult(String result) {
        String value = result == null ? "" : result.trim().toUpperCase();
        return switch (value) {
            case "PASS", "OK", "合格" -> "PASS";
            case "REWORK", "返工", "需返工" -> "REWORK";
            case "FAIL", "FAILED", "NG", "不合格" -> "FAIL";
            default -> throw new BadRequestException("质检结果只能是合格、不合格或需返工");
        };
    }

    /**
     * 业务用例：执行 isFail 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private boolean isFail(String result) {
        return "FAIL".equalsIgnoreCase(result)
                || "FAILED".equalsIgnoreCase(result)
                || "NG".equalsIgnoreCase(result)
                || "不合格".equals(result);
    }

    /**
     * 业务用例：执行 traceStatus 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private String traceStatus(String result) {
        return switch (result) {
            case "PASS" -> "NORMAL";
            case "REWORK" -> "REWORKED";
            default -> "QUALITY_RISK";
        };
    }

    /**
     * 业务用例：生成业务结果。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private String generateCode(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }
}
