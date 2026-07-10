async function refreshWarehouse() {
    const [
        materials,
        warehouses,
        locations,
        inventory,
        robots,
        requisitions,
        pickingTasks,
        deliveryTasks,
        transactions
    ] = await Promise.all([
        getJson("/materials"),
        getJson("/warehouses"),
        getJson("/warehouses/locations"),
        getJson("/inventory"),
        getJson("/robots"),
        getJson("/requisitions"),
        getJson("/picking-tasks"),
        getJson("/robot-delivery-tasks"),
        getJson("/inventory/transactions")
    ]);
    renderTable("materialTable", materials, [
        { title: "ID", key: "materialId" },
        { title: "编码", key: "materialCode" },
        { title: "名称", key: "materialName" },
        { title: "单位", key: "unit" },
        { title: "操作", render: row => `
            <button onclick="showMaterialDetail(${row.materialId})">详情</button>
            <button onclick="fillMaterial(${row.materialId})">使用</button>
        ` }
    ]);
    renderTable("warehouseTable", warehouses, [
        { title: "ID", key: "warehouseId" },
        { title: "编码", key: "warehouseCode" },
        { title: "名称", key: "warehouseName" },
        { title: "类型", key: "warehouseType" },
        { title: "操作", render: row => `<button onclick="showWarehouseDetail(${row.warehouseId})">详情</button>` }
    ]);
    renderTable("locationTable", locations, [
        { title: "ID", key: "locationId" },
        { title: "仓库", key: "warehouseId" },
        { title: "编码", key: "locationCode" },
        { title: "名称", key: "locationName" },
        { title: "操作", render: row => `<button onclick="showLocationDetail(${row.locationId})">详情</button>` }
    ]);
    renderTable("inventoryTable", inventory, [
        { title: "ID", key: "inventoryId" },
        { title: "物料", key: "materialId" },
        { title: "批次", key: "batchNo" },
        { title: "可用", key: "availableQty" },
        { title: "状态", key: "qualityStatus" },
        { title: "操作", render: row => `
            <button onclick="showInventoryDetail(${row.inventoryId})">详情</button>
            <button onclick="fillInventory(${row.materialId}, '${escapeJs(row.batchNo)}')">使用</button>
        ` }
    ]);
    renderTable("robotTable", robots, [
        { title: "ID", key: "robotId" },
        { title: "编码", key: "robotCode" },
        { title: "名称", key: "robotName" },
        { title: "状态", key: "robotStatus" },
        { title: "电量", key: "batteryLevel" },
        { title: "操作", render: row => `<button onclick="showRobotDetail(${row.robotId})">详情</button>` }
    ]);
    renderTable("requisitionTable", requisitions, [
        { title: "ID", key: "requisitionId" },
        { title: "编号", key: "requisitionNo" },
        { title: "工单", key: "workOrderId" },
        { title: "状态", key: "requestStatus" },
        { title: "操作", render: renderRequisitionActions }
    ]);
    renderTable("pickingTable", pickingTasks, [
        { title: "ID", key: "pickingTaskId" },
        { title: "编号", key: "pickingTaskNo" },
        { title: "领料", key: "requisitionId" },
        { title: "状态", key: "taskStatus" },
        { title: "操作", render: renderPickingActions }
    ]);
    renderTable("deliveryTable", deliveryTasks, [
        { title: "ID", key: "deliveryTaskId" },
        { title: "编号", key: "deliveryTaskNo" },
        { title: "机器人", key: "robotId" },
        { title: "状态", key: "deliveryStatus" },
        { title: "操作", render: renderDeliveryActions }
    ]);
    renderTable("transactionTable", transactions, [
        { title: "ID", key: "transactionId" },
        { title: "编号", key: "transactionNo" },
        { title: "物料", key: "materialId" },
        { title: "类型", key: "transactionType" },
        { title: "数量", key: "qty" },
        { title: "来源", key: "sourceDocType" },
        { title: "操作", render: row => `<button onclick="showTransactionDetail(${row.transactionId})">详情</button>` }
    ]);
}

function escapeJs(value) {
    return String(value ?? "").replaceAll("\\", "\\\\").replaceAll("'", "\\'");
}

function fillMaterial(materialId) {
    document.querySelector("#requisitionForm [name='materialId']").value = materialId;
    showMessage("已填入物料 ID");
}

function fillInventory(materialId, batchNo) {
    document.querySelector("#requisitionForm [name='materialId']").value = materialId;
    document.querySelector("#requisitionForm [name='batchNo']").value = batchNo;
    showMessage("已填入物料和批次");
}

function renderRequisitionActions(row) {
    const actions = [`<button onclick="showRequisitionDetail(${row.requisitionId})">详情</button>`];
    if (row.requestStatus === "CREATED") {
        actions.push(`<button onclick="approveRequisition(${row.requisitionId})">审核</button>`);
    }
    return actions.join("");
}

function renderPickingActions(row) {
    const actions = [`<button onclick="showPickingDetail(${row.pickingTaskId})">详情</button>`];
    if (row.taskStatus === "CREATED") {
        actions.push(`<button onclick="completePicking(${row.pickingTaskId})">完成</button>`);
    }
    return actions.join("");
}

function renderDeliveryActions(row) {
    const actions = [`<button onclick="showDeliveryDetail(${row.deliveryTaskId})">详情</button>`];
    if (row.deliveryStatus === "PENDING") {
        actions.push(`<button onclick="arriveDelivery(${row.deliveryTaskId})">到达</button>`);
    }
    if (row.deliveryStatus === "ARRIVED") {
        actions.push(`<button onclick="confirmReceipt(${row.deliveryTaskId})">收料</button>`);
    }
    return actions.join("");
}

async function showMaterialDetail(id) {
    try {
        renderDetail("warehouseDetail", await getJson(`/materials/${id}`), "物料详情");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function showWarehouseDetail(id) {
    try {
        renderDetail("warehouseDetail", await getJson(`/warehouses/${id}`), "仓库详情");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function showLocationDetail(id) {
    try {
        renderDetail("warehouseDetail", await getJson(`/warehouses/locations/${id}`), "库位详情");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function showInventoryDetail(id) {
    try {
        renderDetail("warehouseDetail", await getJson(`/inventory/${id}`), "库存详情");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function showRobotDetail(id) {
    try {
        renderDetail("warehouseDetail", await getJson(`/robots/${id}`), "机器人详情");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function showRequisitionDetail(id) {
    try {
        renderDetail("warehouseDetail", await getJson(`/requisitions/${id}`), "领料任务详情");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function showPickingDetail(id) {
    try {
        renderDetail("warehouseDetail", await getJson(`/picking-tasks/${id}`), "拣货任务详情");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function showDeliveryDetail(id) {
    try {
        renderDetail("warehouseDetail", await getJson(`/robot-delivery-tasks/${id}`), "配送任务详情");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function showTransactionDetail(id) {
    try {
        renderDetail("warehouseDetail", await getJson(`/inventory/transactions/${id}`), "库存流水详情");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function seedWarehouse() {
    try {
        const suffix = Date.now();
        const batchNo = `BATCH-DEMO-${suffix}`;
        const material = await postJson("/materials", {
            materialCode: `MAT-DEMO-${suffix}`,
            materialName: "天然橡胶",
            materialType: "RAW",
            specification: "RSS3",
            unit: "kg"
        });
        const warehouse = await postJson("/warehouses", {
            warehouseCode: `WH-DEMO-${suffix}`,
            warehouseName: "原材料仓",
            warehouseType: "RAW"
        });
        const location = await postJson("/warehouses/locations", {
            warehouseId: warehouse.warehouseId,
            locationCode: `LOC-DEMO-${suffix}`,
            locationName: "A-01"
        });
        await postJson("/inventory", {
            materialId: material.materialId,
            warehouseId: warehouse.warehouseId,
            locationId: location.locationId,
            batchNo,
            availableQty: 500
        });
        await postJson("/robots", {
            robotCode: `ROB-DEMO-${suffix}`,
            robotName: "配送机器人一号",
            batteryLevel: 88,
            currentLocation: "原材料仓"
        });
        fillInventory(material.materialId, batchNo);
        renderDetail("warehouseDetail", { material, warehouse, location }, "新建演示数据");
        showMessage("演示数据已生成");
        await refreshWarehouse();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function approveRequisition(id) {
    try {
        await postJson(`/requisitions/${id}/approve?approvedBy=1`);
        showMessage("领料已审核");
        await refreshWarehouse();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function completePicking(id) {
    try {
        await postJson(`/picking-tasks/${id}/complete`);
        showMessage("拣货已完成");
        await refreshWarehouse();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function arriveDelivery(id) {
    try {
        await postJson(`/robot-delivery-tasks/${id}/arrive`);
        showMessage("配送已到达，等待确认收料");
        await refreshWarehouse();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function confirmReceipt(id) {
    try {
        await postJson(`/robot-delivery-tasks/${id}/confirm-receipt`);
        showMessage("收料已确认，库存已扣减");
        await refreshWarehouse();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

document.getElementById("seedWarehouse").addEventListener("click", seedWarehouse);
document.getElementById("refreshWarehouse").addEventListener("click", () => refreshWarehouse().catch(error => showMessage(error.message, "error")));
document.getElementById("requisitionForm").addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.target);
    try {
        await postJson("/requisitions", {
            workOrderId: Number(form.get("workOrderId")),
            requestedBy: 1,
            items: [{
                materialId: Number(form.get("materialId")),
                requiredQty: Number(form.get("requiredQty")),
                unit: "kg",
                batchNo: String(form.get("batchNo"))
            }]
        });
        showMessage("领料任务已创建");
        await refreshWarehouse();
    } catch (error) {
        showMessage(error.message, "error");
    }
});

refreshWarehouse().catch(() => {});
