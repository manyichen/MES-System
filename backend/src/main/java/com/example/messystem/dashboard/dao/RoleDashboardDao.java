package com.example.messystem.dashboard.dao;

import com.example.messystem.common.Db;
import com.example.messystem.dashboard.entity.RoleDashboard.DashboardCard;
import com.example.messystem.dashboard.entity.RoleDashboard.DashboardTodo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** 执行角色首页所需的分角色聚合查询。 */
public class RoleDashboardDao {
    public DashboardData load(String role, long userId) {
        try (Connection connection = Db.getConnection()) {
            List<DashboardCard> metrics = metrics(connection, role, userId);
            List<DashboardTodo> todos = todos(connection, role, userId);
            addNotifications(connection, todos, userId, role);
            return new DashboardData(metrics, todos);
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    private static List<DashboardCard> metrics(Connection c, String role, long userId) throws SQLException {
        List<DashboardCard> cards = new ArrayList<>();
        switch (role) {
            case "SUPER_ADMIN" -> {
                card(cards, "users", "\u7cfb\u7edf\u7528\u6237", count(c, "select count(*) from mes_user"), "\u4eba", "normal", "system");
                card(cards, "active_work_orders", "\u6267\u884c\u4e2d\u5de5\u5355", count(c, "select count(*) from mes_work_order where work_order_status not in ('FINISHED','COMPLETED','CLOSED','CANCELLED')"), "\u5f20", "normal", "planning");
                card(cards, "requisitions", "\u5f85\u5904\u7406\u9886\u6599\u5355", count(c, "select count(*) from mes_material_requisition where request_status in ('CREATED','RECEIVED')"), "\u5f20", "warning", "warehouse");
                card(cards, "quality_review", "\u5f85\u5ba1\u6838\u8d28\u68c0", count(c, "select count(*) from mes_quality_inspection where inspection_status = 'SUBMITTED'"), "\u5f20", "warning", "quality");
                card(cards, "equipment_fault", "\u6545\u969c\u8bbe\u5907", count(c, "select count(*) from mes_equipment where equipment_status in ('FAULT','REPAIRING','DOWN')"), "\u53f0", "danger", "equipment");
                card(cards, "permission_apply", "\u5f85\u5904\u7406\u6743\u9650\u7533\u8bf7", count(c, "select count(*) from mes_permission_apply where apply_status = 'SUBMITTED'"), "\u9879", "warning", "system");
            }
            case "SYSTEM_ADMIN" -> {
                card(cards, "users", "系统用户", count(c, "select count(*) from mes_user"), "人", "normal", "system");
                card(cards, "sessions", "当前有效会话", count(c, "select count(*) from mes_user_session where revoked_at is null and expires_at > current_timestamp"), "个", "normal", "systemOps");
                card(cards, "permission_apply", "待处理权限申请", count(c, "select count(*) from mes_permission_apply where apply_status = 'SUBMITTED'"), "项", "warning", "system");
                card(cards, "account_apply", "待审核账号申请", countOptional(c,
                        "select count(*) from mes_account_apply where apply_status = 'SUBMITTED'"), "项", "warning", "system");
            }
            case "HR_MANAGER" -> {
                card(cards, "users", "在册账号", count(c, "select count(*) from mes_user"), "人", "normal", "system");
                card(cards, "enabled_users", "启用账号", count(c, "select count(*) from mes_user where enabled = 1"), "人", "normal", "system");
                card(cards, "disabled_users", "停用账号", count(c, "select count(*) from mes_user where enabled <> 1"), "人", "warning", "system");
                card(cards, "account_apply", "账号申请", countOptional(c,
                        "select count(*) from mes_account_apply where applicant_id = ? and apply_status = 'SUBMITTED'", userId), "项", "warning", "system");
            }
            case "GENERAL_MANAGER" -> {
                card(cards, "pending_orders", "待排产订单", count(c, "select count(*) from mes_customer_order where order_status = 'PENDING_PLAN'"), "单", "warning", "planning");
                card(cards, "active_work_orders", "执行中工单", count(c, "select count(*) from mes_work_order where work_order_status not in ('FINISHED','COMPLETED','CLOSED','CANCELLED')"), "单", "normal", "planning");
                card(cards, "quality_risk", "质量异常/返工", count(c, "select count(*) from mes_rework_order where rework_status not in ('FINISHED','CLOSED')"), "单", "danger", "quality");
                card(cards, "equipment_fault", "故障设备", count(c, "select count(*) from mes_equipment where equipment_status in ('FAULT','REPAIRING','DOWN')"), "台", "danger", "equipment");
            }
            case "PMC_PLANNER" -> {
                card(cards, "pending_orders", "待排产订单", count(c, "select count(*) from mes_customer_order where order_status = 'PENDING_PLAN'"), "单", "warning", "planning");
                card(cards, "pending_tasks", "待齐套生产任务", count(c, "select count(*) from mes_production_task where task_status in ('CREATED','SHORTAGE')"), "项", "warning", "planning");
                card(cards, "ready_tasks", "待生成工单任务", count(c, "select count(*) from mes_production_task where task_status = 'READY' and kitting_status = 'READY'"), "项", "normal", "planning");
                card(cards, "shortages", "未解决缺料预警", count(c, "select count(*) from mes_shortage_alert where alert_status in ('OPEN','ACCEPTED')"), "项", "danger", "planning");
                card(cards, "active_work_orders", "执行中生产工单", count(c, "select count(*) from mes_work_order where work_order_status not in ('FINISHED','COMPLETED','CLOSED','CANCELLED')"), "单", "normal", "planning");
                card(cards, "rework", "待排返工需求", count(c, "select count(*) from mes_rework_order where rework_status = 'CREATED'"), "单", "warning", "planning");
            }
            case "WORKSHOP_MANAGER" -> {
                card(cards, "active_work_orders", "本车间执行工单", count(c, "select count(*) from mes_work_order wo where wo.work_order_status in ('DISPATCHED','RECEIVED','IN_PROGRESS') and wo.line_id in (select line_id from mes_user_line_scope where user_id = ?)", userId), "单", "normal", "planning");
                card(cards, "report_review", "待审核报工", count(c, "select count(*) from mes_work_report r join mes_work_order wo on wo.work_order_id = r.work_order_id where r.report_status = 'SUBMITTED' and wo.line_id in (select line_id from mes_user_line_scope where user_id = ?)", userId), "单", "warning", "production");
                card(cards, "material_request", "待协调领料", count(c, "select count(*) from mes_material_requisition r join mes_work_order wo on wo.work_order_id = r.work_order_id where r.request_status in ('CREATED','RECEIVED','APPROVED') and wo.line_id in (select line_id from mes_user_line_scope where user_id = ?)", userId), "单", "normal", "warehouse");
                card(cards, "equipment_fault", "影响生产的报修", count(c, "select count(*) from mes_equipment_repair_report r join mes_equipment e on e.equipment_id = r.equipment_id where r.repair_status = 'REPORTED' and e.line_id in (select line_id from mes_user_line_scope where user_id = ?)", userId), "单", "danger", "equipment");
            }
            case "PRODUCTION_OPERATOR" -> {
                card(cards, "my_work_orders", "我的未完工工单", count(c, "select count(*) from mes_work_order where (assigned_to = ? or accepted_by = ?) and work_order_status not in ('COMPLETED','CLOSED')", userId, userId), "单", "normal", "planning");
                card(cards, "my_reports", "我的报工记录", count(c, "select count(*) from mes_work_report where operator_id = ?", userId), "单", "normal", "production");
                card(cards, "my_pending_reports", "待审核报工", count(c, "select count(*) from mes_work_report where operator_id = ? and report_status = 'SUBMITTED'", userId), "单", "warning", "production");
                card(cards, "my_wage", "已生成计件记录", count(c, "select count(*) from mes_piecework_wage where operator_id = ?", userId), "条", "normal", "production");
            }
            case "WAREHOUSE_ADMIN" -> {
                card(cards, "requisitions", "待接收/审批领料单", count(c, "select count(*) from mes_material_requisition r where r.request_status in ('CREATED','RECEIVED') and r.warehouse_id in (select warehouse_id from mes_user_warehouse_scope where user_id = ?)", userId), "单", "warning", "warehouse");
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
            case "SUPER_ADMIN" -> {
                todo(todos, "account-review", "\u5ba1\u6838\u8d26\u53f7\u7533\u8bf7", "\u5904\u7406\u5f85\u5ba1\u6838\u7684\u65b0\u8d26\u53f7\u7533\u8bf7\u3002",
                        countOptional(c, "select count(*) from mes_account_apply where apply_status = 'SUBMITTED'"), "HIGH", "system", "accountApplications", "permission.review");
                todo(todos, "permission-review", "\u5ba1\u6279\u6743\u9650\u53d8\u66f4", "\u5904\u7406\u5f85\u5ba1\u6279\u7684\u6743\u9650\u53d8\u66f4\u7533\u8bf7\u3002",
                        count(c, "select count(*) from mes_permission_apply where apply_status = 'SUBMITTED'"), "HIGH", "system", "permission-apply-table", "permission.review");
                todo(todos, "quality-review", "\u5ba1\u6838\u8d28\u68c0\u7ed3\u679c", "\u5904\u7406\u5f85\u5ba1\u6838\u7684\u4ea7\u54c1\u68c0\u9a8c\u3002",
                        count(c, "select count(*) from mes_quality_inspection where inspection_status = 'SUBMITTED'"), "HIGH", "quality", "quality-table", "quality.review");
                todo(todos, "repair-review", "\u5904\u7406\u8bbe\u5907\u6545\u969c", "\u5ba1\u6838\u5f85\u5904\u7406\u7684\u8bbe\u5907\u6545\u969c\u4e0a\u62a5\u3002",
                        count(c, "select count(*) from mes_equipment_repair_report where repair_status = 'REPORTED'"), "HIGH", "equipment", "repair-table", "equipment.repair.review");
            }
            case "SYSTEM_ADMIN" -> {
                todo(todos, "account-review", "审核账号申请", "核对新账号姓名、角色、部门与申请说明后决定通过或拒绝。",
                        countOptional(c, "select count(*) from mes_account_apply where apply_status = 'SUBMITTED'"), "HIGH", "system", "accountApplications", "permission.review");
                todo(todos, "permission-review", "审批权限变更申请", "核对申请人、目标用户、原角色与申请角色后处理。",
                        count(c, "select count(*) from mes_permission_apply where apply_status = 'SUBMITTED'"), "HIGH", "system", "permission-apply-table", "permission.review");
                todo(todos, "login-risk", "处理异常登录", "检查连续失败或已锁定账号，必要时联系用户并重置。",
                        count(c, "select count(*) from mes_user where locked_until > current_timestamp"), "HIGH", "systemOps", "locked-user-table", "system.health.read");
            }
            case "HR_MANAGER" -> todo(todos, "account-apply", "发起账号申请", "为新员工填写账号申请单，系统管理员审核通过后账号生效。",
                    countOptional(c, "select count(*) from mes_account_apply where applicant_id = ? and apply_status = 'SUBMITTED'", userId), "MEDIUM", "system", "accountApplications", "permission.apply");
            case "GENERAL_MANAGER" -> todo(todos, "management-feedback", "督办未闭环管理反馈", "查看跨部门异常、处理意见与闭环进度。",
                    count(c, "select count(*) from mes_management_feedback where feedback_status = 'OPEN'"), "HIGH", "feedback", "feedback-table", "feedback.read");
            case "PMC_PLANNER" -> {
                todo(todos, "plan-orders", "为待排产订单创建生产任务", "确认交期、数量和目标产线后完成排产。",
                        count(c, "select count(*) from mes_customer_order where order_status = 'PENDING_PLAN'"), "HIGH", "planning", "orderTable", "planning.task.create");
                todo(todos, "shortage", "处理齐套与缺料预警", "分析受影响订单，协调仓库或调整排产。",
                        count(c, "select count(*) from mes_shortage_alert where alert_status in ('OPEN','ACCEPTED')"), "HIGH", "planning", "tasks", "planning.task.release");
                todo(todos, "rework-plan", "安排质量返工需求", "将质量部门已经确认的返工需求生成返工生产任务。",
                        count(c, "select count(*) from mes_rework_order where rework_status = 'CREATED'"), "MEDIUM", "planning", "reworks", "planning.rework.plan");
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
                        count(c, "select count(*) from mes_material_requisition r where r.request_status in ('CREATED','RECEIVED') and r.warehouse_id in (select warehouse_id from mes_user_warehouse_scope where user_id = ?)", userId), "HIGH", "warehouse", "requisitionTable", "warehouse.requisition.approve");
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

    private static void addNotifications(Connection c, List<DashboardTodo> todos, long userId, String role)
            throws SQLException {
        long count = count(c, """
                select count(*) from mes_notification
                where read_status = 'UNREAD' and (receiver_user_id = ? or receiver_role_code = ?)
                """, userId, role);
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

    /** 新增迁移尚未执行时，首页保持可用但不伪造待办。 */
    private static long countOptional(Connection connection, String sql, Object... args) throws SQLException {
        try {
            return count(connection, sql, args);
        } catch (SQLException ex) {
            return 0;
        }
    }

    public record DashboardData(List<DashboardCard> metrics, List<DashboardTodo> todos) {
    }
}
