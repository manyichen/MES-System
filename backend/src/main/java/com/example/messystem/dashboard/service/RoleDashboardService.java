package com.example.messystem.dashboard.service;

import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.dashboard.dao.RoleDashboardDao;
import com.example.messystem.dashboard.entity.RoleDashboard;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 根据角色策略元数据和 DAO 聚合结果构建角色首页。 */
public class RoleDashboardService {
    private final RoleDashboardDao dao = new RoleDashboardDao();

    public RoleDashboard build(AuthenticatedUser currentUser) {
        Profile profile = profileFor(primaryRole(currentUser));
        RoleDashboardDao.DashboardData data = dao.load(profile.roleCode, currentUser.user.userId);
        return new RoleDashboard(profile.roleCode, profile.roleName, profile.scope,
                profile.modules, profile.prohibited, data.metrics(), data.todos());
    }

    /** 多角色账号优先按系统管理员处理，否则使用认证结果中的主角色。 */
    private static String primaryRole(AuthenticatedUser user) {
        if (user.roles.contains("SYSTEM_ADMIN")) return "SYSTEM_ADMIN";
        return user.roles.stream().findFirst().orElse("UNASSIGNED");
    }

    /** 集中定义每个正式角色的数据范围、可见模块和禁止操作。 */
    private static Profile profileFor(String role) {
        return switch (role) {
            case "SYSTEM_ADMIN" -> profile(role, "系统管理员", "系统账号、角色权限、数据范围、会话与运行健康；不参与具体生产业务",
                    modules("dashboard", "systemOps", "audit", "system"),
                    "不能查看任何用户的明文密码", "不能绕过审计日志执行高风险操作", "不能排产、报工、改库存、审核质检或处理设备维修");
            case "HR_MANAGER" -> profile(role, "人事经理", "组织与账号范围；角色变更只能申请，不能直接授权",
                    modules("dashboard", "system"), "不能直接修改用户角色", "不能查看工艺配方、质量判定和库存明细", "不能操作生产、仓储或设备流程");
            case "GENERAL_MANAGER" -> profile(role, "总经理/管理层", "全厂经营汇总和异常下钻，只读业务数据",
                    modules("executiveOverview", "productionLive", "departmentReports", "managementAudit"),
                    "不能新增、修改或删除业务数据", "不能查看密码、权限配置和个人工资明细", "不能代替业务主管审批");
            case "PMC_PLANNER" -> profile(role, "PMC 计划员", "全厂订单、计划、齐套和相关工单范围",
                    modules("dashboard", "planning", "warehouse", "quality", "equipment", "trace", "feedback"),
                    "不能提交或审核生产报工", "不能修改库存", "不能审核质检结论或维修结果", "不能管理用户");
            case "WORKSHOP_MANAGER" -> profile(role, "车间管理员", "仅明确分配的产线，以及这些产线关联的工单、报工和设备数据",
                    modules("dashboard", "planning", "production", "equipment", "trace", "feedback"),
                    "不能创建客户订单或最终排产", "不能修改库存", "不能审核质检结果", "不能管理用户");
            case "PRODUCTION_OPERATOR" -> profile(role, "生产操作工", "本人、本人被派工单和本人报工/计件记录",
                    modules("dashboard", "planning", "production", "requisition", "equipment", "feedback"),
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
