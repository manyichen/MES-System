/*
 * 答辩定位：登录认证与会话 模块的 AuthorizationPolicy。
 * 分层职责：安全边界：在业务方法执行前完成身份、权限或数据范围判断，避免只依赖前端隐藏按钮。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.auth;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 接口级 RBAC 权限白名单。
 * 每条 Rule 由 HTTP 方法正则、去掉 /api 前缀后的路径正则、一个或多个可接受权限点组成；
 * AuthFilter 采用 any-match，因此 {@code any(...)} 表示多个岗位权限满足任意一个即可访问。
 * 列表未命中的接口返回空集合并被默认拒绝，新接口必须同步登记并补 AuthorizationPolicyTest。
 */
final class AuthorizationPolicy {
    /**
     * 权限规则按模块排列，便于从业务接口反查权限点。路径中的数字 ID 用 {@code \d+} 限定，
     * 避免过宽正则把相似但未授权的接口一起放行。
     */
    private static final List<Rule> RULES = List.of(
            rule("GET", "db/ping", "system.health.read"),

            rule("GET", "dashboard/(?:my-summary|executive)", "dashboard.read"),

            rule("GET", "planning/reworks", "planning.rework.read"),
            rule("POST", "planning/reworks/\\d+/tasks", "planning.rework.plan"),
            rule("GET", "orders(?:/.*)?", "planning.read"),
            rule("POST", "orders", "planning.order.create"),
            rule("GET", "production-tasks(?:/.*)?", "planning.read"),
            rule("POST", "production-tasks", "planning.task.create"),
            rule("POST", "production-tasks/\\d+/(?:kitting|shortage-alerts)", "planning.task.release"),
            any("GET", "shortage-alerts(?:/.*)?", "planning.read", "warehouse.read"),
            rule("POST", "shortage-alerts/\\d+/accept", "warehouse.inventory.adjust"),
            rule("POST", "ai/planning/advice", "planning.work_order.create"),
            any("GET", "work-orders(?:/.*)?", "planning.read", "planning.work_order.read"),
            rule("POST", "work-orders", "planning.work_order.create"),
            rule("POST", "work-orders/\\d+/dispatch", "planning.work_order.dispatch"),
            rule("POST", "work-orders/\\d+/receive", "planning.work_order.receive"),

            any("GET", "materials(?:/.*)?", "warehouse.read", "warehouse.requisition.create", "master.read"),
            any("GET", "warehouses(?:/\\d+)?", "warehouse.read", "warehouse.requisition.create"),
            rule("GET", "(?:warehouses|picking-tasks|robots|robot-delivery-tasks)(?:/.*)?", "warehouse.read"),
            any("GET", "inventory(?:/.*)?", "warehouse.read", "warehouse.requisition.create"),
            any("GET", "requisitions(?:/.*)?", "warehouse.read", "warehouse.requisition.create"),
            rule("POST|PUT|DELETE", "(?:materials|warehouses|robots)(?:/.*)?", "warehouse.master.manage"),
            rule("POST|PUT|DELETE", "warehouses/locations(?:/.*)?", "warehouse.master.manage"),
            rule("POST", "inventory/external-purchase", "warehouse.inventory.adjust"),
            rule("POST|PUT|DELETE", "inventory(?:/.*)?", "warehouse.inventory.adjust"),
            rule("POST", "requisitions", "warehouse.requisition.create"),
            rule("POST", "requisitions/\\d+/receive", "warehouse.requisition.approve"),
            rule("POST", "requisitions/\\d+/(?:approve|reject)", "warehouse.requisition.approve"),
            rule("POST", "picking-tasks(?:/.*)?", "warehouse.picking.execute"),
            rule("POST", "robot-delivery-tasks/\\d+/confirm-receipt", "warehouse.requisition.create"),
            rule("POST", "robot-delivery-tasks(?:/.*)?", "warehouse.delivery.execute"),

            rule("GET", "work-reports(?:/.*)?", "production.read"),
            rule("POST", "work-reports", "production.report.create"),
            rule("PUT", "work-reports/\\d+", "production.report.update_own"),
            rule("DELETE", "work-reports/\\d+", "business.delete"),
            rule("POST", "work-reports/\\d+/(?:approve|reject)", "production.report.review"),
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
            rule("PUT", "equipment/\\d+/status", "equipment.manage"),
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
            rule("POST|PUT|DELETE", "products(?:/.*)?", "master.manage"),
            rule("GET", "product-boms(?:/.*)?", "master.read"),
            rule("POST|PUT|DELETE", "product-boms(?:/.*)?", "master.manage"),
            rule("GET", "production-lines(?:/.*)?", "master.read"),
            rule("POST|PUT|DELETE", "production-lines(?:/.*)?", "master.manage"),
            rule("GET", "process-routes(?:/.*)?", "process.read"),
            rule("POST|PUT|DELETE", "process-routes(?:/.*)?", "process.manage"),
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
            rule("POST", "access/system-maintenance/.*", "system.health.read"),
            any("GET", "access/permission-applications", "permission.apply", "permission.review", "role.manage"),
            rule("POST", "access/permission-applications", "permission.apply"),
            rule("POST", "access/permission-applications/\\d+/review", "permission.review"),
            rule("POST", "access/permission-applications/\\d+/apply", "role.manage"),
            any("GET", "access/account-applications", "permission.apply", "permission.review", "role.manage"),
            rule("POST", "access/account-applications", "permission.apply"),
            rule("POST", "access/account-applications/\\d+/review", "permission.review"),
            rule("GET", "access/users/\\d+/roles", "user.read"),
            rule("PUT", "access/users/\\d+/roles", "user.update_role"),
            rule("GET|PUT", "access/users/\\d+/data-scopes", "data_scope.manage")
    );

    /**
     * 内部实现步骤：执行 AuthorizationPolicy 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private AuthorizationPolicy() {
    }

    static Set<String> requiredPermissions(String method, String path) {
        return RULES.stream()
                .filter(rule -> rule.matches(method, path))
                .findFirst()
                .map(Rule::permissions)
                .orElse(Set.of());
    }

    /**
     * 内部实现步骤：执行 rule 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static Rule rule(String methods, String path, String permission) {
        return new Rule(Pattern.compile("^(?:" + methods + ")$"), Pattern.compile("^(?:" + path + ")$"), Set.of(permission));
    }

    /**
     * 内部实现步骤：执行 any 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static Rule any(String methods, String path, String... permissions) {
        return new Rule(Pattern.compile("^(?:" + methods + ")$"), Pattern.compile("^(?:" + path + ")$"), Set.of(permissions));
    }

    /**
     * 内部实现步骤：执行 Rule 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private record Rule(Pattern methods, Pattern path, Set<String> permissions) {
        boolean matches(String method, String requestPath) {
            return methods.matcher(method).matches() && path.matcher(requestPath).matches();
        }
    }
}
