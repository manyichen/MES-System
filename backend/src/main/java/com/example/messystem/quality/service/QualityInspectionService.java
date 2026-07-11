package com.example.messystem.quality.service;

import com.example.messystem.quality.dao.QualityInspectionDao;
import com.example.messystem.quality.dao.QualityInspectionItemDao;
import com.example.messystem.quality.dao.QualityTraceDao;
import com.example.messystem.quality.dao.ReworkOrderDao;
import com.example.messystem.quality.entity.MesQualityInspection;
import com.example.messystem.quality.entity.MesQualityInspectionItem;
import com.example.messystem.quality.entity.MesQualityTrace;
import com.example.messystem.quality.entity.MesReworkOrder;
import com.example.messystem.common.BadRequestException;

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
        if (!inspectionDao.assign(inspectionId, inspectorId)) {
            throw new BadRequestException("质检单不存在、状态不允许分配或已被处理");
        }
        return true;
    }

    public boolean submitInspection(long inspectionId, long inspectorId) throws SQLException {
        if (!inspectionDao.submit(inspectionId, inspectorId)) {
            throw new BadRequestException("只能提交分配给本人的未完成质检单，且至少需要一条检验项目");
        }
        return true;
    }

    public long addInspectionItem(MesQualityInspectionItem item) throws SQLException {
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
        String finalStatus = "PASS".equalsIgnoreCase(result) ? "APPROVED"
                : "REWORK".equalsIgnoreCase(result) ? "REWORK_REQUIRED" : status;
        boolean updated = inspectionDao.updateStatus(inspectionId, finalStatus, result, reviewedBy);
        if (!updated) {
            throw new BadRequestException("只有质检员已提交的检验结果才能审核");
        }
        // Optional: generate trace or rework order if needed
        if ("REWORK".equalsIgnoreCase(result)) {
            MesReworkOrder reworkOrder = new MesReworkOrder(
                    null,
                    generateCode("RW"),
                    null,
                    inspectionId,
                    "Quality rework due to inspection",
                    "CREATED",
                    null,
                    LocalDateTime.now(),
                    null
            );
            long reworkId = reworkOrderDao.insert(reworkOrder);
            MesQualityTrace trace = new MesQualityTrace(
                    null,
                    generateCode("TRACE"),
                    null,
                    null,
                    null,
                    null,
                    inspectionId,
                    reworkId,
                    "REWORKED",
                    LocalDateTime.now()
            );
            traceDao.insert(trace);
        }
        return true;
    }

    private String generateCode(String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }
}
