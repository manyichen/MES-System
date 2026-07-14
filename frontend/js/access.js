let accessRoles = [];
let accessUsers = [];
let accessMaintenanceSummary = {};

async function loadAccessManagement() {
    const canViewUsers = hasPermission("user.read");
    const canViewRoles = hasPermission("role.read");
    const canViewApplications = hasPermission("permission.apply") || hasPermission("permission.review") || hasPermission("role.manage");
    const canViewMaintenance = hasPermission("system.health.read") || hasPermission("audit.read");
    if (!canViewUsers && !canViewRoles && !canViewApplications && !canViewMaintenance) return;
    try {
        const results = await Promise.allSettled([
            canViewUsers ? getJson("/users") : Promise.resolve([]),
            canViewRoles ? getJson("/access/roles") : Promise.resolve([]),
            canViewApplications ? getJson("/access/permission-applications") : Promise.resolve([]),
            canViewMaintenance ? getJson("/access/system-maintenance") : Promise.resolve(null)
        ]);
        const [usersResult, rolesResult, applicationsResult, maintenanceResult] = results;
        const users = usersResult.status === "fulfilled" && Array.isArray(usersResult.value) ? usersResult.value : [];
        const roles = rolesResult.status === "fulfilled" && Array.isArray(rolesResult.value) ? rolesResult.value : [];
        const applications = applicationsResult.status === "fulfilled" && Array.isArray(applicationsResult.value) ? applicationsResult.value : [];
        const maintenance = maintenanceResult.status === "fulfilled" ? maintenanceResult.value : {};
        results
            .filter(result => result.status === "rejected")
            .forEach(result => showMessage(result.reason?.message || "数据加载失败", "error"));
        accessRoles = roles;
        accessUsers = users;
        accessMaintenanceSummary = maintenance;
        renderSystemMaintenance(maintenance);
        if (canViewUsers) renderUserRoleTable(users, roles);
        if (canViewApplications) renderPermissionApplications(applications);
        if (canViewRoles) renderTable("role-table", roles, [
            { key: "roleCode", label: "角色代码", render: row => roleText(row.roleCode, row.roleName) },
            { key: "roleName", label: "角色名称" },
            { key: "dataScope", label: "数据范围", render: row => dataScopeText(row.dataScope) },
            { key: "permissionCount", label: "权限点" },
            { key: "userCount", label: "用户数" },
            { key: "description", label: "职责说明" }
        ], [
            { name: "view-role-permissions", label: "查看权限", idKey: "roleCode", permission: "role.read", handler: loadRolePermissions }
        ]);
    } catch (error) {
        showMessage(error.message, "error");
    }
}

function renderUserRoleTable(users, roles) {
    const container = document.getElementById("user-table");
    if (!container) return;
    const canUpdate = hasPermission("user.update_role") && !hasRole("SYSTEM_ADMIN");
    const canApply = hasPermission("permission.apply");
    const canAdjust = canUpdate || canApply;
    container.innerHTML = `
        <table>
            <thead><tr><th>ID</th><th>用户名</th><th>姓名</th><th>部门</th><th>当前角色</th><th>状态</th>${canAdjust ? "<th>操作</th>" : ""}</tr></thead>
            <tbody>${users.map(user => `
                <tr>
                    <td>${escapeHtml(user.userId)}</td>
                    <td>${escapeHtml(user.username)}</td>
                    <td>${escapeHtml(user.realName)}</td>
                    <td>${escapeHtml(user.department || "")}</td>
                    <td>${escapeHtml(roleText(user.roleCode))}</td>
                    <td>${user.enabled === 1 ? "启用" : "停用"}</td>
                    ${canAdjust ? `<td><button type="button" data-adjust-user="${escapeHtml(user.userId)}">调整角色</button></td>` : ""}
                </tr>`).join("")}
            </tbody>
        </table>`;
    container.querySelectorAll("[data-adjust-user]").forEach(button => {
        button.addEventListener("click", () => {
            const user = users.find(item => String(item.userId) === String(button.dataset.adjustUser));
            if (user) showRoleAdjustmentDialog(user, roles);
        });
    });
}

function renderPermissionApplications(rows) {
    renderTable("permission-apply-table", rows, [
        { key: "applyNo", label: "申请单" },
        { key: "applicantId", label: "申请人ID" },
        { key: "targetUserId", label: "目标用户ID" },
        { key: "fromRoleCode", label: "原角色", render: row => roleText(row.fromRoleCode) },
        { key: "toRoleCode", label: "申请角色", render: row => roleText(row.toRoleCode) },
        { key: "applyReason", label: "申请说明", render: row => `<span class="multiline-cell">${escapeHtml(row.applyReason || "")}</span>` },
        { key: "applyStatus", label: "状态", render: row => permissionApplyStatusText(row.applyStatus) },
        { key: "reviewComment", label: "审批意见" }
    ], [
        { name: "review-application", label: "审批通过", idKey: "applyId", permission: "permission.review", visible: row => row.applyStatus === "SUBMITTED", handler: id => reviewPermissionApplication(id, "REVIEWED") },
        { name: "reject-application", label: "驳回", idKey: "applyId", permission: "permission.review", visible: row => row.applyStatus === "SUBMITTED", handler: id => reviewPermissionApplication(id, "REJECTED") },
        { name: "apply-application", label: "执行变更", idKey: "applyId", permission: "role.manage", visible: row => ["SUBMITTED", "REVIEWED"].includes(row.applyStatus), handler: applyPermissionApplication }
    ]);
}

async function reviewPermissionApplication(id, decision) {
    const comment = window.prompt(decision === "REJECTED" ? "请输入驳回原因" : "请输入审批意见") || "";
    await postJson(`/access/permission-applications/${id}/review`, { decision, comment });
    showMessage(decision === "REJECTED" ? "申请已驳回" : "审批通过，等待执行变更", "ok");
    await loadAccessManagement();
    await loadDashboard();
}

async function applyPermissionApplication(id) {
    if (!window.confirm("确定执行这条权限变更申请吗？执行后目标用户原登录会话会失效。")) return;
    await postJson(`/access/permission-applications/${id}/apply`);
    showMessage("申请角色已应用，目标用户需要重新登录", "ok");
    await loadAccessManagement();
    await loadDashboard();
}

const ROLE_DEPARTMENT_BY_CODE = {
    SYSTEM_ADMIN: "信息技术部",
    GENERAL_MANAGER: "经营管理层",
    HR_MANAGER: "人事部",
    PMC_PLANNER: "生产计划部",
    WORKSHOP_MANAGER: "生产车间",
    WORKSHOP_OPERATOR: "生产车间",
    PRODUCTION_OPERATOR: "生产车间",
    QUALITY_MANAGER: "质量部",
    QUALITY_INSPECTOR: "质量部",
    PROCESS_ENGINEER: "工艺部",
    WAREHOUSE_ADMIN: "仓储物流部",
    WAREHOUSE_KEEPER: "仓储物流部",
    EQUIPMENT_ADMIN: "设备部",
    EQUIPMENT_MAINTAINER: "设备部"
};

const ROLE_ADJUSTMENT_EXCLUDED_CODES = new Set(["SYSTEM_MAINTAINER", "VIEWER"]);

function roleAdjustmentOptions(roles) {
    return roles.filter(role => !ROLE_ADJUSTMENT_EXCLUDED_CODES.has(role.roleCode));
}

function roleDepartment(role) {
    return ROLE_DEPARTMENT_BY_CODE[role.roleCode] || role.department || "";
}

function roleNeedsLineScope(role) {
    const code = String(role?.roleCode || "");
    const scope = String(role?.dataScope || "").toUpperCase();
    return scope === "LINE" || scope === "CUSTOM" || code.includes("WORKSHOP") || code.includes("PRODUCTION");
}

function roleNeedsWarehouseScope(role) {
    const code = String(role?.roleCode || "");
    const scope = String(role?.dataScope || "").toUpperCase();
    return scope === "WAREHOUSE" || scope === "CUSTOM" || code.includes("WAREHOUSE");
}

function shouldCreateRoleChangeApplication() {
    const session = typeof getCurrentSession === "function" ? getCurrentSession() : null;
    const roles = session?.roles || [];
    return hasPermission("permission.apply") && roles.includes("HR_MANAGER");
}

async function showRoleAdjustmentDialog(user, roles) {
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal-card role-adjustment-modal">
            <h3>调整角色</h3>
            <p class="modal-subtitle">${escapeHtml(user.username)} · ${escapeHtml(user.realName || "")}</p>
            <div class="role-adjustment-grid">
                <label>部门
                    <select id="role-adjust-department">
                        <option value="">请选择部门</option>
                    </select>
                </label>
                <label>具体角色
                    <select id="role-adjust-role" disabled>
                        <option value="">请先选择部门</option>
                    </select>
                </label>
            </div>
            <div class="role-scope-section">
                <h4>选择产线</h4>
                <p class="scope-hint">当前角色需要产线范围时选择。</p>
                <div id="role-adjust-lines" class="scope-checkbox-grid"></div>
            </div>
            <div class="role-scope-section">
                <h4>选择仓库</h4>
                <p class="scope-hint">当前角色需要仓库范围时选择。</p>
                <div id="role-adjust-warehouses" class="scope-checkbox-grid"></div>
            </div>
            <div class="modal-actions">
                <button type="button" id="roleAdjustCancel">取消</button>
                <button type="button" id="roleAdjustSave">${shouldCreateRoleChangeApplication() ? "发起权限变更申请" : "保存调整"}</button>
            </div>
        </div>`;
    document.body.appendChild(mask);

    const departmentSelect = mask.querySelector("#role-adjust-department");
    const roleSelect = mask.querySelector("#role-adjust-role");
    const lineSection = mask.querySelector("#role-adjust-lines")?.closest(".role-scope-section");
    const warehouseSection = mask.querySelector("#role-adjust-warehouses")?.closest(".role-scope-section");
    const availableRoles = roleAdjustmentOptions(roles);
    const departments = [...new Set(availableRoles.map(roleDepartment).filter(Boolean))].sort((a, b) => a.localeCompare(b, "zh-CN"));
    departmentSelect.innerHTML += departments.map(dept => `<option value="${escapeHtml(dept)}">${escapeHtml(dept)}</option>`).join("");

    const updateScopeVisibility = () => {
        const selectedRole = availableRoles.find(role => role.roleCode === roleSelect.value);
        const needsLine = roleNeedsLineScope(selectedRole);
        const needsWarehouse = roleNeedsWarehouseScope(selectedRole);
        lineSection?.classList.toggle("hidden", !needsLine);
        warehouseSection?.classList.toggle("hidden", !needsWarehouse);
        if (!needsLine) mask.querySelectorAll(`[data-scope-type="line"]`).forEach(input => { input.checked = false; });
        if (!needsWarehouse) mask.querySelectorAll(`[data-scope-type="warehouse"]`).forEach(input => { input.checked = false; });
    };
    updateScopeVisibility();

    departmentSelect.addEventListener("change", () => {
        const dept = departmentSelect.value;
        const departmentRoles = availableRoles.filter(role => roleDepartment(role) === dept);
        roleSelect.disabled = !dept;
        roleSelect.innerHTML = dept
            ? `<option value="">请选择角色</option>${departmentRoles.map(role => `<option value="${escapeHtml(role.roleCode)}">${escapeHtml(roleText(role.roleCode, role.roleName))}</option>`).join("")}`
            : `<option value="">请先选择部门</option>`;
        updateScopeVisibility();
    });
    roleSelect.addEventListener("change", updateScopeVisibility);

    await renderRoleScopeOptions(mask, user.userId);
    updateScopeVisibility();

    mask.querySelector("#roleAdjustCancel").addEventListener("click", () => mask.remove());
    mask.addEventListener("click", event => {
        if (event.target === mask) mask.remove();
    });
    mask.querySelector("#roleAdjustSave").addEventListener("click", async () => {
        const department = departmentSelect.value;
        const roleCode = roleSelect.value;
        const selectedRole = availableRoles.find(role => role.roleCode === roleCode);
        if (!department) {
            showMessage("请先选择部门", "error");
            return;
        }
        if (!roleCode) {
            showMessage("请选择具体角色", "error");
            return;
        }
        const lineIds = checkedScopeIds(mask, "line");
        const warehouseIds = checkedScopeIds(mask, "warehouse");
        if (roleNeedsLineScope(selectedRole) && !lineIds.length) {
            showMessage("请选择产线", "error");
            return;
        }
        if (roleNeedsWarehouseScope(selectedRole) && !warehouseIds.length) {
            showMessage("请选择仓库", "error");
            return;
        }
        try {
            if (shouldCreateRoleChangeApplication()) {
                await postJson("/access/permission-applications", {
                    targetUserId: Number(user.userId),
                    toRoleCode: roleCode,
                    reason: roleAdjustmentReason(department, selectedRole, lineIds, warehouseIds)
                });
                showMessage("权限变更申请已提交", "ok");
            } else if (hasPermission("user.update_role")) {
                await putJson(`/access/users/${user.userId}/roles`, { roleCodes: [roleCode] });
                if (hasPermission("data_scope.manage")) {
                    await putJson(`/access/users/${user.userId}/data-scopes`, { lineIds, warehouseIds });
                }
                showMessage("用户角色与数据范围已更新", "ok");
            } else {
                showMessage("当前账号没有发起或执行角色调整的权限", "error");
                return;
            }
            mask.remove();
            await loadAccessManagement();
            await loadDashboard();
        } catch (error) {
            showMessage(error.message, "error");
        }
    });
}

function roleAdjustmentReason(department, role, lineIds, warehouseIds) {
    const parts = [`申请部门：${department}`, `申请角色：${roleText(role?.roleCode, role?.roleName)}`];
    if (lineIds.length) parts.push(`产线范围：${lineIds.join(",")}`);
    if (warehouseIds.length) parts.push(`仓库范围：${warehouseIds.join(",")}`);
    return parts.join("；");
}

async function renderRoleScopeOptions(mask, userId) {
    const [current, lines, warehouses] = await Promise.all([
        hasPermission("data_scope.manage") ? getJson(`/access/users/${userId}/data-scopes`).catch(() => ({ lineIds: [], warehouseIds: [] })) : Promise.resolve({ lineIds: [], warehouseIds: [] }),
        getJson("/production-lines").catch(() => []),
        getJson("/warehouses").catch(() => [])
    ]);
    const currentLineIds = new Set((current.lineIds || []).map(String));
    const currentWarehouseIds = new Set((current.warehouseIds || []).map(String));
    renderScopeCheckboxes(mask.querySelector("#role-adjust-lines"), lines, "line", "lineId",
        item => `${item.lineName || item.lineCode || "产线"} / ID ${item.lineId}`, currentLineIds);
    renderScopeCheckboxes(mask.querySelector("#role-adjust-warehouses"), warehouses, "warehouse", "warehouseId",
        item => `${item.warehouseName || item.warehouseCode || "仓库"} / ID ${item.warehouseId}`, currentWarehouseIds);
}

function renderScopeCheckboxes(container, rows, type, valueKey, labelFn, selectedIds) {
    if (!container) return;
    if (!rows.length) {
        container.innerHTML = `<span class="scope-empty">暂无可选数据</span>`;
        return;
    }
    container.innerHTML = rows.map(row => {
        const value = String(row[valueKey]);
        return `<label class="scope-check"><input type="checkbox" data-scope-type="${type}" value="${escapeHtml(value)}" ${selectedIds.has(value) ? "checked" : ""}>${escapeHtml(labelFn(row))}</label>`;
    }).join("");
}

function checkedScopeIds(root, type) {
    return [...root.querySelectorAll(`[data-scope-type="${type}"]:checked`)]
        .map(input => Number(input.value))
        .filter(value => Number.isInteger(value) && value > 0);
}

async function loadRolePermissions(roleCode) {
    try {
        const permissions = await getJson(`/access/permissions?roleCode=${encodeURIComponent(roleCode)}`);
        const grantedPermissions = permissions.filter(item => item.granted);
        showRolePermissionDialog(roleCode, grantedPermissions);
    } catch (error) {
        showMessage(error.message, "error");
    }
}

function renderSystemMaintenance(summary) {
    summary = summary || {};
    const metrics = document.getElementById("system-maintenance-metrics");
    if (metrics) {
        const visibleMetrics = (summary.metrics || []).filter(item => item.code !== "pendingApplications");
        metrics.innerHTML = visibleMetrics.map(item => `
            <button type="button" class="system-maintenance-card level-${escapeHtml(item.level || "normal")}" data-maintenance-metric="${escapeHtml(item.code)}">
                <span>${escapeHtml(item.label)}</span>
                <strong>${escapeHtml(item.value ?? 0)}<small>${escapeHtml(item.unit || "")}</small></strong>
            </button>
        `).join("") + `<button type="button" class="system-maintenance-card" data-cleanup-expired-sessions>
                <span>运维操作</span>
                <strong>清理<small>过期会话</small></strong>
            </button>`;
        metrics.querySelectorAll("[data-maintenance-metric]").forEach(card => {
            card.addEventListener("click", () => showSystemMaintenanceDetail(card.dataset.maintenanceMetric));
        });
        metrics.querySelector("[data-cleanup-expired-sessions]")?.addEventListener("click", cleanupExpiredSessions);
        hideSystemMaintenanceDetail();
    }
    renderTable("session-table", summary.sessions || [], [
        { key: "sessionId", label: "会话ID" },
        { key: "userId", label: "用户ID" },
        { key: "username", label: "账号" },
        { key: "realName", label: "姓名" },
        { key: "roleCode", label: "角色", render: row => roleText(row.roleCode) },
        { key: "loginIp", label: "登录IP" },
        { key: "createdAt", label: "登录时间", render: row => formatDateTime(row.createdAt) },
        { key: "expiresAt", label: "过期时间", render: row => formatDateTime(row.expiresAt) }
    ], sessionActions());
    renderTable("locked-user-table", summary.lockedUsers || [], [
        { key: "userId", label: "用户ID" },
        { key: "username", label: "账号" },
        { key: "realName", label: "姓名" },
        { key: "roleCode", label: "角色", render: row => roleText(row.roleCode) },
        { key: "failedLoginCount", label: "失败次数" },
        { key: "lockedUntil", label: "锁定至", render: row => formatDateTime(row.lockedUntil) }
    ], lockedUserActions());
    renderTable("audit-log-table", summary.auditLogs || [], [
        { key: "auditId", label: "审计ID" },
        { key: "eventType", label: "事件" },
        { key: "actionCode", label: "动作", render: row => actionText(row.actionCode) },
        { key: "resourceType", label: "资源" },
        { key: "actorUsername", label: "操作人" },
        { key: "actorRoleCode", label: "角色", render: row => roleText(row.actorRoleCode) },
        { key: "result", label: "结果" },
        { key: "createdAt", label: "时间", render: row => formatDateTime(row.createdAt) }
    ]);
    renderTable("sync-log-table", summary.syncLogs || [], [
        { key: "syncLogId", label: "日志ID" },
        { key: "syncType", label: "类型" },
        { key: "sourceSystem", label: "来源系统" },
        { key: "targetTable", label: "目标表" },
        { key: "syncStatus", label: "状态" },
        { key: "message", label: "消息" },
        { key: "createdAt", label: "时间", render: row => formatDateTime(row.createdAt) }
    ], syncLogActions());
}

function showSystemMaintenanceDetail(metricCode) {
    const detail = getSystemMaintenanceDetail(metricCode);
    const panel = ensureSystemMaintenanceDetailPanel();
    const tableId = "system-maintenance-detail-table";
    panel.innerHTML = `
        <div class="system-maintenance-detail-head">
            <div>
                <span>${escapeHtml(detail.kicker)}</span>
                <h3>${escapeHtml(detail.title)}</h3>
            </div>
            <p class="modal-subtitle">${escapeHtml(detail.description)}</p>
        </div>
        <div id="${tableId}"></div>`;
    panel.classList.remove("hidden");
    document.querySelectorAll("[data-maintenance-metric]").forEach(card => {
        card.classList.toggle("active", card.dataset.maintenanceMetric === metricCode);
    });
    renderTable(tableId, detail.rows, detail.columns, detail.actions || []);
    if (typeof updateModuleWorkspace === "function") updateModuleWorkspace(panel);
}

function ensureSystemMaintenanceDetailPanel() {
    let panel = document.getElementById("system-maintenance-detail-panel");
    if (panel) return panel;
    const metrics = document.getElementById("system-maintenance-metrics");
    panel = document.createElement("div");
    panel.id = "system-maintenance-detail-panel";
    panel.className = "system-maintenance-detail-panel hidden";
    metrics?.after(panel);
    return panel;
}

function hideSystemMaintenanceDetail() {
    const panel = ensureSystemMaintenanceDetailPanel();
    panel.classList.add("hidden");
    panel.innerHTML = "";
    document.querySelectorAll("[data-maintenance-metric]").forEach(card => card.classList.remove("active"));
}

function sessionActions() {
    return [
        { name: "revoke-session", label: "强制下线", idKey: "sessionId", permission: "system.health.read", visible: row => Number(row.userId) !== Number(getCurrentSession()?.user?.userId), handler: revokeSession }
    ];
}

function lockedUserActions() {
    return [
        { name: "unlock-user", label: "解除锁定", idKey: "userId", permission: "system.health.read", handler: unlockUser },
        { name: "revoke-user-sessions", label: "撤销会话", idKey: "userId", permission: "system.health.read", visible: row => Number(row.userId) !== Number(getCurrentSession()?.user?.userId), handler: revokeUserSessions }
    ];
}

function syncLogActions() {
    return [
        { name: "mark-sync-handled", label: "标记已处理", idKey: "syncLogId", permission: "system.health.read", visible: row => ["FAILED", "ERROR"].includes(String(row.syncStatus || "").toUpperCase()), handler: markSyncLogHandled }
    ];
}

function failedLoginActions() {
    return [
        { name: "unlock-login-user", label: "解除锁定", idKey: "actorUserId", permission: "system.health.read", visible: row => Boolean(row.actorUserId), handler: unlockUser },
        { name: "revoke-login-user-sessions", label: "撤销会话", idKey: "actorUserId", permission: "system.health.read", visible: row => Boolean(row.actorUserId) && Number(row.actorUserId) !== Number(getCurrentSession()?.user?.userId), handler: revokeUserSessions }
    ];
}

function enabledUserActions() {
    return [
        { name: "disable-user", label: "删除账号", idKey: "userId", permission: "system.health.read", visible: row => Number(row.userId) !== Number(getCurrentSession()?.user?.userId), handler: disableUser }
    ];
}

async function runSystemMaintenanceAction(path, message) {
    try {
        await postJson(path);
        showMessage(message, "ok");
        await loadAccessManagement();
        await loadDashboard();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function revokeSession(sessionId) {
    if (!window.confirm("确定要强制下线这个会话吗？")) return;
    await runSystemMaintenanceAction(`/access/system-maintenance/sessions/${sessionId}/revoke`, "登录会话已强制下线");
}

async function revokeUserSessions(userId) {
    if (!window.confirm("确定要撤销该用户的所有有效会话吗？")) return;
    await runSystemMaintenanceAction(`/access/system-maintenance/users/${userId}/revoke-sessions`, "用户有效会话已撤销");
}

async function cleanupExpiredSessions() {
    await runSystemMaintenanceAction("/access/system-maintenance/sessions/cleanup-expired", "过期会话已清理");
}

async function unlockUser(userId) {
    await runSystemMaintenanceAction(`/access/system-maintenance/users/${userId}/unlock`, "账号锁定已解除");
}

async function disableUser(userId) {
    if (!window.confirm("确定要删除这个账号吗？删除后该用户将无法登录系统。")) return;
    await runSystemMaintenanceAction(`/access/system-maintenance/users/${userId}/disable`, "账号已删除，用户无法再登录");
}

async function markSyncLogHandled(syncLogId) {
    await runSystemMaintenanceAction(`/access/system-maintenance/sync-logs/${syncLogId}/mark-handled`, "同步异常已标记处理");
}

function getSystemMaintenanceDetail(metricCode) {
    const summary = accessMaintenanceSummary || {};
    const failedAuditLogs = (summary.failedLoginLogs || summary.auditLogs || []).filter(row =>
        String(row.eventType || "").toUpperCase() === "LOGIN" && String(row.result || "").toUpperCase() === "FAILED");
    const syncFailures = (summary.syncFailures || summary.syncLogs || []).filter(row =>
        ["FAILED", "ERROR"].includes(String(row.syncStatus || "").toUpperCase()));
    const enabledUsers = accessUsers.filter(user => user.enabled === 1 || user.enabled === true);

    const userColumns = [
        { key: "userId", label: "用户ID" },
        { key: "username", label: "账号" },
        { key: "realName", label: "姓名" },
        { key: "department", label: "部门" },
        { key: "roleCode", label: "角色", render: row => roleText(row.roleCode) },
        { key: "lastLoginAt", label: "最近登录", render: row => formatDateTime(row.lastLoginAt) }
    ];
    const sessionColumns = [
        { key: "sessionId", label: "会话ID" },
        { key: "userId", label: "用户ID" },
        { key: "username", label: "账号" },
        { key: "realName", label: "姓名" },
        { key: "roleCode", label: "角色", render: row => roleText(row.roleCode) },
        { key: "loginIp", label: "登录IP" },
        { key: "createdAt", label: "登录时间", render: row => formatDateTime(row.createdAt) },
        { key: "expiresAt", label: "过期时间", render: row => formatDateTime(row.expiresAt) }
    ];
    const lockedColumns = [
        { key: "userId", label: "用户ID" },
        { key: "username", label: "账号" },
        { key: "realName", label: "姓名" },
        { key: "roleCode", label: "角色", render: row => roleText(row.roleCode) },
        { key: "failedLoginCount", label: "失败次数" },
        { key: "lockedUntil", label: "锁定至", render: row => formatDateTime(row.lockedUntil) }
    ];
    const auditColumns = [
        { key: "auditId", label: "审计ID" },
        { key: "actorUsername", label: "账号" },
        { key: "actorRoleCode", label: "角色", render: row => roleText(row.actorRoleCode) },
        { key: "actionCode", label: "动作", render: row => actionText(row.actionCode) },
        { key: "resourceType", label: "资源" },
        { key: "result", label: "结果" },
        { key: "createdAt", label: "时间", render: row => formatDateTime(row.createdAt) }
    ];
    const syncColumns = [
        { key: "syncLogId", label: "日志ID" },
        { key: "syncType", label: "类型" },
        { key: "sourceSystem", label: "来源系统" },
        { key: "targetTable", label: "目标表" },
        { key: "syncStatus", label: "状态" },
        { key: "message", label: "消息" },
        { key: "createdAt", label: "时间", render: row => formatDateTime(row.createdAt) }
    ];

    return {
        enabledUsers: {
            title: "启用账号明细",
            kicker: "系统账号",
            description: "当前处于启用状态的账号。若当前角色无用户读取权限，此处可能为空。",
            rows: enabledUsers,
            columns: userColumns,
            actions: enabledUserActions()
        },
        lockedUsers: {
            title: "锁定账号明细",
            kicker: "登录安全",
            description: "因登录失败或安全策略被临时锁定的账号。",
            rows: summary.lockedUsers || [],
            columns: lockedColumns,
            actions: lockedUserActions()
        },
        activeSessions: {
            title: "有效会话明细",
            kicker: "在线会话",
            description: "当前未注销且未过期的登录会话。",
            rows: summary.sessions || [],
            columns: sessionColumns,
            actions: sessionActions()
        },
        failedLogins: {
            title: "失败登录明细",
            kicker: "24小时登录风险",
            description: "最近审计日志中登录失败的记录。",
            rows: failedAuditLogs,
            columns: auditColumns,
            actions: failedLoginActions()
        },
        syncFailures: {
            title: "同步异常明细",
            kicker: "数据同步",
            description: "同步状态为 FAILED 或 ERROR 的数据同步日志。",
            rows: syncFailures,
            columns: syncColumns,
            actions: syncLogActions()
        }
    }[metricCode] || {
        title: "系统运维明细",
        kicker: "系统运行",
        description: "该指标暂无可展示的明细。",
        rows: [],
        columns: [],
        actions: []
    };
}

function formatDateTime(value) {
    if (!value) return "";
    return String(value).replace("T", " ").slice(0, 19);
}

function showRolePermissionDialog(roleCode, permissions) {
    const role = accessRoles.find(item => item.roleCode === roleCode);
    const modalId = `rolePermissionModalTable-${Date.now()}`;
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal-card role-permission-modal">
            <h3>${escapeHtml(roleText(roleCode, role?.roleName))} 权限明细</h3>
            <p class="modal-subtitle">${escapeHtml(roleCode)} · 共 ${permissions.length} 个权限点</p>
            <div id="${modalId}"></div>
            <div class="modal-actions">
                <button type="button" id="rolePermissionClose">关闭</button>
            </div>
        </div>`;
    document.body.appendChild(mask);
    renderTable(modalId, permissions, [
        { key: "moduleCode", label: "模块", render: row => moduleText(row.moduleCode) },
        { key: "permissionCode", label: "权限项", render: row => permissionCodeText(row.permissionCode, row.permissionName) },
        { key: "permissionName", label: "权限名称" },
        { key: "actionCode", label: "动作", render: row => actionText(row.actionCode) },
        { key: "riskLevel", label: "风险级别", render: row => levelText(row.riskLevel) }
    ]);
    mask.querySelector("#rolePermissionClose").addEventListener("click", () => mask.remove());
}

function roleText(roleCode, fallbackName = "") {
    return {
        SYSTEM_ADMIN: "系统管理员",
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

function permissionApplyStatusText(status) {
    const key = String(status || "").toUpperCase();
    return {
        DRAFT: "草稿",
        SUBMITTED: "待审批",
        REVIEWED: "审批通过",
        REJECTED: "已驳回",
        RETURNED: "已退回",
        APPLIED: "已执行"
    }[key] || status || "";
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
document.getElementById("refresh-system-ops")?.addEventListener("click", loadAccessManagement);
document.getElementById("refresh-audit")?.addEventListener("click", loadAccessManagement);
