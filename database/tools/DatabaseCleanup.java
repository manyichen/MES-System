import com.example.messystem.common.Db;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** 在单个数据库事务中执行清理 SQL，并在提交前验证数量上限和主要引用完整性。 */
public class DatabaseCleanup {
    private record Scenario(String table, String groupBy, int limit) {
    }

    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario("mes_audit_log", "event_type, result", 2),
            new Scenario("mes_customer_order", "order_status", 2),
            new Scenario("mes_equipment", "equipment_status", 2),
            new Scenario("mes_equipment_repair_report", "repair_status", 2),
            new Scenario("mes_inventory", "quality_status", 2),
            new Scenario("mes_inventory_transaction", "transaction_type", 2),
            new Scenario("mes_kitting_analysis", "result_status", 2),
            new Scenario("mes_kitting_shortage_item", "shortage_type", 2),
            new Scenario("mes_maintenance_order", "maintenance_status", 2),
            new Scenario("mes_maintenance_plan", "plan_status", 2),
            new Scenario("mes_management_feedback", "feedback_status, feedback_type", 1),
            new Scenario("mes_material", "material_type", 2),
            new Scenario("mes_material_requisition", "request_status", 2),
            new Scenario("mes_material_requisition_item", "item_status", 2),
            new Scenario("mes_permission_apply", "apply_status", 2),
            new Scenario("mes_picking_task", "task_status", 2),
            new Scenario("mes_piecework_wage", "settlement_status", 2),
            new Scenario("mes_process_route", "coalesce(required_equipment_type, '<NULL>')", 1),
            new Scenario("mes_product", "enabled", 2),
            new Scenario("mes_production_line", "line_status", 2),
            new Scenario("mes_production_task", "task_status, kitting_status", 1),
            new Scenario("mes_quality_inspection", "inspection_status", 2),
            new Scenario("mes_quality_inspection_item", "item_result", 2),
            new Scenario("mes_quality_trace", "trace_status", 2),
            new Scenario("mes_rework_order", "rework_status", 2),
            new Scenario("mes_robot", "robot_status", 2),
            new Scenario("mes_robot_delivery_task", "delivery_status", 2),
            new Scenario("mes_shortage_alert", "alert_status", 2),
            new Scenario("mes_tire_instance", "tire_status", 2),
            new Scenario("mes_tire_qrcode", "qrcode_status", 2),
            new Scenario("mes_trace_document", "document_type", 2),
            new Scenario("mes_user", "role_code", 1),
            new Scenario("mes_warehouse", "warehouse_type", 2),
            new Scenario("mes_warehouse_location", "warehouse_id", 1),
            new Scenario("mes_work_order", "work_order_status", 2),
            new Scenario("mes_work_order_operation_log", "operation_type", 2),
            new Scenario("mes_work_report", "report_status", 2));

    private static final List<String> ORPHAN_CHECKS = List.of(
            orphan("mes_production_task", "order_id", "mes_customer_order", "order_id"),
            orphan("mes_kitting_analysis", "task_id", "mes_production_task", "task_id"),
            orphan("mes_shortage_alert", "task_id", "mes_production_task", "task_id"),
            orphan("mes_work_order", "task_id", "mes_production_task", "task_id"),
            orphan("mes_work_order", "product_id", "mes_product", "product_id"),
            orphan("mes_work_order", "line_id", "mes_production_line", "line_id"),
            orphan("mes_work_order", "process_id", "mes_process_route", "process_id"),
            orphan("mes_material_requisition", "work_order_id", "mes_work_order", "work_order_id"),
            orphan("mes_material_requisition", "warehouse_id", "mes_warehouse", "warehouse_id"),
            orphan("mes_material_requisition_item", "requisition_id", "mes_material_requisition", "requisition_id"),
            orphan("mes_inventory", "material_id", "mes_material", "material_id"),
            orphan("mes_inventory", "warehouse_id", "mes_warehouse", "warehouse_id"),
            orphan("mes_inventory", "location_id", "mes_warehouse_location", "location_id"),
            orphan("mes_picking_task", "requisition_id", "mes_material_requisition", "requisition_id"),
            orphan("mes_robot_delivery_task", "picking_task_id", "mes_picking_task", "picking_task_id"),
            orphan("mes_work_report", "work_order_id", "mes_work_order", "work_order_id"),
            orphan("mes_piecework_wage", "report_id", "mes_work_report", "report_id"),
            orphan("mes_quality_inspection", "work_order_id", "mes_work_order", "work_order_id"),
            orphan("mes_quality_inspection", "work_report_id", "mes_work_report", "report_id"),
            orphan("mes_rework_order", "inspection_id", "mes_quality_inspection", "inspection_id"),
            orphan("mes_equipment_repair_report", "equipment_id", "mes_equipment", "equipment_id"),
            orphan("mes_maintenance_order", "equipment_id", "mes_equipment", "equipment_id"),
            orphan("mes_tire_instance", "inspection_id", "mes_quality_inspection", "inspection_id"),
            orphan("mes_tire_instance", "work_report_id", "mes_work_report", "report_id"));

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !("--confirm-cloud-cleanup".equals(args[0]) || "--dry-run".equals(args[0]))) {
            throw new IllegalArgumentException(
                    "用法: DatabaseCleanup <--dry-run|--confirm-cloud-cleanup> <SQL文件>");
        }
        boolean commit = "--confirm-cloud-cleanup".equals(args[0]);
        Path sqlFile = Path.of(args[1]).toAbsolutePath().normalize();
        List<String> statements = splitSql(Files.readString(sqlFile, StandardCharsets.UTF_8));
        System.out.println("清理脚本: " + sqlFile);
        System.out.println("语句数量: " + statements.size());

        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try (Statement statement = connection.createStatement()) {
                statement.execute("select pg_advisory_xact_lock(hashtext('mes_cloud_data_cleanup_v9'))");
                int index = 0;
                for (String sql : statements) {
                    index++;
                    int affected = statement.executeUpdate(sql);
                    System.out.println(index + "/" + statements.size() + " affected=" + affected
                            + " sql=" + firstLine(sql));
                }
                validate(connection);
                if (commit) {
                    connection.commit();
                    System.out.println("清理事务已提交。");
                } else {
                    connection.rollback();
                    System.out.println("试运行校验通过，事务已主动回滚，数据库未修改。");
                }
            } catch (Exception ex) {
                connection.rollback();
                System.out.println("清理事务已回滚: " + ex.getMessage());
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private static void validate(Connection connection) throws Exception {
        requireScalar(connection, "select count(*) from mes_user", 12, "正式账号总数");
        requireScalar(connection, "select count(*) from mes_role", 12, "正式角色总数");
        requireScalar(connection, "select count(*) from mes_user_session", 0, "登录会话清空");
        requireScalar(connection, "select count(*) from mes_dashboard_metric", 0, "废弃看板数据清空");
        requireScalar(connection, """
                select count(*) from (
                    select role_code from mes_user group by role_code having count(*) <> 1
                ) invalid
                """, 0, "每个角色一个账号");
        requireScalar(connection, """
                select count(*) from mes_user
                where username not in (
                    'admin', 'mes_hr', 'mes_general', 'mes_pmc', 'mes_workshop', 'mes_operator',
                    'mes_warehouse', 'mes_quality_mgr', 'mes_inspector', 'mes_process',
                    'mes_equipment_mgr', 'mes_maintainer')
                """, 0, "无额外测试账号");

        for (Scenario scenario : SCENARIOS) {
            String sql = "select coalesce(max(group_count), 0) from (select count(*) group_count from "
                    + scenario.table + " group by " + scenario.groupBy + ") grouped";
            long max = scalar(connection, sql);
            if (max > scenario.limit) {
                throw new IllegalStateException(scenario.table + " 场景数量 " + max + " 超过上限 " + scenario.limit);
            }
        }
        requireAtMost(connection, "mes_product_bom", 2);
        requireAtMost(connection, "mes_quality_standard", 2);
        requireAtMost(connection, "mes_quality_standard_item", 2);
        requireAtMost(connection, "mes_user_line_scope", 1);
        requireAtMost(connection, "mes_user_warehouse_scope", 1);

        for (String sql : ORPHAN_CHECKS) requireScalar(connection, sql, 0, "业务引用完整性");
        System.out.println("提交前校验通过。");
    }

    private static void requireAtMost(Connection connection, String table, long limit) throws Exception {
        long actual = scalar(connection, "select count(*) from " + table);
        if (actual > limit) throw new IllegalStateException(table + " 行数 " + actual + " 超过上限 " + limit);
    }

    private static void requireScalar(Connection connection, String sql, long expected, String label) throws Exception {
        long actual = scalar(connection, sql);
        if (actual != expected) throw new IllegalStateException(label + ": expected=" + expected + ", actual=" + actual);
    }

    private static long scalar(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static String orphan(String child, String childColumn, String parent, String parentColumn) {
        return "select count(*) from " + child + " c left join " + parent + " p on p." + parentColumn
                + " = c." + childColumn + " where c." + childColumn + " is not null and p." + parentColumn + " is null";
    }

    private static List<String> splitSql(String rawSql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : rawSql.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
            current.append(line).append(System.lineSeparator());
            if (trimmed.endsWith(";")) {
                String sql = current.toString().trim();
                statements.add(sql.substring(0, sql.length() - 1).trim());
                current.setLength(0);
            }
        }
        if (!current.toString().isBlank()) statements.add(current.toString().trim());
        return statements;
    }

    private static String firstLine(String sql) {
        return sql.split("\\R", 2)[0].trim();
    }
}
