/*
 * 答辩定位：驾驶舱、反馈与产品追溯 模块的 ExecutiveDashboardDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.dashboard.dao;

import com.example.messystem.common.Db;
import com.example.messystem.dashboard.entity.ExecutiveDashboard;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 读取总经理经营看板所需的跨部门聚合数据。 */
public class ExecutiveDashboardDao {
    /**
     * 数据访问：装载业务数据。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public DashboardData load() throws SQLException {
        try (Connection connection = Db.getConnection()) {
            return new DashboardData(
                    readTotals(connection),
                    productionTrend(connection),
                    productionLines(connection),
                    alerts(connection),
                    scalar(connection, "select count(*) from mes_quality_inspection where inspection_status = 'SUBMITTED'"),
                    scalar(connection, "select count(*) from mes_maintenance_order where maintenance_status in ('CREATED', 'ASSIGNED', 'IN_PROGRESS')"));
        }
    }

    /**
     * 数据访问：执行 readTotals 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static Totals readTotals(Connection connection) throws SQLException {
        String sql = """
                select
                    (select count(*) from mes_customer_order) as order_count,
                    (select coalesce(sum(order_qty), 0) from mes_customer_order) as order_qty,
                    (select count(*) from mes_work_order where work_order_status not in ('COMPLETED', 'CLOSED', 'CANCELLED')) as active_work_orders,
                    (select count(*) from mes_work_order where work_order_status in ('COMPLETED', 'CLOSED')) as completed_work_orders,
                    (select count(*) from mes_work_order) as work_order_count,
                    (select coalesce(sum(report_qty), 0) from mes_work_report) as reported_qty,
                    (select coalesce(sum(qualified_qty), 0) from mes_work_report) as qualified_qty,
                    (select coalesce(sum(defect_qty), 0) from mes_work_report) as defect_qty,
                    (select count(*) from mes_equipment where enabled = 1) as equipment_total,
                    (select count(*) from mes_equipment where enabled = 1 and equipment_status in ('FAULT', 'REPAIRING', 'DOWN')) as equipment_faults,
                    (select count(*) from mes_shortage_alert where alert_status in ('OPEN', 'ACCEPTED')) as shortage_alerts,
                    (select count(*) from mes_rework_order where rework_status not in ('FINISHED', 'CLOSED')) as open_rework,
                    (select count(*) from mes_equipment_repair_report where repair_status in ('REPORTED', 'APPROVED')) as pending_repairs,
                    (select count(*) from mes_inventory where available_qty > 0) as available_inventory,
                    (select count(*) from mes_work_report where report_status = 'SUBMITTED') as pending_reports
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            rs.next();
            return new Totals(
                    rs.getLong("order_count"), rs.getLong("order_qty"),
                    rs.getLong("active_work_orders"), rs.getLong("completed_work_orders"), rs.getLong("work_order_count"),
                    rs.getLong("reported_qty"), rs.getLong("qualified_qty"), rs.getLong("defect_qty"),
                    rs.getLong("equipment_total"), rs.getLong("equipment_faults"),
                    rs.getLong("shortage_alerts"), rs.getLong("open_rework"), rs.getLong("pending_repairs"),
                    rs.getLong("available_inventory"), rs.getLong("pending_reports"));
        }
    }

    /**
     * 数据访问：执行 productionTrend 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static List<ExecutiveDashboard.TrendPoint> productionTrend(Connection connection) throws SQLException {
        String sql = """
                select to_char(days.day, 'MM-DD') as day,
                       coalesce(sum(reports.report_qty), 0) as reported_qty,
                       coalesce(sum(reports.qualified_qty), 0) as qualified_qty,
                       coalesce(sum(reports.defect_qty), 0) as defect_qty
                from generate_series(current_date - 6, current_date, interval '1 day') as days(day)
                left join mes_work_report reports
                    on reports.report_time >= days.day
                    and reports.report_time < days.day + interval '1 day'
                group by days.day
                order by days.day
                """;
        List<ExecutiveDashboard.TrendPoint> points = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                points.add(new ExecutiveDashboard.TrendPoint(
                        rs.getString("day"), rs.getLong("reported_qty"),
                        rs.getLong("qualified_qty"), rs.getLong("defect_qty")));
            }
        }
        return points;
    }

    /**
     * 数据访问：执行 productionLines 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static List<ExecutiveDashboard.LineSnapshot> productionLines(Connection connection) throws SQLException {
        String sql = """
                with work_orders as (
                    select line_id,
                           count(*) filter (where work_order_status not in ('COMPLETED', 'CLOSED', 'CANCELLED')) as active_work_orders,
                           coalesce(sum(planned_qty) filter (where work_order_status not in ('COMPLETED', 'CLOSED', 'CANCELLED')), 0) as planned_qty,
                           coalesce(sum(actual_qty) filter (where work_order_status not in ('COMPLETED', 'CLOSED', 'CANCELLED')), 0) as actual_qty
                    from mes_work_order group by line_id
                ), equipment as (
                    select line_id,
                           count(*) filter (where enabled = 1) as equipment_total,
                           count(*) filter (where enabled = 1 and equipment_status in ('FAULT', 'REPAIRING', 'DOWN')) as equipment_faults
                    from mes_equipment group by line_id
                )
                select lines.line_id, lines.line_code, lines.line_name, lines.line_status,
                       coalesce(lines.daily_capacity, 0) as daily_capacity,
                       coalesce(work_orders.active_work_orders, 0) as active_work_orders,
                       coalesce(work_orders.planned_qty, 0) as planned_qty,
                       coalesce(work_orders.actual_qty, 0) as actual_qty,
                       coalesce(equipment.equipment_total, 0) as equipment_total,
                       coalesce(equipment.equipment_faults, 0) as equipment_faults
                from mes_production_line lines
                left join work_orders on work_orders.line_id = lines.line_id
                left join equipment on equipment.line_id = lines.line_id
                where lines.enabled = 1 order by lines.line_id
                """;
        List<ExecutiveDashboard.LineSnapshot> lines = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                lines.add(new ExecutiveDashboard.LineSnapshot(
                        rs.getLong("line_id"), rs.getString("line_code"), rs.getString("line_name"),
                        rs.getString("line_status"), rs.getLong("daily_capacity"),
                        rs.getLong("active_work_orders"), rs.getLong("planned_qty"), rs.getLong("actual_qty"),
                        rs.getLong("equipment_total"), rs.getLong("equipment_faults")));
            }
        }
        return lines;
    }

    /**
     * 数据访问：执行 alerts 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static List<ExecutiveDashboard.Alert> alerts(Connection connection) throws SQLException {
        String sql = """
                select domain, title, detail, severity, occurred_at
                from (
                    select 'MATERIAL' as domain, coalesce(material_name, alert_no) as title,
                           alert_content as detail, severity, created_at as occurred_at
                    from mes_shortage_alert where alert_status in ('OPEN', 'ACCEPTED')
                    union all
                    select 'QUALITY' as domain, rework_order_no as title,
                           rework_reason as detail, 'HIGH' as severity, created_at as occurred_at
                    from mes_rework_order where rework_status not in ('FINISHED', 'CLOSED')
                    union all
                    select 'EQUIPMENT' as domain, repair_report_no as title,
                           fault_desc as detail, fault_level as severity, report_time as occurred_at
                    from mes_equipment_repair_report where repair_status in ('REPORTED', 'APPROVED')
                ) risks
                order by occurred_at desc nulls last limit 6
                """;
        List<ExecutiveDashboard.Alert> alerts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                alerts.add(new ExecutiveDashboard.Alert(
                        rs.getString("domain"), rs.getString("title"), rs.getString("detail"),
                        rs.getString("severity"), rs.getObject("occurred_at", LocalDateTime.class)));
            }
        }
        return alerts;
    }

    /**
     * 数据访问：执行 scalar 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static long scalar(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    /**
     * 数据访问：执行 DashboardData 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record DashboardData(Totals totals, List<ExecutiveDashboard.TrendPoint> productionTrend,
            List<ExecutiveDashboard.LineSnapshot> productionLines, List<ExecutiveDashboard.Alert> alerts,
            long submittedInspections, long activeMaintenance) {
    }

    /**
     * 数据访问：执行 Totals 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record Totals(long orderCount, long orderQty, long activeWorkOrders, long completedWorkOrders,
            long workOrderCount, long reportedQty, long qualifiedQty, long defectQty,
            long equipmentTotal, long equipmentFaults, long shortageAlerts, long openRework,
            long pendingRepairs, long availableInventory, long pendingReports) {
    }
}
