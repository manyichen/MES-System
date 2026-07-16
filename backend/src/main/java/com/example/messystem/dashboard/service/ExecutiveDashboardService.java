package com.example.messystem.dashboard.service;

import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.dashboard.dao.ExecutiveDashboardDao;
import com.example.messystem.dashboard.dao.ExecutiveDashboardDao.Totals;
import com.example.messystem.dashboard.entity.ExecutiveDashboard;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** 构建仅供总经理查看的 MES 全流程只读经营视图。 */
public class ExecutiveDashboardService {
    private final ExecutiveDashboardDao dao = new ExecutiveDashboardDao();

    /** 校验经营视图权限，并将 DAO 汇总数据转换为可解释的业务指标。 */
    public ExecutiveDashboard build(AuthenticatedUser currentUser) {
        if (currentUser == null || !currentUser.hasRole("GENERAL_MANAGER")) {
            throw new BadRequestException("executive dashboard is only available to the general manager");
        }
        try {
            ExecutiveDashboardDao.DashboardData data = dao.load();
            return new ExecutiveDashboard(
                    LocalDateTime.now(),
                    metrics(data.totals()),
                    data.productionTrend(),
                    data.productionLines(),
                    data.alerts(),
                    departmentReports(data.totals(), data.submittedInspections(), data.activeMaintenance()),
                    auditFindings(data.totals()));
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    private static List<ExecutiveDashboard.Metric> metrics(Totals totals) {
        long riskCount = totals.shortageAlerts() + totals.openRework()
                + totals.equipmentFaults() + totals.pendingRepairs();
        return List.of(
                metric("order-volume", "\u8ba2\u5355\u8ba1\u5212\u91cf", totals.orderQty(), "\u6761", "blue",
                        "\u5f53\u524d\u7d2f\u8ba1 " + totals.orderCount() + " \u7b14\u5ba2\u6237\u8ba2\u5355"),
                metric("qualified-output", "\u5408\u683c\u4ea7\u51fa", totals.qualifiedQty(), "\u6761", "cyan",
                        "\u7d2f\u8ba1\u62a5\u5de5 " + totals.reportedQty() + " \u6761"),
                new ExecutiveDashboard.Metric("quality-rate", "\u5408\u683c\u7387",
                        percent(totals.qualifiedQty(), totals.reportedQty()), "%", "green",
                        "\u4e0d\u5408\u683c\u54c1 " + totals.defectQty() + " \u6761"),
                new ExecutiveDashboard.Metric("equipment-availability", "\u8bbe\u5907\u53ef\u7528\u7387",
                        percent(totals.equipmentTotal() - totals.equipmentFaults(), totals.equipmentTotal()),
                        "%", "blue", totals.equipmentFaults() + " \u53f0\u8bbe\u5907\u9700\u5173\u6ce8"),
                metric("active-work-orders", "\u5728\u5236\u5de5\u5355", totals.activeWorkOrders(), "\u5f20", "cyan",
                        "\u5df2\u5b8c\u6210 " + totals.completedWorkOrders() + " \u5f20"),
                metric("management-risks", "\u7ecf\u8425\u98ce\u9669\u4e8b\u9879", riskCount, "\u9879",
                        riskCount == 0 ? "green" : "amber",
                        "\u7f3a\u6599\u3001\u8fd4\u5de5\u3001\u8bbe\u5907\u4e0e\u7ef4\u4fee\u5f85\u95ed\u73af"));
    }

    private static List<ExecutiveDashboard.DepartmentReport> departmentReports(
            Totals totals, long submittedInspections, long activeMaintenance) {
        String period = LocalDate.now().getYear() + " \u5e74\u5ea6 \u00b7 \u622a\u81f3\u5f53\u524d";
        return List.of(
                report("\u4ed3\u50a8\u7269\u6d41", "\u4ed3\u5e93\u7ba1\u7406\u5458", period,
                        "\u53ef\u7528\u5e93\u5b58\u6279\u6b21", totals.availableInventory(), "\u6279", totals.shortageAlerts(),
                        totals.shortageAlerts() == 0 ? "\u672a\u53d1\u73b0\u5f85\u534f\u540c\u7684\u7269\u6599\u7f3a\u53e3"
                                : "\u5b58\u5728\u5f85\u95ed\u73af\u7684\u7269\u6599\u9884\u8b66"),
                report("\u8f66\u95f4\u751f\u4ea7", "\u8f66\u95f4\u7ba1\u7406\u5458", period,
                        "\u5de5\u5355\u5b8c\u6210\u7387", percent(totals.completedWorkOrders(), totals.workOrderCount()),
                        "%", totals.pendingReports(), totals.activeWorkOrders() + " \u5f20\u5de5\u5355\u5904\u4e8e\u6267\u884c\u4e2d"),
                report("\u8d28\u91cf\u7ba1\u63a7", "\u8d28\u91cf\u4e3b\u7ba1", period,
                        "\u62a5\u5de5\u5408\u683c\u7387", percent(totals.qualifiedQty(), totals.reportedQty()), "%",
                        totals.openRework() + submittedInspections,
                        totals.openRework() == 0 ? "\u672a\u53d1\u73b0\u672a\u95ed\u73af\u8fd4\u5de5"
                                : "\u8fd4\u5de5\u8ba2\u5355\u9700\u6301\u7eed\u7763\u529e"),
                report("\u8bbe\u5907\u4fdd\u969c", "\u8bbe\u5907\u4e3b\u7ba1", period,
                        "\u8bbe\u5907\u53ef\u7528\u7387",
                        percent(totals.equipmentTotal() - totals.equipmentFaults(), totals.equipmentTotal()), "%",
                        totals.equipmentFaults() + activeMaintenance,
                        totals.equipmentFaults() == 0 ? "\u672a\u53d1\u73b0\u505c\u673a\u8bbe\u5907"
                                : "\u6545\u969c\u4e0e\u7ef4\u4fee\u5de5\u5355\u5df2\u7eb3\u5165\u7763\u529e"));
    }

    private static List<ExecutiveDashboard.AuditFinding> auditFindings(Totals totals) {
        return List.of(
                finding("\u4ed3\u50a8\u7269\u6d41", "\u7269\u6599\u4fdd\u969c", totals.shortageAlerts(),
                        "\u5f85\u95ed\u73af\u7f3a\u6599\u9884\u8b66 " + totals.shortageAlerts() + " \u9879",
                        "\u5173\u6ce8\u4ed3\u50a8\u5907\u6599\u4e0e\u9884\u8b66\u63a5\u6536\u8fdb\u5ea6"),
                finding("\u8f66\u95f4\u751f\u4ea7", "\u5de5\u5355\u6267\u884c", totals.pendingReports(),
                        "\u5f85\u5ba1\u6838\u62a5\u5de5 " + totals.pendingReports() + " \u5f20\uff0c\u5728\u5236\u5de5\u5355 "
                                + totals.activeWorkOrders() + " \u5f20",
                        "\u6838\u5bf9\u5de5\u5355\u8fdb\u5ea6\u4e0e\u62a5\u5de5\u7ed3\u679c\u662f\u5426\u53ca\u65f6\u95ed\u73af"),
                finding("\u8d28\u91cf\u7ba1\u63a7", "\u8fd4\u5de5\u95ed\u73af", totals.openRework(),
                        "\u672a\u95ed\u73af\u8fd4\u5de5\u5355 " + totals.openRework() + " \u5f20",
                        "\u590d\u6838\u8d23\u4efb\u5f52\u5c5e\u3001\u5904\u7f6e\u65f6\u9650\u4e0e\u9a8c\u8bc1\u8bc1\u636e"),
                finding("\u8bbe\u5907\u4fdd\u969c", "\u8bbe\u5907\u7a33\u5b9a\u6027",
                        totals.equipmentFaults() + totals.pendingRepairs(),
                        "\u6545\u969c\u8bbe\u5907 " + totals.equipmentFaults() + " \u53f0\uff0c\u5f85\u5904\u7406\u62a5\u4fee "
                                + totals.pendingRepairs() + " \u9879",
                        "\u5ba1\u89c6\u6545\u969c\u54cd\u5e94\u4e0e\u7ef4\u4fee\u5b8c\u7ed3\u60c5\u51b5"));
    }

    private static ExecutiveDashboard.Metric metric(
            String key, String label, long value, String unit, String tone, String detail) {
        return new ExecutiveDashboard.Metric(key, label, String.valueOf(value), unit, tone, detail);
    }

    private static ExecutiveDashboard.DepartmentReport report(String department, String ownerRole, String period,
            String metricLabel, long value, String unit, long riskCount, String summary) {
        return report(department, ownerRole, period, metricLabel, String.valueOf(value), unit, riskCount, summary);
    }

    private static ExecutiveDashboard.DepartmentReport report(String department, String ownerRole, String period,
            String metricLabel, String value, String unit, long riskCount, String summary) {
        return new ExecutiveDashboard.DepartmentReport(
                department, ownerRole, period, metricLabel, value, unit, riskCount, summary);
    }

    private static ExecutiveDashboard.AuditFinding finding(
            String department, String title, long riskCount, String detail, String nextStep) {
        String severity = riskCount == 0 ? "NORMAL" : (riskCount >= 3 ? "HIGH" : "MEDIUM");
        return new ExecutiveDashboard.AuditFinding(department, title, severity, detail, nextStep);
    }

    private static String percent(long numerator, long denominator) {
        if (denominator <= 0) return "0.0";
        return String.format(Locale.ROOT, "%.1f", numerator * 100D / denominator);
    }
}
