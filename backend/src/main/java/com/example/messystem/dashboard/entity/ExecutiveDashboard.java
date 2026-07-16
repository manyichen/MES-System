package com.example.messystem.dashboard.entity;

import java.time.LocalDateTime;
import java.util.List;

/** 总经理经营看板使用的只读聚合模型。 */
public record ExecutiveDashboard(
        LocalDateTime generatedAt,
        List<Metric> metrics,
        List<TrendPoint> productionTrend,
        List<LineSnapshot> productionLines,
        List<Alert> alerts,
        List<DepartmentReport> departmentReports,
        List<AuditFinding> auditFindings) {

    public record Metric(String key, String label, String value, String unit, String tone, String detail) {
    }

    public record TrendPoint(String day, long reportedQty, long qualifiedQty, long defectQty) {
    }

    public record LineSnapshot(
            long lineId,
            String lineCode,
            String lineName,
            String lineStatus,
            long dailyCapacity,
            long activeWorkOrders,
            long plannedQty,
            long actualQty,
            long equipmentTotal,
            long equipmentFaults) {
    }

    public record Alert(String domain, String title, String detail, String severity, LocalDateTime occurredAt) {
    }

    public record DepartmentReport(
            String department,
            String ownerRole,
            String period,
            String metricLabel,
            String metricValue,
            String unit,
            long riskCount,
            String summary) {
    }

    public record AuditFinding(String department, String title, String severity, String detail, String nextStep) {
    }
}
