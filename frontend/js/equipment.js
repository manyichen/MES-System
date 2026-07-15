let equipmentCache = [];

async function loadEquipment(options = {}) {
    try {
        const equipment = await getJson("/equipment");
        equipmentCache = Array.isArray(equipment) ? equipment : [];
        const equipmentById = new Map(equipmentCache.map(item => [String(item.equipmentId), item]));
        refreshRepairEquipmentOptions(equipmentCache);
        renderTable("equipment-table", equipmentCache, [
            { key: "equipmentId", label: "ID" },
            { key: "equipmentCode", label: "编码" },
            { key: "equipmentName", label: "名称" },
            { key: "equipmentType", label: "类型", render: row => typeText(row.equipmentType) },
            { key: "lineId", label: "产线" },
            { key: "equipmentStatus", label: "状态", render: row => equipmentStatusText(row.equipmentStatus) },
            { key: "enabled", label: "启用", render: row => equipmentEnabledText(row) }
        ], [
            { name: "repair-equipment", label: "报修", idKey: "equipmentId", permission: "equipment.fault.report", visible: row => row.equipmentStatus === "FAULT", handler: openRepairReportDialog },
            ...equipmentStatusActions()
        ]);

        const repairs = await getJson("/equipment-repair-reports");
        renderTable("repair-table", repairs, [
            { key: "repairReportId", label: "ID" },
            { key: "repairReportNo", label: "报修单" },
            { key: "equipmentId", label: "故障设备", render: row => equipmentDisplay(row.equipmentId, equipmentById) },
            { key: "faultLevel", label: "级别", render: row => levelText(row.faultLevel) },
            { key: "faultDesc", label: "描述" },
            { key: "repairStatus", label: "状态", render: row => repairStatusText(row.repairStatus) }
        ], [
            { name: "approve-repair", label: "审核并生成维修", idKey: "repairReportId", permission: "equipment.repair.review", visible: row => row.repairStatus === "REPORTED", handler: approveRepair },
            { name: "to-maintenance", label: "补生成维修", idKey: "repairReportId", permission: "equipment.maintenance.assign", visible: row => row.repairStatus === "APPROVED", handler: toMaintenanceOrder }
        ]);

        const orders = await getJson("/maintenance-orders");
        renderTable("maintenance-table", orders, [
            { key: "equipmentId", label: "对应设备", render: row => equipmentDisplay(row.equipmentId, equipmentById) },
            { key: "maintenanceOrderId", label: "ID" },
            { key: "maintenanceOrderNo", label: "维修单" },
            { key: "repairReportId", label: "报修单" },
            { key: "maintainerId", label: "维护员" },
            { key: "maintenanceStatus", label: "状态", render: row => maintenanceStatusText(row.maintenanceStatus) },
            { key: "resultDesc", label: "结果" }
        ], [
            { name: "assign-maintenance", label: "派工", idKey: "maintenanceOrderId", permission: "equipment.maintenance.assign", visible: row => row.maintenanceStatus === "CREATED", handler: assignMaintenance },
            { name: "finish-maintenance", label: "上传结果", idKey: "maintenanceOrderId", permission: "equipment.maintenance.execute", visible: row => ["ASSIGNED", "IN_PROGRESS"].includes(row.maintenanceStatus), handler: finishMaintenance },
            { name: "accept-maintenance", label: "验收", idKey: "maintenanceOrderId", permission: "equipment.maintenance.accept", visible: row => row.maintenanceStatus === "FINISHED", handler: acceptMaintenance }
        ]);

        await loadMaintenancePlans();
        if (options.notify) showMessage("设备维护数据已刷新", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function loadMaintenancePlans(options = {}) {
    const container = document.getElementById("maintenance-plan-table");
    if (!container) return;
    try {
        const plans = await getJson("/maintenance-plans");
        renderTable("maintenance-plan-table", plans, [
            { key: "maintenancePlanId", label: "ID" },
            { key: "equipmentId", label: "设备" },
            { key: "planCycle", label: "周期", render: row => cycleText(row.planCycle) },
            { key: "nextPlanTime", label: "下次维护" },
            { key: "planStatus", label: "状态", render: row => statusText(row.planStatus) }
        ]);
        if (options.notify) showMessage("维护计划已刷新", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function openRepairReportDialog(id) {
    const equipment = equipmentCache.find(item => Number(item.equipmentId) === Number(id));
    if (!equipment) {
        showMessage("未找到设备信息，请刷新后重试", "error");
        return;
    }
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal-card equipment-repair-modal">
            <h3>填写报修单</h3>
            <form id="equipmentRepairDialogForm" class="equipment-repair-form">
                <input type="hidden" name="equipmentId" value="${escapeHtml(equipment.equipmentId)}">
                <div class="edit-summary">
                    ${equipmentDialogField("设备编码", equipment.equipmentCode)}
                    ${equipmentDialogField("设备名称", equipment.equipmentName)}
                    ${equipmentDialogField("设备类型", typeText(equipment.equipmentType))}
                    ${equipmentDialogField("当前状态", equipmentStatusText(equipment.equipmentStatus))}
                </div>
                <label>报修单号 <input name="repairReportNo" value="${escapeHtml(`REP-${Date.now()}`)}" required></label>
                <label>工单ID <input name="workOrderId" type="number" min="1"></label>
                <label>故障级别
                    <select name="faultLevel">
                        <option value="HIGH" selected>高</option>
                        <option value="MEDIUM">中</option>
                        <option value="LOW">低</option>
                        <option value="URGENT">紧急</option>
                        <option value="CRITICAL">严重</option>
                    </select>
                </label>
                <label class="wide-field">故障描述 <textarea name="faultDesc" rows="3" required placeholder="请填写设备故障现象">设备异常停机</textarea></label>
                <div class="modal-actions">
                    <button type="button" id="equipmentRepairCancel">取消</button>
                    <button type="submit">提交报修</button>
                </div>
            </form>
        </div>`;
    document.body.appendChild(mask);
    mask.querySelector("#equipmentRepairCancel")?.addEventListener("click", () => mask.remove());
    mask.addEventListener("click", event => {
        if (event.target === mask) mask.remove();
    });
    mask.querySelector("#equipmentRepairDialogForm")?.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            await submitRepairReport(event.target);
            mask.remove();
        } catch (error) {
            showMessage(toChineseError(error), "error");
        }
    });
}

async function submitRepairReport(form) {
    const payload = { ...formToObject(form), reportTime: nowIsoLocal(), repairStatus: "REPORTED" };
    if (!payload.repairReportNo || payload.repairReportNo === "REP-DEMO-001") {
        payload.repairReportNo = `REP-${Date.now()}`;
    }
    await postJson("/equipment-repair-reports", payload);
    showMessage("报修单已提交", "ok");
    await loadEquipment();
    await loadDashboard();
}

function equipmentDialogField(label, value) {
    return `<div class="detail-row"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value ?? "-")}</strong></div>`;
}

async function updateEquipmentStatus(id, status) {
    try {
        await postJson(`/equipment/${id}/status`, { status });
        showMessage(`设备状态已更新为：${equipmentStatusText(status)}`, "ok");
        await loadEquipment();
        await loadDashboard();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function equipmentStatusActions() {
    return [
        { status: "RUNNING", label: "设为运行" },
        { status: "IDLE", label: "设为空闲" },
        { status: "FAULT", label: "设为故障" },
        { status: "MAINTENANCE", label: "设为维护" }
    ].map(item => ({
        name: `status-${item.status.toLowerCase()}`,
        label: item.label,
        idKey: "equipmentId",
        permission: "equipment.manage",
        visible: row => row.equipmentStatus !== item.status,
        handler: id => updateEquipmentStatus(id, item.status)
    }));
}

function equipmentEnabledText(row) {
    return row.equipmentStatus === "RUNNING" ? "是" : "否";
}

function equipmentDisplay(equipmentId, equipmentById) {
    const equipment = equipmentById.get(String(equipmentId));
    if (!equipment) return equipmentId || "-";
    return `${equipment.equipmentName}（${equipment.equipmentCode}）`;
}

function refreshRepairEquipmentOptions(equipment) {
    const select = document.getElementById("repairEquipmentSelect");
    if (!select) return;
    const currentValue = select.value;
    select.innerHTML = "";
    if (!equipment.length) {
        const option = document.createElement("option");
        option.value = "";
        option.textContent = "暂无可报修设备";
        select.appendChild(option);
        return;
    }
    for (const item of equipment) {
        const option = document.createElement("option");
        option.value = item.equipmentId;
        option.textContent = `${item.equipmentName}（${item.equipmentCode}） / ${equipmentStatusText(item.equipmentStatus)}`;
        select.appendChild(option);
    }
    select.value = equipment.some(item => String(item.equipmentId) === currentValue) ? currentValue : (equipment[0]?.equipmentId || "");
}

async function approveRepair(id) {
    try {
        await postJson(`/equipment-repair-reports/${id}/approve`);
        showMessage("报修单已审核，维修工单已自动生成", "ok");
        await loadEquipment();
        await loadDashboard();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function toMaintenanceOrder(id) {
    try {
        await postJson(`/equipment-repair-reports/${id}/to-maintenance-order`);
        showMessage("维修工单已生成", "ok");
        await loadEquipment();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function assignMaintenance(id) {
    await openAssignMaintenanceDialog(id);
}

async function openAssignMaintenanceDialog(id) {
    try {
        const users = await getJson("/users").catch(() => []);
        const maintainers = users.filter(user => user.roleCode === "EQUIPMENT_MAINTAINER" && Number(user.enabled) !== 0);
        let drawer = document.getElementById("maintenance-assign-dialog");
        if (!drawer) {
            drawer = document.createElement("aside");
            drawer.id = "maintenance-assign-dialog";
            drawer.className = "detail-drawer";
            document.body.appendChild(drawer);
        }
        const options = maintainers.map(user =>
            `<option value="${escapeHtml(user.userId)}">${escapeHtml(user.realName || user.username)}（ID ${escapeHtml(user.userId)}）</option>`
        ).join("");
        drawer.innerHTML = `
            <div class="detail-drawer-head">
                <div><span>维修派工</span><h3>维修工单 ${escapeHtml(id)} · 选择设备维修员</h3></div>
                <button type="button" class="detail-close" aria-label="关闭">×</button>
            </div>
            <div class="detail-drawer-body quality-submit-body">
                <form id="maintenance-assign-form" class="quality-submit-form">
                    <section class="quality-submit-card">
                        <span class="quality-submit-label">设备维修员</span>
                        ${maintainers.length
                            ? `<select id="maintenance-assign-maintainer" required>${options}</select>`
                            : `<input id="maintenance-assign-maintainer" type="number" min="1" required placeholder="没有读取到维修员列表，请输入维修员用户 ID">`}
                    </section>
                    <button type="submit" class="quality-submit-button">派发维修工单</button>
                </form>
            </div>`;
        closeModuleDrawers();
        ensureWorkspaceBackdrop().classList.add("open");
        drawer.classList.add("open");
        document.body.classList.add("overlay-open");
        drawer.querySelector(".detail-close")?.addEventListener("click", closeModuleDrawers);
        drawer.querySelector("#maintenance-assign-form")?.addEventListener("submit", async event => {
            event.preventDefault();
            const maintainerId = drawer.querySelector("#maintenance-assign-maintainer")?.value;
            if (!maintainerId) {
                showMessage("请选择设备维修员", "error");
                return;
            }
            await submitMaintenanceAssignment(id, maintainerId);
        });
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function submitMaintenanceAssignment(id, maintainerId) {
    try {
        await postJson(`/maintenance-orders/${id}/assign?maintainerId=${encodeURIComponent(maintainerId)}`);
        showMessage("维修工单已派发给维护员", "ok");
        closeModuleDrawers();
        await loadEquipment();
        await loadDashboard();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function finishMaintenance(id) {
    const form = document.getElementById("maintenance-result-form");
    if (form) {
        form.maintenanceOrderId.value = id;
        const drawer = form.closest(".module-drawer");
        if (drawer && typeof selectActionView === "function") {
            selectActionView(drawer, form.dataset.actionView);
        }
        if (drawer && typeof openModuleDrawer === "function") {
            openModuleDrawer(drawer);
        } else {
            form.scrollIntoView({ behavior: "smooth", block: "center" });
        }
        showMessage("请填写维修结果后提交验收", "info");
        return;
    }
    showMessage("请使用上传维修结果表单提交验收", "info");
}

async function acceptMaintenance(id) {
    try {
        await postJson(`/maintenance-orders/${id}/accept`);
        showMessage("维修工单已验收，设备状态已恢复", "ok");
        await loadEquipment();
        await loadDashboard();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function bindEquipmentEvents() {
    document.getElementById("refresh-equipment")?.addEventListener("click", () => loadEquipment({ notify: true }));

    document.getElementById("equipment-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            const payload = { ...formToObject(event.target), lastMaintenanceTime: null, enabled: true };
            if (payload.equipmentCode === "EQ-DEMO-001") {
                payload.equipmentCode = `EQ-${Date.now()}`;
            }
            await postJson("/equipment", payload);
            showMessage("设备已创建", "ok");
            event.target.reset();
            await loadEquipment();
        } catch (error) {
            showMessage(toChineseError(error), "error");
        }
    });

    document.getElementById("repair-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            await submitRepairReport(event.target);
            event.target.reset();
        } catch (error) {
            showMessage(toChineseError(error), "error");
        }
    });

    document.getElementById("maintenance-plan-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            const payload = { ...formToObject(event.target), createdAt: nowIsoLocal() };
            await postJson("/maintenance-plans", payload);
            showMessage("维护计划已创建", "ok");
            event.target.reset();
            await loadMaintenancePlans();
        } catch (error) {
            showMessage(toChineseError(error), "error");
        }
    });

    document.getElementById("maintenance-result-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const status = document.getElementById("maintenance-result-status");
        if (status) status.textContent = "正在上传维修结果...";
        try {
            const values = formToObject(event.target);
            const resultDesc = [
                `故障原因：${values.faultCause || ""}`,
                `处理措施：${values.repairAction || ""}`,
                values.replacedParts ? `更换备件：${values.replacedParts}` : "",
                `维修结论：${values.repairConclusion || ""}`
            ].filter(Boolean).join("\n");
            await postJson(`/maintenance-orders/${values.maintenanceOrderId}/finish`, { resultDesc });
            if (status) status.textContent = "维修结果已上传，等待设备管理员验收";
            showMessage("维修结果已上传，等待设备管理员验收", "ok");
            event.target.reset();
            await loadEquipment();
            await loadDashboard();
        } catch (error) {
            const message = toChineseError(error);
            if (status) status.textContent = `上传失败：${message}`;
            showMessage(message, "error");
        }
    });
}

function equipmentStatusText(status) {
    return statusText(status);
}

function repairStatusText(status) {
    return statusText(status);
}

function maintenanceStatusText(status) {
    return statusText(status);
}
