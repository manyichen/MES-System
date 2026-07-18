/*
 * 答辩定位：驾驶舱、反馈与产品追溯 模块的 ExecutiveDashboard。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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

    /**
     * 公共能力：执行 Metric 对应的业务步骤。
     * 由 ExecutiveDashboard 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public record Metric(String key, String label, String value, String unit, String tone, String detail) {
    }

    /**
     * 公共能力：执行 TrendPoint 对应的业务步骤。
     * 由 ExecutiveDashboard 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public record TrendPoint(String day, long reportedQty, long qualifiedQty, long defectQty) {
    }

    /**
     * 公共能力：执行 LineSnapshot 对应的业务步骤。
     * 由 ExecutiveDashboard 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
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

    /**
     * 公共能力：执行 Alert 对应的业务步骤。
     * 由 ExecutiveDashboard 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public record Alert(String domain, String title, String detail, String severity, LocalDateTime occurredAt) {
    }

    /**
     * 公共能力：执行 DepartmentReport 对应的业务步骤。
     * 由 ExecutiveDashboard 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
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

    /**
     * 公共能力：执行 AuditFinding 对应的业务步骤。
     * 由 ExecutiveDashboard 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public record AuditFinding(String department, String title, String severity, String detail, String nextStep) {
    }
}
