package com.example.messystem.dashboard.service;

import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.Db;
import com.example.messystem.dashboard.entity.RoleDashboard;
import com.example.messystem.dashboard.entity.RoleDashboard.DashboardCard;
import com.example.messystem.dashboard.entity.RoleDashboard.DashboardTodo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RoleDashboardService {
    public RoleDashboard build(AuthenticatedUser currentUser) {
        Profile profile = profileFor(primaryRole(currentUser));
        try (Connection connection = Db.getConnection()) {
            List<DashboardCard> metrics = metrics(connection, profile.roleCode, currentUser.user.userId);
            List<DashboardTodo> todos = todos(connection, profile.roleCode, currentUser.user.userId);
            addNotifications(connection, todos, currentUser, profile.roleCode);
            return new RoleDashboard(profile.roleCode, profile.roleName, profile.scope,
                    profile.modules, profile.prohibited, metrics, todos);
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    private static List<DashboardCard> metrics(Connection c, String role, long userId) throws SQLException {
        List<DashboardCard> cards = new ArrayList<>();
        switch (role) {
            case "SYSTEM_ADMIN" -> {
                card(cards, "users", "系统用户", count(c, "select count(*) from mes_user"), "人", "normal", "system");
                card(cards, "sessions", "当前有效会话", count(c, "select count(*) from mes_user_session where revoked_at is null and expires_at > current_timestamp"), "个", "normal", "systemOps");
                card(cards, "permission_apply", "待处理权限申请", count(c, "select count(*) from mes_permission_apply where apply_status = 'SUBMITTED'"), "项", "warning", "system");
                card(cards, "locked_users", "锁定账号", count(c, "select count(*) from mes_user where locked_until > current_timestamp"), "个", "warning", "systemOps");
            }
            case "HR_MANAGER" -> {
                card(cards, "users", "在册账号", count(c, "select count(*) from mes_user"), "人", "normal", "system");
                card(cards, "enabled_users", "启用账号", count(c, "select count(*) from mes_user where enabled = 1"), "人", "normal", "system");
                card(cards, "disabled_users", "停用账号", count(c, "select count(*) from mes_user where enabled <> 1"), "人", "warning", "system");
                card(cards, "permission_apply", "权限变更申请", count(c, "select count(*) from mes_permission_apply where apply_status = 'SUBMITTED'"), "项", "warning", "system");
            }
            case "GENERAL_MANAGER" -> {
                card(cards, "pending_orders", "待排产订单", count(c, "select count(*) from mes_customer_order where order_status = 'PENDING_PLAN'"), "单", "warning", "planning");
                card(cards, "active_work_orders", "执行中工单", count(c, "select count(*) from mes_work_order where work_order_status not in ('COMPLETED','CLOSED')"), "单", "normal", "planning");
                card(cards, "quality_risk", "质量异常/返工", count(c, "select count(*) from mes_rework_order where rework_status not in ('FINISHED','CLOSED')"), "单", "danger", "quality");
                card(cards, "equipment_fault", "故障设备", count(c, "select count(*) from mes_equipment where equipment_status in ('FAULT','REPAIRING','DOWN')"), "台", "danger", "equipment");
            }
            case "PMC_PLANNER" -> {
                card(cards, "pending_orders", "待排产订单", count(c, "select count(*) from mes_customer_order where order_status = 'PENDING_PLAN'"), "单", "warning", "planning");
                card(cards, "draft_tasks", "待发布生产任务", count(c, "select count(*) from mes_production_task where task_status = 'DRAFT'"), "项", "warning", "planning");
                card(cards, "shortages", "未解决缺料预警", count(c, "select count(*) from mes_shortage_alert where alert_status = 'OPEN'"), "项", "danger", "warehouse");
                card(cards, "rework", "待排返工单", count(c, "select count(*) from mes_rework_order where rework_status = 'CREATED'"), "单", "warning", "quality");
            }
            case "WORKSHOP_MANAGER" -> {
                card(cards, "active_work_orders", "本车间执行工单", count(c, "select count(*) from mes_work_order wo where wo.work_order_status in ('DISPATCHED','RECEIVED','IN_PROGRESS') and wo.line_id in (select line_id from mes_user_line_scope where user_id = ?)", userId), "单", "normal", "planning");
                card(cards, "report_review", "待审核报工", count(c, "select count(*) from mes_work_report r join mes_work_order wo on wo.work_order_id = r.work_order_id where r.report_status = 'SUBMITTED' and wo.line_id in (select line_id from mes_user_line_scope where user_id = ?)", userId), "单", "warning", "production");
                card(cards, "material_request", "待协调领料", count(c, "select count(*) from mes_material_requisition r join mes_work_order wo on wo.work_order_id = r.work_order_id where r.request_status in ('CREATED','APPROVED') and wo.line_id in (select line_id from mes_user_line_scope where user_id = ?)", userId), "单", "normal", "warehouse");
                card(cards, "equipment_fault", "影响生产的报修", count(c, "select count(*) from mes_equipment_repair_report r join mes_equipment e on e.equipment_id = r.equipment_id where r.repair_status = 'REPORTED' and e.line_id in (select line_id from mes_user_line_scope where user_id = ?)", userId), "单", "danger", "equipment");
            }
            case "PRODUCTION_OPERATOR" -> {
                card(cards, "my_work_orders", "我的未完工工单", count(c, "select count(*) from mes_work_order where (assigned_to = ? or accepted_by = ?) and work_order_status not in ('COMPLETED','CLOSED')", userId, userId), "单", "normal", "planning");
                card(cards, "my_reports", "我的报工记录", count(c, "select count(*) from mes_work_report where operator_id = ?", userId), "单", "normal", "production");
                card(cards, "my_pending_reports", "待审核报工", count(c, "select count(*) from mes_work_report where operator_id = ? and report_status = 'SUBMITTED'", userId), "单", "warning", "production");
                card(cards, "my_wage", "已生成计件记录", count(c, "select count(*) from mes_piecework_wage where operator_id = ?", userId), "条", "normal", "production");
            }
            case "WAREHOUSE_ADMIN" -> {
                card(cards, "requisitions", "待审批领料单", count(c, "select count(*) from mes_material_requisition r where r.request_status = 'CREATED' and r.warehouse_id in (select warehouse_id from mes_user_warehouse_scope where user_id = ?)", userId), "单", "warning", "warehouse");
                card(cards, "picking", "待完成拣货", count(c, "select count(*) from mes_picking_task p where p.task_status not in ('COMPLETED','CLOSED') and p.warehouse_id in (select warehouse_id from mes_user_warehouse_scope where user_id = ?)", userId), "项", "warning", "warehouse");
                card(cards, "delivery", "配送中任务", count(c, "select count(*) from mes_robot_delivery_task d join mes_picking_task p on p.picking_task_id = d.picking_task_id where d.delivery_status not in ('RECEIVED','COMPLETED','CLOSED') and p.warehouse_id in (select warehouse_id from mes_user_warehouse_scope where user_id = ?)", userId), "项", "normal", "warehouse");
                card(cards, "shortages", "本仓库存为零", count(c, "select count(*) from mes_inventory i where i.available_qty <= 0 and i.warehouse_id in (select warehouse_id from mes_user_warehouse_scope where user_id = ?)", userId), "项", "danger", "warehouse");
            }
            case "QUALITY_MANAGER" -> {
                card(cards, "quality_review", "待审核检验结果", count(c, "select count(*) from mes_quality_inspection where inspection_status = 'SUBMITTED'"), "单", "warning", "quality");
                card(cards, "quality_assign", "待分配质检单", count(c, "select count(*) from mes_quality_inspection where inspection_status = 'CREATED' and assigned_to is null"), "单", "warning", "quality");
                card(cards, "rework", "未闭环返工", count(c, "select count(*) from mes_rework_order where rework_status not in ('FINISHED','CLOSED')"), "单", "danger", "quality");
                card(cards, "defects", "累计不合格数量", count(c, "select coalesce(sum(defect_qty),0) from mes_work_report"), "件", "danger", "quality");
            }
            case "QUALITY_INSPECTOR" -> {
                card(cards, "my_inspections", "我的待检任务", count(c, "select count(*) from mes_quality_inspection where assigned_to = ? and inspection_status in ('CREATED','IN_PROGRESS')", userId), "单", "warning", "quality");
                card(cards, "submitted", "已提交待审核", count(c, "select count(*) from mes_quality_inspection where assigned_to = ? and inspection_status = 'SUBMITTED'", userId), "单", "normal", "quality");
                card(cards, "reviewed", "已完成质检", count(c, "select count(*) from mes_quality_inspection where assigned_to = ? and inspection_status in ('APPROVED','REJECTED','REVIEWED')", userId), "单", "normal", "quality");
                card(cards, "rework", "相关返工单", count(c, "select count(*) from mes_rework_order r join mes_quality_inspection q on q.inspection_id = r.inspection_id where q.assigned_to = ?", userId), "单", "warning", "quality");
            }
            case "PROCESS_ENGINEER" -> {
                card(cards, "draft_sop", "待发布 SOP", count(c, "select count(*) from mes_sop where sop_status = 'DRAFT'"), "份", "warning", "process");
                card(cards, "active_parameters", "有效工艺参数", count(c, "select count(*) from mes_process_parameter where enabled = 1"), "项", "normal", "process");
                card(cards, "defect_reasons", "启用不良原因", count(c, "select count(*) from mes_defect_reason where enabled = 1"), "项", "normal", "process");
                card(cards, "process_routes", "制造方法/工艺路线", count(c, "select count(*) from mes_process_route where enabled = 1"), "项", "normal", "process");
            }
            case "EQUIPMENT_ADMIN" -> {
                card(cards, "faults", "待核实报修", count(c, "select count(*) from mes_equipment_repair_report where repair_status = 'REPORTED'"), "单", "danger", "equipment");
                card(cards, "unassigned", "待派维修工单", count(c, "select count(*) from mes_maintenance_order where maintenance_status = 'CREATED'"), "单", "warning", "equipment");
                card(cards, "acceptance", "待验收维修工单", count(c, "select count(*) from mes_maintenance_order where maintenance_status = 'FINISHED'"), "单", "warning", "equipment");
                card(cards, "fault_equipment", "故障/维修设备", count(c, "select count(*) from mes_equipment where equipment_status in ('FAULT','REPAIRING','DOWN')"), "台", "danger", "equipment");
            }
            case "EQUIPMENT_MAINTAINER" -> {
                card(cards, "my_orders", "我的待维修工单", count(c, "select count(*) from mes_maintenance_order where maintainer_id = ? and maintenance_status in ('ASSIGNED','IN_PROGRESS')", userId), "单", "warning", "equipment");
                card(cards, "finished", "我的待验收工单", count(c, "select count(*) from mes_maintenance_order where maintainer_id = ? and maintenance_status = 'FINISHED'", userId), "单", "normal", "equipment");
                card(cards, "plans", "近期维护计划", count(c, "select count(*) from mes_maintenance_plan where plan_status = 'ACTIVE' and next_plan_time <= current_timestamp + interval '7 days'"), "项", "warning", "equipment");
                card(cards, "faults", "未转维修报修", count(c, "select count(*) from mes_equipment_repair_report where repair_status in ('REPORTED','APPROVED')"), "单", "normal", "equipment");
            }
            default -> {
                card(cards, "orders", "订单概览", count(c, "select count(*) from mes_customer_order"), "单", "normal", "dashboard");
                card(cards, "work_orders", "生产工单", count(c, "select count(*) from mes_work_order"), "单", "normal", "dashboard");
                card(cards, "quality", "质检记录", count(c, "select count(*) from mes_quality_inspection"), "单", "normal", "dashboard");
                card(cards, "equipment", "设备数量", count(c, "select count(*) from mes_equipment where enabled = 1"), "台", "normal", "dashboard");
            }
        }
        return cards;
    }

    private static List<DashboardTodo> todos(Connection c, String role, long userId) throws SQLException {
        List<DashboardTodo> todos = new ArrayList<>();
        switch (role) {
            case "SYSTEM_ADMIN" -> {
                todo(todos, "permission-review", "审批权限变更申请", "核对申请人、目标用户、原角色与申请角色后处理。",
                        count(c, "select count(*) from mes_permission_apply where apply_status = 'SUBMITTED'"), "HIGH", "system", "permission-apply-table", "permission.review");
                todo(todos, "login-risk", "处理异常登录", "检查连续失败或已锁定账号，必要时联系用户并重置。",
                        count(c, "select count(*) from mes_user where locked_until > current_timestamp"), "HIGH", "systemOps", "locked-user-table", "system.health.read");
            }
            case "HR_MANAGER" -> todo(todos, "permission-apply", "补充并提交权限申请", "为岗位变化人员发起角色变更，系统管理员审批后生效。",
                    count(c, "select count(*) from mes_permission_apply where applicant_id = ? and apply_status in ('DRAFT','RETURNED')", userId), "MEDIUM", "system", "permission-apply-form", "permission.apply");
            case "GENERAL_MANAGER" -> todo(todos, "management-feedback", "督办未闭环管理反馈", "查看跨部门异常、处理意见与闭环进度。",
                    count(c, "select count(*) from mes_management_feedback where feedback_status = 'OPEN'"), "HIGH", "feedback", "feedback-table", "feedback.read");
            case "PMC_PLANNER" -> {
                todo(todos, "plan-orders", "为待排产订单创建生产任务", "确认交期、数量和目标产线后完成排产。",
                        count(c, "select count(*) from mes_customer_order where order_status = 'PENDING_PLAN'"), "HIGH", "planning", "orderTable", "planning.task.create");
                todo(todos, "shortage", "处理齐套与缺料预警", "分析受影响订单，协调仓库或调整排产。",
                        count(c, "select count(*) from mes_shortage_alert where alert_status = 'OPEN'"), "HIGH", "planning", "taskTable", "planning.task.release");
                todo(todos, "rework-plan", "安排质量返工", "将质量部门确认的返工需求纳入生产计划。",
                        count(c, "select count(*) from mes_rework_order where rework_status = 'CREATED'"), "MEDIUM", "quality", "rework-table", "planning.work_order.create");
            }
            case "WORKSHOP_MANAGER" -> {
                todo(todos, "report-review", "审核操作工报工单", "核对工单、批次、合格数、不良数和工时后审批。",
                        count(c, "select count(*) from mes_work_report r join mes_work_order wo on wo.work_order_id = r.work_order_id where r.report_status = 'SUBMITTED' and wo.line_id in (select line_id from mes_user_line_scope where user_id = ?)", userId), "HIGH", "production", "reportTable", "production.report.review");
                todo(todos, "dispatch", "派发待执行制造工单", "根据产线负荷和人员安排派发工单。",
                        count(c, "select count(*) from mes_work_order wo where wo.work_order_status = 'CREATED' and wo.line_id in (select line_id from mes_user_line_scope where user_id = ?)", userId), "HIGH", "planning", "workOrderTable", "planning.work_order.dispatch");
            }
            case "PRODUCTION_OPERATOR" -> {
                todo(todos, "receive-work-order", "接收派发给我的工单", "确认工位、批次和生产要求后接单。",
                        count(c, "select count(*) from mes_work_order where assigned_to = ? and accepted_by is null and work_order_status = 'DISPATCHED'", userId), "HIGH", "planning", "workOrderTable", "planning.work_order.receive");
                todo(todos, "submit-report", "为已接工单提交报工", "完成生产后录入产量、合格数、不良数和工时。",
                        count(c, "select count(*) from mes_work_order where accepted_by = ? and work_order_status in ('RECEIVED','IN_PROGRESS')", userId), "HIGH", "production", "reportForm", "production.report.create");
            }
            case "WAREHOUSE_ADMIN" -> {
                todo(todos, "approve-requisition", "审核生产领料申请", "核对工单、物料、需求数量和可用库存。",
                        count(c, "select count(*) from mes_material_requisition r where r.request_status = 'CREATED' and r.warehouse_id in (select warehouse_id from mes_user_warehouse_scope where user_id = ?)", userId), "HIGH", "warehouse", "requisitionTable", "warehouse.requisition.approve");
                todo(todos, "picking", "完成待拣货任务", "按库位和批次完成拣货并生成库存流水。",
                        count(c, "select count(*) from mes_picking_task p where p.task_status = 'CREATED' and p.warehouse_id in (select warehouse_id from mes_user_warehouse_scope where user_id = ?)", userId), "HIGH", "warehouse", "pickingTable", "warehouse.picking.execute");
                todo(todos, "delivery", "跟踪待交接配送任务", "确认到达和生产线收料，完成配送闭环。",
                        count(c, "select count(*) from mes_robot_delivery_task d join mes_picking_task p on p.picking_task_id = d.picking_task_id where d.delivery_status not in ('RECEIVED','COMPLETED','CLOSED') and p.warehouse_id in (select warehouse_id from mes_user_warehouse_scope where user_id = ?)", userId), "MEDIUM", "warehouse", "deliveryTable", "warehouse.delivery.execute");
            }
            case "QUALITY_MANAGER" -> {
                todo(todos, "quality-review", "审核产品检验结果", "检查检验项目、实测值和质检员结论，决定放行、复检或返工。",
                        count(c, "select count(*) from mes_quality_inspection where inspection_status = 'SUBMITTED'"), "HIGH", "quality", "quality-table", "quality.review");
                todo(todos, "quality-assign", "分配未指派质检任务", "根据产品、批次和工作量指定质检员。",
                        count(c, "select count(*) from mes_quality_inspection where inspection_status = 'CREATED' and assigned_to is null"), "HIGH", "quality", "quality-table", "quality.inspection.assign");
            }
            case "QUALITY_INSPECTOR" -> todo(todos, "my-inspection", "完成分配给我的质检任务", "录入全部检验项目后提交质量主管审核，不得自行最终放行。",
                    count(c, "select count(*) from mes_quality_inspection where assigned_to = ? and inspection_status in ('CREATED','IN_PROGRESS')", userId), "HIGH", "quality", "quality-table", "quality.inspect");
            case "PROCESS_ENGINEER" -> {
                todo(todos, "sop-publish", "复核待发布 SOP", "确认版本、适用产品和关键工艺参数后发布。",
                        count(c, "select count(*) from mes_sop where sop_status = 'DRAFT'"), "HIGH", "process", "process-route-table", "process.manage");
                todo(todos, "route-maintain", "维护轮胎制造方法", "确认工序顺序、设备/工作中心和启停状态。",
                        count(c, "select count(*) from mes_process_route where enabled = 1"), "MEDIUM", "process", "process-route-table", "process.manage");
            }
            case "EQUIPMENT_ADMIN" -> {
                todo(todos, "repair-review", "审核设备故障上报", "核实故障等级、生产影响和描述后决定是否转维修。",
                        count(c, "select count(*) from mes_equipment_repair_report where repair_status = 'REPORTED'"), "HIGH", "equipment", "repair-table", "equipment.repair.review");
                todo(todos, "maintenance-assign", "派发维修工单", "选择维护员并明确维修要求和优先级。",
                        count(c, "select count(*) from mes_maintenance_order where maintenance_status = 'CREATED'"), "HIGH", "equipment", "maintenance-table", "equipment.maintenance.assign");
                todo(todos, "maintenance-accept", "验收已完成维修", "核对维修结果并确认设备是否恢复运行。",
                        count(c, "select count(*) from mes_maintenance_order where maintenance_status = 'FINISHED'"), "HIGH", "equipment", "maintenance-table", "equipment.maintenance.accept");
            }
            case "EQUIPMENT_MAINTAINER" -> todo(todos, "maintenance-execute", "处理分配给我的维修工单", "执行维修并记录故障原因、措施、耗时和结果。",
                    count(c, "select count(*) from mes_maintenance_order where maintainer_id = ? and maintenance_status in ('ASSIGNED','IN_PROGRESS')", userId), "HIGH", "equipment", "maintenance-table", "equipment.maintenance.execute");
            default -> {
            }
        }
        return todos;
    }

    private static void addNotifications(Connection c, List<DashboardTodo> todos, AuthenticatedUser user, String role)
            throws SQLException {
        long count = count(c, """
                select count(*) from mes_notification
                where read_status = 'UNREAD' and (receiver_user_id = ? or receiver_role_code = ?)
                """, user.user.userId, role);
        todo(todos, "notifications", "未读业务通知", "查看系统发送的缺料、审核、质检和维修提醒。",
                count, "MEDIUM", "dashboard", "dashboard-todos", "dashboard.read");
    }

    private static void card(List<DashboardCard> cards, String code, String label, long value,
            String unit, String level, String targetTab) {
        cards.add(new DashboardCard(code, label, String.valueOf(value), unit, level, targetTab));
    }

    private static void todo(List<DashboardTodo> todos, String code, String title, String description,
            long count, String priority, String targetTab, String targetAnchor, String permission) {
        if (count > 0) todos.add(new DashboardTodo(code, title, description, count, priority,
                targetTab, targetAnchor, permission));
    }

    private static long count(Connection connection, String sql, Object... args) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static String primaryRole(AuthenticatedUser user) {
        if (user.roles.contains("SYSTEM_ADMIN")) return "SYSTEM_ADMIN";
        return user.roles.stream().findFirst().orElse("UNASSIGNED");
    }

    private static Profile profileFor(String role) {
        return switch (role) {
            case "SYSTEM_ADMIN" -> profile(role, "系统管理员", "系统账号、角色权限、数据范围、会话与运行健康；不参与具体生产业务",
                    modules("dashboard", "systemOps", "audit", "system"),
                    "不能查看任何用户的明文密码", "不能绕过审计日志执行高风险操作", "不能排产、报工、改库存、审核质检或处理设备维修");
            case "HR_MANAGER" -> profile(role, "人事经理", "组织与账号范围；角色变更只能申请，不能直接授权",
                    modules("dashboard", "system"), "不能直接修改用户角色", "不能查看工艺配方、质量判定和库存明细", "不能操作生产、仓储或设备流程");
            case "GENERAL_MANAGER" -> profile(role, "总经理/管理层", "全厂经营汇总和异常下钻，只读业务数据",
                    modules("dashboard", "planning", "warehouse", "production", "quality", "equipment", "trace", "feedback"),
                    "不能新增、修改或删除业务数据", "不能查看密码、权限配置和个人工资明细", "不能代替业务主管审批");
            case "PMC_PLANNER" -> profile(role, "PMC 计划员", "全厂订单、计划、齐套和相关工单范围",
                    modules("dashboard", "planning", "warehouse", "quality", "equipment", "trace", "feedback"),
                    "不能提交或审核生产报工", "不能修改库存", "不能审核质检结论或维修结果", "不能管理用户");
            case "WORKSHOP_MANAGER" -> profile(role, "车间管理员", "仅明确分配的产线，以及这些产线关联的工单、报工和设备数据",
                    modules("dashboard", "planning", "warehouse", "production", "equipment", "trace", "feedback"),
                    "不能创建客户订单或最终排产", "不能修改库存", "不能审核质检结果", "不能管理用户");
            case "PRODUCTION_OPERATOR" -> profile(role, "生产操作工", "本人、本人被派工单和本人报工/计件记录",
                    modules("dashboard", "planning", "production", "equipment"),
                    "不能查看其他员工报工和工资", "不能派发工单或审核报工", "不能修改库存、质检结论和设备台账", "不能查看用户信息");
            case "WAREHOUSE_ADMIN" -> profile(role, "仓库管理员", "仅明确分配的仓库、库位、库存、领料、拣货、机器人和配送数据",
                    modules("dashboard", "planning", "warehouse", "trace", "feedback"),
                    "不能创建生产计划或报工", "不能审核质检和维修", "不能查看个人工资或用户权限");
            case "QUALITY_MANAGER" -> profile(role, "质量主管", "全厂质量数据、相关工单批次和追溯信息",
                    modules("dashboard", "planning", "quality", "trace", "feedback"),
                    "不能代替质检员录入其检验数据", "不能提交生产报工或修改库存", "不能修改用户权限", "不能发布工艺参数");
            case "QUALITY_INSPECTOR" -> profile(role, "质检员", "仅本人被分配的质检任务及相关批次追溯",
                    modules("dashboard", "quality", "trace", "feedback"),
                    "不能审核或最终放行自己的检验结果", "不能维护质检标准", "不能查看其他质检员任务", "不能修改生产、库存和用户数据");
            case "PROCESS_ENGINEER" -> profile(role, "工艺工程师", "工艺路线、SOP、产品和原料主数据",
                    modules("dashboard", "process", "trace", "feedback"),
                    "不能审核质检放行", "不能提交或审核报工", "不能修改库存或维修状态", "不能管理用户");
            case "EQUIPMENT_ADMIN" -> profile(role, "设备管理员", "全厂设备、维修和维护计划数据",
                    modules("dashboard", "planning", "equipment", "trace", "feedback"),
                    "不能提交生产报工或修改库存", "不能审核质检结论", "不能创建生产计划", "不能管理用户");
            case "EQUIPMENT_MAINTAINER" -> profile(role, "设备维护员", "本人被分配的维修工单和相关设备",
                    modules("dashboard", "equipment", "feedback"),
                    "不能给自己派工或验收自己的维修", "不能修改设备基础台账", "不能操作生产、库存、质量和用户数据");
            default -> profile("UNASSIGNED", "未配置角色", "当前账号未配置业务角色，请联系系统管理员分配岗位权限。",
                    modules("dashboard"), "不能进入业务模块", "不能新增、修改、审批或删除任何数据");
        };
    }

    private static Profile profile(String roleCode, String roleName, String scope, Set<String> modules,
            String... prohibited) {
        return new Profile(roleCode, roleName, scope, modules, List.of(prohibited));
    }

    private static Set<String> modules(String... values) {
        return new LinkedHashSet<>(List.of(values));
    }

    private record Profile(String roleCode, String roleName, String scope, Set<String> modules,
            List<String> prohibited) {
    }
}
