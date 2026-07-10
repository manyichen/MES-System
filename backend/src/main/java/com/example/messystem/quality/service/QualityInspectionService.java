package com.example.messystem.quality.service;

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

    public List<MesQualityInspection> listInspections() throws SQLException {
        return inspectionDao.findAll();
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

    public boolean judgeInspection(long inspectionId, String status, String result) throws SQLException {
        boolean updated = inspectionDao.updateStatus(inspectionId, status, result);
        if (!updated) {
            return false;
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
