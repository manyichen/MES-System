async function loadEquipment() {
    try {
        const equipment = await getJson("/equipment");
        renderTable("equipment-table", equipment, [
            { key: "equipmentId", label: "ID" },
            { key: "equipmentCode", label: "编码" },
            { key: "equipmentName", label: "名称" },
            { key: "equipmentType", label: "类型" },
            { key: "lineId", label: "产线" },
            { key: "equipmentStatus", label: "状态" },
            { key: "enabled", label: "启用" }
        ], [
            { name: "status-running", label: "运行", idKey: "equipmentId", handler: id => updateEquipmentStatus(id, "RUNNING") },
            { name: "status-fault", label: "故障", idKey: "equipmentId", handler: id => updateEquipmentStatus(id, "FAULT") }
        ]);

        const repairs = await getJson("/equipment-repair-reports");
        renderTable("repair-table", repairs, [
            { key: "repairReportId", label: "ID" },
            { key: "repairReportNo", label: "报修单" },
            { key: "equipmentId", label: "设备" },
            { key: "faultLevel", label: "级别" },
            { key: "faultDesc", label: "描述" },
            { key: "repairStatus", label: "状态" }
        ], [
            { name: "approve-repair", label: "审核", idKey: "repairReportId", handler: approveRepair },
            { name: "to-maintenance", label: "转维修", idKey: "repairReportId", handler: toMaintenanceOrder }
        ]);

        const orders = await getJson("/maintenance-orders");
        renderTable("maintenance-table", orders, [
            { key: "maintenanceOrderId", label: "ID" },
            { key: "maintenanceOrderNo", label: "维修单" },
            { key: "repairReportId", label: "报修单" },
            { key: "equipmentId", label: "设备" },
            { key: "maintenanceStatus", label: "状态" },
            { key: "resultDesc", label: "结果" }
        ], [
            { name: "assign-maintenance", label: "派工", idKey: "maintenanceOrderId", handler: assignMaintenance },
            { name: "finish-maintenance", label: "完成", idKey: "maintenanceOrderId", handler: finishMaintenance },
            { name: "accept-maintenance", label: "验收", idKey: "maintenanceOrderId", handler: acceptMaintenance }
        ]);
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function updateEquipmentStatus(id, status) {
    await postJson(`/equipment/${id}/status`, { status });
    showMessage(`设备状态已更新为 ${status}`);
    loadEquipment();
    loadDashboard();
}

async function approveRepair(id) {
    await postJson(`/equipment-repair-reports/${id}/approve`);
    showMessage("报修单已审核");
    loadEquipment();
}

async function toMaintenanceOrder(id) {
    await postJson(`/equipment-repair-reports/${id}/to-maintenance-order`);
    showMessage("维修工单已生成");
    loadEquipment();
}

async function assignMaintenance(id) {
    await postJson(`/maintenance-orders/${id}/assign`);
    showMessage("维修工单已派工");
    loadEquipment();
}

async function finishMaintenance(id) {
    await postJson(`/maintenance-orders/${id}/finish`);
    showMessage("维修工单已完成");
    loadEquipment();
}

async function acceptMaintenance(id) {
    await postJson(`/maintenance-orders/${id}/accept`);
    showMessage("维修工单已验收");
    loadEquipment();
}

function bindEquipmentEvents() {
    document.getElementById("refresh-equipment")?.addEventListener("click", loadEquipment);

    document.getElementById("equipment-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const payload = {
            ...formToObject(event.target),
            lastMaintenanceTime: null,
            enabled: true
        };
        await postJson("/equipment", payload);
        showMessage("设备已创建");
        event.target.reset();
        loadEquipment();
    });

    document.getElementById("repair-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const payload = {
            ...formToObject(event.target),
            reportTime: nowIsoLocal(),
            repairStatus: "SUBMITTED"
        };
        await postJson("/equipment-repair-reports", payload);
        showMessage("报修单已提交");
        event.target.reset();
        loadEquipment();
    });
}
