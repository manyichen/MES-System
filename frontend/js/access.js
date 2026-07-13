let accessRoles = [];

async function loadAccessManagement() {
    if (!hasPermission("user.read")) return;
    try {
        const canViewApplications = hasPermission("permission.apply") || hasPermission("permission.review") || hasPermission("role.manage");
        const [users, roles, applications] = await Promise.all([
            getJson("/users"),
            hasPermission("role.read") ? getJson("/access/roles") : Promise.resolve([]),
            canViewApplications ? getJson("/access/permission-applications") : Promise.resolve([])
        ]);
        accessRoles = roles;
        renderPermissionApplyRoleOptions(roles);
        renderPermissionApplications(applications);
        renderUserRoleTable(users, roles);
        renderTable("role-table", roles, [
            { key: "roleCode", label: "角色代码", render: row => roleText(row.roleCode, row.roleName) },
            { key: "roleName", label: "角色名称" },
            { key: "dataScope", label: "数据范围", render: row => dataScopeText(row.dataScope) },
            { key: "permissionCount", label: "权限点" },
            { key: "userCount", label: "用户数" },
            { key: "description", label: "职责说明" }
        ], [
            { name: "view-role-permissions", label: "查看权限", idKey: "roleCode", permission: "role.read", handler: loadRolePermissions }
        ]);
        if (roles.length) await loadRolePermissions(roles[0].roleCode);
    } catch (error) {
        showMessage(error.message, "error");
    }
}

function renderPermissionApplyRoleOptions(roles) {
    const select = document.getElementById("permission-apply-role");
    if (!select) return;
    select.innerHTML = roles.map(role => `<option value="${escapeHtml(role.roleCode)}">${escapeHtml(roleText(role.roleCode, role.roleName))}</option>`).join("");
}

function renderPermissionApplications(rows) {
    renderTable("permission-apply-table", rows, [
        { key: "applyNo", label: "申请单" }, { key: "applicantId", label: "申请人ID" },
        { key: "targetUserId", label: "目标用户ID" }, { key: "fromRoleCode", label: "原角色", render: row => roleText(row.fromRoleCode) },
        { key: "toRoleCode", label: "申请角色", render: row => roleText(row.toRoleCode) }, { key: "applyReason", label: "原因" },
        { key: "applyStatus", label: "状态" }, { key: "reviewComment", label: "复核意见" }
    ], [
        { name: "review-application", label: "复核通过", idKey: "applyId", permission: "permission.review", handler: id => reviewPermissionApplication(id, "REVIEWED") },
        { name: "reject-application", label: "驳回", idKey: "applyId", permission: "permission.review", handler: id => reviewPermissionApplication(id, "REJECTED") },
        { name: "apply-application", label: "执行变更", idKey: "applyId", permission: "role.manage", handler: applyPermissionApplication }
    ]);
}

async function reviewPermissionApplication(id, decision) {
    const comment = window.prompt(decision === "REJECTED" ? "请输入驳回原因" : "请输入复核意见") || "";
    await postJson(`/access/permission-applications/${id}/review`, { decision, comment });
    showMessage(decision === "REJECTED" ? "申请已驳回" : "复核完成，等待系统管理员执行变更", "ok");
    await loadAccessManagement();
    await loadDashboard();
}

async function applyPermissionApplication(id) {
    await postJson(`/access/permission-applications/${id}/apply`);
    showMessage("申请角色已应用，目标用户需要重新登录", "ok");
    await loadAccessManagement();
    await loadDashboard();
}

function renderUserRoleTable(users, roles) {
    const container = document.getElementById("user-table");
    if (!container) return;
    const roleOptions = roles.map(role => `<option value="${escapeHtml(role.roleCode)}">${escapeHtml(roleText(role.roleCode, role.roleName))}</option>`).join("");
    const canUpdate = hasPermission("user.update_role");
    const canScope = hasPermission("data_scope.manage");
    container.innerHTML = `
        <table>
            <thead><tr><th>ID</th><th>用户名</th><th>姓名</th><th>部门</th><th>当前角色</th><th>状态</th>${canUpdate ? "<th>角色调整</th>" : ""}${canScope ? "<th>数据范围</th>" : ""}</tr></thead>
            <tbody>${users.map(user => `
                <tr>
                    <td>${escapeHtml(user.userId)}</td>
                    <td>${escapeHtml(user.username)}</td>
                    <td>${escapeHtml(user.realName)}</td>
                    <td>${escapeHtml(user.department || "")}</td>
                    <td>${escapeHtml(roleText(user.roleCode))}</td>
                    <td>${user.enabled === 1 ? "启用" : "停用"}</td>
                    ${canUpdate ? `<td><div class="role-editor"><select data-role-user="${escapeHtml(user.userId)}">${roleOptions}</select><button type="button" data-save-role="${escapeHtml(user.userId)}">保存</button></div></td>` : ""}
                    ${canScope ? `<td><button type="button" data-edit-scope="${escapeHtml(user.userId)}">分配产线/仓库</button></td>` : ""}
                </tr>`).join("")}
            </tbody>
        </table>`;
    users.forEach(user => {
        const select = container.querySelector(`[data-role-user="${user.userId}"]`);
        if (select) select.value = user.roleCode;
    });
    container.querySelectorAll("[data-save-role]").forEach(button => {
        button.addEventListener("click", async () => {
            const userId = button.dataset.saveRole;
            const roleCode = container.querySelector(`[data-role-user="${userId}"]`)?.value;
            try {
                await putJson(`/access/users/${userId}/roles`, { roleCodes: [roleCode] });
                showMessage("用户角色已更新，该用户需要重新登录", "ok");
                await loadAccessManagement();
            } catch (error) {
                showMessage(error.message, "error");
            }
        });
    });
    container.querySelectorAll("[data-edit-scope]").forEach(button => {
        button.addEventListener("click", () => editUserDataScopes(button.dataset.editScope));
    });
}

async function editUserDataScopes(userId) {
    try {
        const current = await getJson(`/access/users/${userId}/data-scopes`);
        const lineText = window.prompt("请输入允许访问的产线 ID，多个用英文逗号分隔；留空表示无产线权限。",
                (current.lineIds || []).join(","));
        if (lineText === null) return;
        const warehouseText = window.prompt("请输入允许访问的仓库 ID，多个用英文逗号分隔；留空表示无仓库权限。",
                (current.warehouseIds || []).join(","));
        if (warehouseText === null) return;
        const parseIds = value => value.split(",").map(item => Number(item.trim())).filter(item => Number.isInteger(item) && item > 0);
        await putJson(`/access/users/${userId}/data-scopes`, {
            lineIds: parseIds(lineText),
            warehouseIds: parseIds(warehouseText)
        });
        showMessage("用户数据范围已更新，重新登录后生效", "ok");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function loadRolePermissions(roleCode) {
    try {
        const permissions = await getJson(`/access/permissions?roleCode=${encodeURIComponent(roleCode)}`);
        renderTable("permission-table", permissions.filter(item => item.granted), [
            { key: "moduleCode", label: "模块", render: row => moduleText(row.moduleCode) },
            { key: "permissionCode", label: "权限项", render: row => permissionCodeText(row.permissionCode, row.permissionName) },
            { key: "permissionName", label: "权限名称" },
            { key: "actionCode", label: "动作", render: row => actionText(row.actionCode) },
            { key: "riskLevel", label: "风险级别", render: row => levelText(row.riskLevel) }
        ]);
    } catch (error) {
        showMessage(error.message, "error");
    }
}

function roleText(roleCode, fallbackName = "") {
    return {
        SYSTEM_ADMIN: "系统管理员",
        SYSTEM_MAINTAINER: "系统维护员",
        GENERAL_MANAGER: "总经理/管理层",
        HR_MANAGER: "人事经理",
        PMC_PLANNER: "PMC计划员",
        WORKSHOP_MANAGER: "车间管理员",
        WORKSHOP_OPERATOR: "车间操作工",
        PRODUCTION_OPERATOR: "生产操作工",
        QUALITY_MANAGER: "质量主管",
        QUALITY_INSPECTOR: "质检员",
        PROCESS_ENGINEER: "工艺工程师",
        WAREHOUSE_ADMIN: "仓库管理员",
        WAREHOUSE_KEEPER: "仓储人员",
        EQUIPMENT_ADMIN: "设备管理员",
        EQUIPMENT_MAINTAINER: "设备维护员"
    }[roleCode] || fallbackName || roleCode || "";
}

function dataScopeText(scope) {
    const key = String(scope || "").toUpperCase();
    return {
        ALL: "全部数据",
        DEPT: "本部门数据",
        OWN: "本人数据",
        CUSTOM: "自定义范围",
        LINE: "指定产线",
        WAREHOUSE: "指定仓库"
    }[key] || scope || "";
}

function moduleText(moduleCode) {
    const key = String(moduleCode || "").toLowerCase();
    return {
        dashboard: "首页看板",
        master: "基础数据",
        planning: "计划工单",
        production: "生产报工",
        warehouse: "仓储物流",
        quality: "质量管理",
        equipment: "设备维护",
        feedback: "管理反馈",
        process: "工艺管理",
        trace: "产品追溯",
        user: "用户管理",
        role: "角色管理",
        permission: "权限管理",
        system: "系统管理",
        audit: "审计日志",
        business: "业务数据",
        demo: "演示数据",
        data_scope: "数据范围"
    }[key] || moduleCode || "";
}

function actionText(actionCode) {
    const key = String(actionCode || "").toLowerCase();
    return {
        read: "查看",
        read_own: "查看本人",
        read_all: "查看全部",
        read_self: "查看本人",
        read_summary: "查看汇总",
        create: "创建",
        update: "修改",
        update_own: "修改本人",
        update_role: "修改角色",
        delete: "删除",
        adjust: "调整",
        manage: "管理",
        dispatch: "派发",
        receive: "接收",
        release: "发布",
        review: "审核",
        approve: "批准",
        reject: "驳回",
        execute: "执行",
        accept: "验收",
        assign: "派工",
        close: "关闭",
        report: "上报",
        seed: "生成",
        inspect: "检验",
        apply: "申请"
    }[key] || actionCode || "";
}

function permissionCodeText(permissionCode, fallbackName = "") {
    if (!permissionCode) return fallbackName || "";
    const parts = String(permissionCode).split(".");
    const module = moduleText(parts[0]);
    const action = actionText(parts[parts.length - 1]);
    const middle = parts.slice(1, -1).map(permissionPartText).filter(Boolean).join("、");
    if (module && middle && action) return `${module}-${middle}-${action}`;
    if (module && action) return `${module}-${action}`;
    return fallbackName || permissionCode;
}

function permissionPartText(part) {
    const key = String(part || "").toLowerCase();
    return {
        system: "系统",
        fault: "故障",
        maintenance: "维修",
        repair: "报修",
        order: "订单",
        task: "任务",
        work_order: "工单",
        work: "工单",
        report: "报工",
        role: "角色",
        user: "用户",
        data_scope: "数据范围",
        permission: "权限",
        requisition: "领料",
        picking: "拣货",
        delivery: "配送",
        inventory: "库存",
        trace: "追溯",
        feedback: "反馈",
        quality: "质量",
        inspection: "质检",
        rework: "返工",
        wage: "工资",
        salary: "工资",
        master: "主数据",
        health: "健康状态"
    }[key] || key.replaceAll("_", "");
}

document.getElementById("refresh-access")?.addEventListener("click", loadAccessManagement);
document.getElementById("permission-apply-form")?.addEventListener("submit", async event => {
    event.preventDefault();
    try {
        const payload = formToObject(event.target);
        await postJson("/access/permission-applications", payload);
        showMessage("权限申请已提交", "ok");
        event.target.reset();
        await loadAccessManagement();
        await loadDashboard();
    } catch (error) {
        showMessage(error.message, "error");
    }
});
