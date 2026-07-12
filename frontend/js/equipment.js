async function loadEquipment(options = {}) {
    try {
        const equipment = await getJson("/equipment");
        renderTable("equipment-table", equipment, [
            { key: "equipmentId", label: "ID" },
            { key: "equipmentCode", label: "编码" },
            { key: "equipmentName", label: "名称" },
            { key: "equipmentType", label: "类型", render: row => typeText(row.equipmentType) },
            { key: "lineId", label: "产线" },
            { key: "equipmentStatus", label: "状态", render: row => equipmentStatusText(row.equipmentStatus) },
            { key: "enabled", label: "启用" }
        ], [
            { name: "status-running", label: "设为运行", idKey: "equipmentId", permission: "equipment.manage", visible: row => row.equipmentStatus !== "RUNNING", handler: id => updateEquipmentStatus(id, "RUNNING") },
            { name: "status-fault", label: "设为故障", idKey: "equipmentId", permission: "equipment.manage", visible: row => row.equipmentStatus !== "FAULT", handler: id => updateEquipmentStatus(id, "FAULT") }
        ]);

        const repairs = await getJson("/equipment-repair-reports");
        renderTable("repair-table", repairs, [
            { key: "repairReportId", label: "ID" },
            { key: "repairReportNo", label: "报修单" },
            { key: "equipmentId", label: "设备" },
            { key: "faultLevel", label: "级别", render: row => levelText(row.faultLevel) },
            { key: "faultDesc", label: "描述" },
            { key: "repairStatus", label: "状态", render: row => repairStatusText(row.repairStatus) }
        ], [
            { name: "approve-repair", label: "审核并生成维修", idKey: "repairReportId", permission: "equipment.repair.review", visible: row => row.repairStatus === "REPORTED", handler: approveRepair },
            { name: "to-maintenance", label: "补生成维修", idKey: "repairReportId", permission: "equipment.maintenance.assign", visible: row => row.repairStatus === "APPROVED", handler: toMaintenanceOrder }
        ]);

        const orders = await getJson("/maintenance-orders");
        renderTable("maintenance-table", orders, [
            { key: "maintenanceOrderId", label: "ID" },
            { key: "maintenanceOrderNo", label: "维修单" },
            { key: "repairReportId", label: "报修单" },
            { key: "equipmentId", label: "设备" },
            { key: "maintainerId", label: "维护员" },
            { key: "maintenanceStatus", label: "状态", render: row => maintenanceStatusText(row.maintenanceStatus) },
            { key: "resultDesc", label: "结果" }
        ], [
            { name: "assign-maintenance", label: "派工", idKey: "maintenanceOrderId", permission: "equipment.maintenance.assign", visible: row => row.maintenanceStatus === "CREATED", handler: assignMaintenance },
            { name: "finish-maintenance", label: "完成维修", idKey: "maintenanceOrderId", permission: "equipment.maintenance.execute", visible: row => ["ASSIGNED", "IN_PROGRESS"].includes(row.maintenanceStatus), handler: finishMaintenance },
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
    try {
        const maintainerId = window.prompt("请输入设备维护员用户 ID");
        if (!maintainerId) {
            showMessage("已取消维修派工", "info");
            return;
        }
        await postJson(`/maintenance-orders/${id}/assign?maintainerId=${encodeURIComponent(maintainerId)}`);
        showMessage("维修工单已派发给维护员", "ok");
        await loadEquipment();
        await loadDashboard();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function finishMaintenance(id) {
    try {
        await postJson(`/maintenance-orders/${id}/finish`);
        showMessage("维修工单已完成，等待设备管理员验收", "ok");
        await loadEquipment();
        await loadDashboard();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
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
            const payload = { ...formToObject(event.target), reportTime: nowIsoLocal(), repairStatus: "REPORTED" };
            if (payload.repairReportNo === "REP-DEMO-001") {
                payload.repairReportNo = `REP-${Date.now()}`;
            }
            await postJson("/equipment-repair-reports", payload);
            showMessage("报修单已提交，设备状态已自动变为故障", "ok");
            event.target.reset();
            await loadEquipment();
            await loadDashboard();
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
