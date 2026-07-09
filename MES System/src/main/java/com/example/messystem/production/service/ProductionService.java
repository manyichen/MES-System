package com.example.messystem.production.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.production.entity.MesPieceworkWage;
import com.example.messystem.production.entity.MesWorkReport;
import com.example.messystem.warehouse.service.InMemoryMesStore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductionService {
    private static final BigDecimal DEFAULT_PIECE_RATE = new BigDecimal("2.50");

    public List<MesWorkReport> listWorkReports() {
        return new ArrayList<>(InMemoryMesStore.workReports.values());
    }

    public MesWorkReport createWorkReport(MesWorkReport report) {
        if (report.workOrderId == null || report.workOrderId <= 0) {
            throw new BadRequestException("workOrderId is required");
        }
        report.reportId = InMemoryMesStore.nextId();
        report.reportNo = IdGenerator.nextCode("WR");
        report.reportQty = report.reportQty == null ? 0 : report.reportQty;
        report.qualifiedQty = report.qualifiedQty == null ? 0 : report.qualifiedQty;
        report.defectQty = report.defectQty == null ? 0 : report.defectQty;
        report.workHours = report.workHours == null ? BigDecimal.ZERO : report.workHours;
        if (report.qualifiedQty + report.defectQty > report.reportQty) {
            throw new BadRequestException("qualifiedQty + defectQty cannot exceed reportQty");
        }
        report.reportStatus = "SUBMITTED";
        report.reportTime = LocalDateTime.now();
        InMemoryMesStore.workReports.put(report.reportId, report);
        return report;
    }

    public MesWorkReport approveWorkReport(long reportId) {
        MesWorkReport report = InMemoryMesStore.workReports.get(reportId);
        if (report == null) {
            throw new NotFoundException("work report not found");
        }
        if (!"SUBMITTED".equals(report.reportStatus)) {
            throw new BadRequestException("only SUBMITTED reports can be approved");
        }
        report.reportStatus = "APPROVED";
        MesPieceworkWage wage = new MesPieceworkWage();
        wage.wageId = InMemoryMesStore.nextId();
        wage.reportId = report.reportId;
        wage.operatorId = report.operatorId;
        wage.pieceRate = DEFAULT_PIECE_RATE;
        wage.qualifiedQty = report.qualifiedQty;
        wage.wageAmount = DEFAULT_PIECE_RATE.multiply(BigDecimal.valueOf(report.qualifiedQty));
        wage.settlementStatus = "UNSETTLED";
        wage.createdAt = LocalDateTime.now();
        InMemoryMesStore.wages.put(wage.wageId, wage);
        return report;
    }

    public List<MesPieceworkWage> listWages() {
        return new ArrayList<>(InMemoryMesStore.wages.values());
    }
}
