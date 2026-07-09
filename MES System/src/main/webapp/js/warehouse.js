async function refreshWarehouse() {
    const [requisitions, pickingTasks, deliveryTasks, transactions] = await Promise.all([
        getJson("/requisitions"),
        getJson("/picking-tasks"),
        getJson("/robot-delivery-tasks"),
        getJson("/inventory/transactions")
    ]);
    renderTable("requisitionTable", requisitions, [
        { title: "ID", key: "requisitionId" },
        { title: "编号", key: "requisitionNo" },
        { title: "工单", key: "workOrderId" },
        { title: "状态", key: "requestStatus" },
        { title: "操作", render: row => `<button onclick="approveRequisition(${row.requisitionId})">审核</button>` }
    ]);
    renderTable("pickingTable", pickingTasks, [
        { title: "ID", key: "pickingTaskId" },
        { title: "编号", key: "pickingTaskNo" },
        { title: "领料", key: "requisitionId" },
        { title: "状态", key: "taskStatus" },
        { title: "操作", render: row => `<button onclick="completePicking(${row.pickingTaskId})">完成</button>` }
    ]);
    renderTable("deliveryTable", deliveryTasks, [
        { title: "ID", key: "deliveryTaskId" },
        { title: "编号", key: "deliveryTaskNo" },
        { title: "机器人", key: "robotId" },
        { title: "状态", key: "deliveryStatus" },
        { title: "操作", render: row => `<button onclick="arriveDelivery(${row.deliveryTaskId})">到达</button>` }
    ]);
    renderTable("transactionTable", transactions, [
        { title: "编号", key: "transactionNo" },
        { title: "物料", key: "materialId" },
        { title: "类型", key: "transactionType" },
        { title: "数量", key: "qty" },
        { title: "来源", key: "sourceDocType" }
    ]);
}

async function seedWarehouse() {
    try {
        const material = await postJson("/materials", {
            materialName: "天然橡胶",
            materialType: "RAW",
            specification: "RSS3",
            unit: "kg"
        });
        const warehouse = await postJson("/warehouses", {
            warehouseName: "原材料仓",
            warehouseType: "RAW"
        });
        await postJson("/warehouses/locations", {
            warehouseId: warehouse.warehouseId,
            locationName: "A-01"
        });
        await postJson("/inventory", {
            materialId: material.materialId,
            warehouseId: warehouse.warehouseId,
            batchNo: "BATCH-20260709-001",
            availableQty: 500
        });
        await postJson("/robots", {
            robotName: "配送机器人一号",
            batteryLevel: 88,
            currentLocation: "原材料仓"
        });
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
        showMessage("配送已到达，库存已扣减");
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
                batchNo: "BATCH-20260709-001"
            }]
        });
        showMessage("领料任务已创建");
        await refreshWarehouse();
    } catch (error) {
        showMessage(error.message, "error");
    }
});

refreshWarehouse().catch(() => {});
