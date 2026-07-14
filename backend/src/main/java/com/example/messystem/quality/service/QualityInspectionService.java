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

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class QualityInspectionService {

    private final QualityInspectionDao inspectionDao = new QualityInspectionDao();
    private final QualityInspectionItemDao itemDao = new QualityInspectionItemDao();
    private final ReworkOrderDao reworkOrderDao = new ReworkOrderDao();
    private final QualityTraceDao traceDao = new QualityTraceDao();

    public long createInspection(MesQualityInspection inspection) throws SQLException {
        return inspectionDao.insert(inspection);
    }

    public Optional<MesQualityInspection> getInspectionById(long inspectionId) throws SQLException {
        return inspectionDao.findById(inspectionId);
    }

    public MesQualityInspection requireAssignedInspection(long inspectionId, long userId) throws SQLException {
        MesQualityInspection inspection = inspectionDao.findById(inspectionId)
                .orElseThrow(() -> new BadRequestException("质检单不存在"));
        if (!Long.valueOf(userId).equals(inspection.assignedTo())
                && !Long.valueOf(userId).equals(inspection.inspectorId())) {
            throw new BadRequestException("只能处理分配给本人的质检任务");
        }
        return inspection;
    }

    public List<MesQualityInspection> listInspections() throws SQLException {
        return inspectionDao.findAll();
    }

    public List<MesQualityInspection> listAssignedInspections(long userId) throws SQLException {
        return inspectionDao.findAssignedTo(userId);
    }

    public boolean assignInspection(long inspectionId, long inspectorId) throws SQLException {
        UserRoleValidator.requireEnabledRole(inspectorId, "QUALITY_INSPECTOR", "质检员");
        if (!inspectionDao.assign(inspectionId, inspectorId)) {
            throw new BadRequestException("质检单不存在、状态不允许分配或已经被处理");
        }
        return true;
    }

    public boolean submitInspection(long inspectionId, long inspectorId, String result, String note) throws SQLException {
        String submittedResult = normalizeSubmittedResult(result);
        if (!"PASS".equals(submittedResult) && (note == null || note.isBlank())) {
            throw new BadRequestException("不合格或需返工时必须说明异常项目和原因");
        }
        if (!inspectionDao.submit(inspectionId, inspectorId, submittedResult, note)) {
            throw new BadRequestException("只能提交分配给本人的未完成质检单");
        }
        return true;
    }

    public long addInspectionItem(MesQualityInspectionItem item) throws SQLException {
        return itemDao.insert(item);
    }

    public long addAssignedInspectionItem(MesQualityInspectionItem item, long inspectorId) throws SQLException {
        MesQualityInspection inspection = requireAssignedInspection(item.inspectionId(), inspectorId);
        if (!List.of("CREATED", "IN_PROGRESS").contains(inspection.inspectionStatus())) {
            throw new BadRequestException("质检结果已提交，不能继续修改检验项目");
        }
        return itemDao.insert(item);
    }

    public List<MesQualityInspectionItem> getInspectionItems(long inspectionId) throws SQLException {
        return itemDao.findByInspectionId(inspectionId);
    }

    public List<MesQualityTrace> listAllTraces() throws SQLException {
        return traceDao.findAll();
    }

    public List<MesQualityTrace> listTracesByWorkOrder(long workOrderId) throws SQLException {
        return traceDao.findByWorkOrderId(workOrderId);
    }

    public List<MesQualityTrace> listTracesByInspection(long inspectionId) throws SQLException {
        return traceDao.findByInspectionId(inspectionId);
    }

    public Optional<MesQualityTrace> getTraceById(long traceId) throws SQLException {
        return traceDao.findById(traceId);
    }

    public boolean judgeInspection(long inspectionId, String status, String result, long reviewedBy) throws SQLException {
        MesQualityInspection inspection = inspectionDao.findById(inspectionId)
                .orElseThrow(() -> new BadRequestException("质检单不存在"));
        List<MesQualityInspectionItem> items = itemDao.findByInspectionId(inspectionId);

        String finalResult = resolveJudgementResult(result, inspection.submittedResult(), items);
        String finalStatus = "PASS".equals(finalResult) ? "APPROVED"
                : "REWORK".equals(finalResult) ? "REWORK_REQUIRED" : "REJECTED";
        if (!inspectionDao.updateStatus(inspectionId, finalStatus, finalResult, reviewedBy)) {
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
        return true;
    }

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

    private String normalizeSubmittedResult(String result) {
        String value = result == null ? "" : result.trim().toUpperCase();
        return switch (value) {
            case "PASS", "OK", "合格" -> "PASS";
            case "REWORK", "返工", "需返工" -> "REWORK";
            case "FAIL", "FAILED", "NG", "不合格" -> "FAIL";
            default -> throw new BadRequestException("质检结果只能是合格、不合格或需返工");
        };
    }

    private boolean isFail(String result) {
        return "FAIL".equalsIgnoreCase(result)
                || "FAILED".equalsIgnoreCase(result)
                || "NG".equalsIgnoreCase(result)
                || "不合格".equals(result);
    }

    private String traceStatus(String result) {
        return switch (result) {
            case "PASS" -> "NORMAL";
            case "REWORK" -> "REWORKED";
            default -> "QUALITY_RISK";
        };
    }

    private String generateCode(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }
}
