package com.example.messystem.production.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.production.dao.ProductionDao;
import com.example.messystem.production.entity.MesPieceworkWage;
import com.example.messystem.production.entity.MesWorkReport;
import java.sql.SQLException;
import java.util.List;

public class ProductionService {
    private final ProductionDao dao = new ProductionDao();

    public List<MesWorkReport> listWorkReports() {
        return database(dao::listWorkReports);
    }

    public List<MesWorkReport> listWorkReportsByOperator(long operatorId) {
        return database(() -> dao.listWorkReportsByOperator(operatorId));
    }

    public MesWorkReport createWorkReport(MesWorkReport report) {
        if (report.workOrderId == null || report.workOrderId <= 0) {
            throw new BadRequestException("workOrderId is required");
        }
        report.reportQty = report.reportQty == null ? 0 : report.reportQty;
        report.qualifiedQty = report.qualifiedQty == null ? 0 : report.qualifiedQty;
        report.defectQty = report.defectQty == null ? 0 : report.defectQty;
        if (report.reportQty < 0 || report.qualifiedQty < 0 || report.defectQty < 0) {
            throw new BadRequestException("quantities cannot be negative");
        }
        if (report.qualifiedQty + report.defectQty > report.reportQty) {
            throw new BadRequestException("qualifiedQty + defectQty cannot exceed reportQty");
        }
        return database(() -> dao.insertWorkReport(report));
    }

    public MesWorkReport getWorkReport(long reportId) {
        return database(() -> dao.findWorkReport(reportId));
    }

    public MesWorkReport updateWorkReport(long reportId, MesWorkReport report) {
        if (report.reportQty != null && report.reportQty < 0
                || report.qualifiedQty != null && report.qualifiedQty < 0
                || report.defectQty != null && report.defectQty < 0) {
            throw new BadRequestException("quantities cannot be negative");
        }
        return database(() -> dao.updateWorkReport(reportId, report));
    }

    public void deleteWorkReport(long reportId) {
        database(() -> {
            dao.deleteWorkReport(reportId);
            return null;
        });
    }

    public List<MesWorkReport> listWorkReportsByWorkOrder(long workOrderId) {
        if (workOrderId <= 0) {
            throw new BadRequestException("workOrderId is required");
        }
        return database(() -> dao.listWorkReportsByWorkOrder(workOrderId));
    }

    public MesWorkReport approveWorkReport(long reportId) {
        return database(() -> dao.approveWorkReport(reportId));
    }

    public List<MesPieceworkWage> listWages() {
        return database(dao::listWages);
    }

    public List<MesWorkReport> listWorkReportsByWorkOrderAndOperator(long workOrderId, long operatorId) {
        return database(() -> dao.listWorkReportsByWorkOrderAndOperator(workOrderId, operatorId));
    }

    public List<MesPieceworkWage> listWagesByOperator(long operatorId) {
        return database(() -> dao.listWagesByOperator(operatorId));
    }

    public java.util.Map<String, Object> wageSummary() {
        return database(dao::wageSummary);
    }

    public java.util.Map<String, Object> wageSummaryForWorkshop(long userId) {
        return database(() -> dao.wageSummaryForWorkshop(userId));
    }

    public MesPieceworkWage getWage(long wageId) {
        return database(() -> dao.findWage(wageId));
    }

    public List<MesPieceworkWage> listWagesByReport(long reportId) {
        if (reportId <= 0) {
            throw new BadRequestException("workReportId is required");
        }
        return database(() -> dao.listWagesByReport(reportId));
    }

    public List<MesPieceworkWage> listWagesByReportAndOperator(long reportId, long operatorId) {
        return database(() -> dao.listWagesByReportAndOperator(reportId, operatorId));
    }

    private static <T> T database(SqlCall<T> call) {
        try {
            return call.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T execute() throws SQLException;
    }
}
