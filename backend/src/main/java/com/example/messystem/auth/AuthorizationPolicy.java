package com.example.messystem.auth;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

final class AuthorizationPolicy {
    private static final List<Rule> RULES = List.of(
            rule("GET", "db/ping", "system.health.read"),

            rule("GET", "dashboard/.*", "dashboard.read"),
            rule("POST", "dashboard/.*", "dashboard.system.read"),

            rule("GET", "orders(?:/.*)?", "planning.read"),
            rule("POST", "orders", "planning.order.create"),
            rule("GET", "production-tasks(?:/.*)?", "planning.read"),
            rule("POST", "production-tasks", "planning.task.create"),
            rule("POST", "production-tasks/\\d+/(?:kitting|release)", "planning.task.release"),
            rule("POST", "ai/planning/advice", "planning.task.release"),
            any("GET", "work-orders(?:/.*)?", "planning.read", "planning.work_order.read"),
            rule("POST", "work-orders", "planning.work_order.create"),
            rule("POST", "work-orders/\\d+/dispatch", "planning.work_order.dispatch"),
            rule("POST", "work-orders/\\d+/receive", "planning.work_order.receive"),

            rule("GET", "(?:materials|warehouses|warehouse-locations|inventory|requisitions|picking-tasks|robots|robot-delivery-tasks)(?:/.*)?", "warehouse.read"),
            rule("POST|PUT|DELETE", "(?:materials|warehouses|warehouse-locations|robots)(?:/.*)?", "warehouse.master.manage"),
            rule("POST|PUT|DELETE", "warehouses/locations(?:/.*)?", "warehouse.master.manage"),
            rule("POST|PUT|DELETE", "inventory(?:/.*)?", "warehouse.inventory.adjust"),
            rule("POST", "requisitions", "warehouse.requisition.create"),
            rule("POST", "requisitions/\\d+/(?:approve|reject)", "warehouse.requisition.approve"),
            rule("POST", "picking-tasks(?:/.*)?", "warehouse.picking.execute"),
            rule("POST", "robot-delivery-tasks(?:/.*)?", "warehouse.delivery.execute"),

            rule("GET", "work-reports(?:/.*)?", "production.read"),
            rule("POST", "work-reports", "production.report.create"),
            rule("PUT", "work-reports/\\d+", "production.report.update_own"),
            rule("DELETE", "work-reports/\\d+", "business.delete"),
            rule("POST", "work-reports/\\d+/approve", "production.report.review"),
            any("GET", "piecework-wages(?:/.*)?", "production.wage.read_self", "production.wage.read_summary", "production.wage.read_all"),

            rule("GET", "quality-inspections(?:/.*)?", "quality.read"),
            rule("POST", "quality-inspections", "quality.inspection.create"),
            rule("POST", "quality-inspections/\\d+/assign", "quality.inspection.assign"),
            rule("POST", "quality-inspections/\\d+/(?:items|submit)", "quality.inspect"),
            rule("POST", "quality-inspections/\\d+/judge", "quality.review"),
            rule("GET", "rework-orders(?:/.*)?", "quality.read"),
            rule("POST", "rework-orders(?:/.*)?", "quality.rework.manage"),
            rule("GET", "quality-traces(?:/.*)?", "trace.read"),

            rule("GET", "equipment(?:/.*)?", "equipment.read"),
            rule("POST", "equipment", "equipment.manage"),
            rule("PUT|POST", "equipment/\\d+/status", "equipment.manage"),
            rule("POST", "equipment/\\d+/repair-reports", "equipment.fault.report"),
            rule("POST", "equipment/\\d+/maintenance-orders", "equipment.maintenance.assign"),
            rule("POST", "equipment/maintenance-plans", "equipment.manage"),
            rule("GET", "equipment-repair-reports(?:/.*)?", "equipment.read"),
            rule("POST", "equipment-repair-reports", "equipment.fault.report"),
            rule("POST", "equipment-repair-reports/\\d+/approve", "equipment.repair.review"),
            rule("POST", "equipment-repair-reports/\\d+/to-maintenance-order", "equipment.maintenance.assign"),
            rule("GET", "maintenance-orders(?:/.*)?", "equipment.read"),
            rule("POST", "maintenance-orders/\\d+/assign", "equipment.maintenance.assign"),
            rule("POST", "maintenance-orders/\\d+/finish", "equipment.maintenance.execute"),
            rule("POST", "maintenance-orders/\\d+/accept", "equipment.maintenance.accept"),
            rule("GET", "maintenance-plans(?:/.*)?", "equipment.read"),
            rule("POST", "maintenance-plans", "equipment.manage"),

            rule("GET", "products(?:/.*)?", "master.read"),
            rule("POST", "products(?:/.*)?", "master.manage"),
            rule("GET", "production-lines(?:/.*)?", "master.read"),
            rule("POST", "production-lines(?:/.*)?", "master.manage"),
            rule("GET", "process-routes(?:/.*)?", "process.read"),
            rule("POST", "process-routes(?:/.*)?", "process.manage"),
            rule("GET", "sync-logs(?:/.*)?", "system.health.read"),

            rule("GET", "product-traces(?:/.*)?", "trace.read"),
            rule("POST", "product-traces", "trace.create"),
            any("GET", "tire-labels(?:/.*)?", "trace.read", "warehouse.read"),
            rule("POST", "tire-labels/generate", "trace.tire.generate"),
            rule("POST", "tire-labels/\\d+/print", "trace.tire.print"),
            rule("GET", "management-feedback(?:/.*)?", "feedback.read"),
            rule("POST", "management-feedback", "feedback.create"),
            rule("POST", "management-feedback/\\d+/close", "feedback.close"),

            rule("GET", "users(?:/.*)?", "user.read"),
            rule("POST", "users", "user.create"),
            rule("PUT", "users/\\d+/role", "user.update_role"),
            rule("GET", "access/roles(?:/.*)?", "role.read"),
            rule("GET", "access/permissions(?:/.*)?", "role.read"),
            rule("GET", "access/system-maintenance", "system.health.read"),
            any("GET", "access/permission-applications", "permission.apply", "permission.review", "role.manage"),
            rule("POST", "access/permission-applications", "permission.apply"),
            rule("POST", "access/permission-applications/\\d+/review", "permission.review"),
            rule("POST", "access/permission-applications/\\d+/apply", "role.manage"),
            rule("GET", "access/users/\\d+/roles", "user.read"),
            rule("PUT", "access/users/\\d+/roles", "user.update_role"),
            rule("GET|PUT", "access/users/\\d+/data-scopes", "data_scope.manage")
    );

    private AuthorizationPolicy() {
    }

    static Set<String> requiredPermissions(String method, String path) {
        return RULES.stream()
                .filter(rule -> rule.matches(method, path))
                .findFirst()
                .map(Rule::permissions)
                .orElse(Set.of());
    }

    private static Rule rule(String methods, String path, String permission) {
        return new Rule(Pattern.compile("^(?:" + methods + ")$"), Pattern.compile("^(?:" + path + ")$"), Set.of(permission));
    }

    private static Rule any(String methods, String path, String... permissions) {
        return new Rule(Pattern.compile("^(?:" + methods + ")$"), Pattern.compile("^(?:" + path + ")$"), Set.of(permissions));
    }

    private record Rule(Pattern methods, Pattern path, Set<String> permissions) {
        boolean matches(String method, String requestPath) {
            return methods.matcher(method).matches() && path.matcher(requestPath).matches();
        }
    }
}
