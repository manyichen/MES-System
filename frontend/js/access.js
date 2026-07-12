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
            { key: "roleCode", label: "角色代码" },
            { key: "roleName", label: "角色名称" },
            { key: "dataScope", label: "数据范围" },
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
    select.innerHTML = roles.map(role => `<option value="${escapeHtml(role.roleCode)}">${escapeHtml(role.roleName || displayText(role.roleCode))}</option>`).join("");
}

function renderPermissionApplications(rows) {
    renderTable("permission-apply-table", rows, [
        { key: "applyNo", label: "申请单" }, { key: "applicantId", label: "申请人ID" },
        { key: "targetUserId", label: "目标用户ID" }, { key: "fromRoleCode", label: "原角色" },
        { key: "toRoleCode", label: "申请角色" }, { key: "applyReason", label: "原因" },
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
    const roleOptions = roles.map(role => `<option value="${escapeHtml(role.roleCode)}">${escapeHtml(role.roleName || displayText(role.roleCode))}</option>`).join("");
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
                    <td>${escapeHtml(displayText(user.roleCode))}</td>
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
            { key: "moduleCode", label: "模块" },
            { key: "permissionCode", label: "权限代码" },
            { key: "permissionName", label: "权限名称" },
            { key: "actionCode", label: "动作" },
            { key: "riskLevel", label: "风险级别" }
        ]);
    } catch (error) {
        showMessage(error.message, "error");
    }
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
