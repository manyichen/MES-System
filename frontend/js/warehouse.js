let warehouseCache = { materials: [], warehouses: [], inventory: [], workOrders: [] };

async function refreshWarehouse(options = {}) {
    try {
        const [materials, warehouses, locations, inventory, robots, requisitions, pickingTasks, deliveryTasks, transactions, workOrders] = await Promise.all([
            getJson("/materials"),
            getJson("/warehouses"),
            getJson("/warehouses/locations"),
            getJson("/inventory"),
            getJson("/robots"),
            getJson("/requisitions"),
            getJson("/picking-tasks"),
            getJson("/robot-delivery-tasks"),
            getJson("/inventory/transactions"),
            getJson("/work-orders").catch(() => [])
        ]);
        warehouseCache = { materials, warehouses, inventory, workOrders };
        renderRequisitionSelectors();
        renderWarehouseTables({ materials, warehouses, locations, inventory, robots, requisitions, pickingTasks, deliveryTasks, transactions });
        if (options.notify) showMessage("\u4ed3\u50a8\u7269\u6d41\u6570\u636e\u5df2\u5237\u65b0", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function renderRequisitionSelectors() {
    replaceWarehouseInput("workOrderId", warehouseCache.workOrders.filter(item => ["DISPATCHED", "RECEIVED", "RUNNING"].includes(item.workOrderStatus)),
        "workOrderId", item => `${item.workOrderNo || "\u5de5\u5355"} / ${item.workOrderStatus} / ID ${item.workOrderId}`);
    replaceWarehouseInput("warehouseId", warehouseCache.warehouses,
        "warehouseId", item => `${item.warehouseName || item.warehouseCode || "\u4ed3\u5e93"} / ID ${item.warehouseId}`);
    replaceWarehouseInput("materialId", warehouseCache.materials,
        "materialId", item => `${item.materialName || item.materialCode || "\u7269\u6599"} / ID ${item.materialId}`);
}

function replaceWarehouseInput(name, rows, valueKey, labelFn) {
    const current = document.querySelector(`#requisitionForm [name='${name}']`);
    if (!current || current.tagName === "SELECT") return;
    const select = document.createElement("select");
    select.name = name;
    select.required = current.required;
    if (!rows.length) {
        const option = document.createElement("option");
        option.value = "";
        option.textContent = "\u6682\u65e0\u53ef\u9009\u6570\u636e";
        select.appendChild(option);
    }
    rows.forEach(row => {
        const option = document.createElement("option");
        option.value = row[valueKey];
        option.textContent = labelFn(row);
        select.appendChild(option);
    });
    current.replaceWith(select);
}

function renderWarehouseTables(data) {
    const visibleMaterials = data.materials.slice(0, 5);
    const visibleWarehouses = data.warehouses.slice(0, 5);
    const visibleLocations = data.locations.slice(0, 5);
    const visibleInventory = data.inventory.slice(0, 5);
    const visibleRobots = prioritizeRows(data.robots, row => ["IDLE", "WORKING", "CHARGING"].includes(row.robotStatus), 5);
    const visibleRequisitions = prioritizeRows(data.requisitions, row => row.requestStatus === "CREATED", 8);
    const visiblePicking = prioritizeRows(data.pickingTasks, row => row.taskStatus === "CREATED", 8);
    const visibleDelivery = prioritizeRows(data.deliveryTasks, row => ["PENDING", "ARRIVED"].includes(row.deliveryStatus), 8);
    const visibleTransactions = data.transactions.slice(0, 8);

    renderWarehouseFocus(data);
    renderTable("materialTable", visibleMaterials, [
        { title: "ID", key: "materialId" },
        { title: "\u7f16\u53f7", key: "materialCode" },
        { title: "\u540d\u79f0", key: "materialName" },
        { title: "\u5355\u4f4d", key: "unit" },
        { title: "\u64cd\u4f5c", render: row => `<button onclick="showMaterialDetail(${row.materialId})">\u8be6\u60c5</button>${hasPermission("warehouse.requisition.create") ? `<button onclick="fillMaterial(${row.materialId})">\u4f7f\u7528</button>` : ""}` }
    ]);
    renderTable("warehouseTable", visibleWarehouses, [
        { title: "ID", key: "warehouseId" },
        { title: "\u7f16\u53f7", key: "warehouseCode" },
        { title: "\u540d\u79f0", key: "warehouseName" },
        { title: "\u7c7b\u578b", key: "warehouseType" },
        { title: "\u64cd\u4f5c", render: row => `<button onclick="showWarehouseDetail(${row.warehouseId})">\u8be6\u60c5</button>` }
    ]);
    renderTable("locationTable", visibleLocations, [
        { title: "ID", key: "locationId" },
        { title: "\u4ed3\u5e93", key: "warehouseId" },
        { title: "\u7f16\u53f7", key: "locationCode" },
        { title: "\u540d\u79f0", key: "locationName" },
        { title: "\u64cd\u4f5c", render: row => `<button onclick="showLocationDetail(${row.locationId})">\u8be6\u60c5</button>` }
    ]);
    renderTable("inventoryTable", visibleInventory, [
        { title: "ID", key: "inventoryId" },
        { title: "\u7269\u6599", key: "materialId" },
        { title: "\u6279\u6b21", key: "batchNo" },
        { title: "\u53ef\u7528", key: "availableQty" },
        { title: "\u72b6\u6001", key: "qualityStatus" },
        { title: "\u64cd\u4f5c", render: row => `<button onclick="showInventoryDetail(${row.inventoryId})">\u8be6\u60c5</button>${hasPermission("warehouse.requisition.create") ? `<button onclick="fillInventory(${row.materialId}, '${escapeJs(row.batchNo)}')">\u4f7f\u7528</button>` : ""}` }
    ]);
    renderTable("robotTable", visibleRobots, [
        { title: "ID", key: "robotId" },
        { title: "\u7f16\u53f7", key: "robotCode" },
        { title: "\u540d\u79f0", key: "robotName" },
        { title: "\u4ed3\u5e93", key: "warehouseId" },
        { title: "\u72b6\u6001", key: "robotStatus" },
        { title: "\u7535\u91cf", key: "batteryLevel" },
        { title: "\u64cd\u4f5c", render: row => `<button onclick="showRobotDetail(${row.robotId})">\u8be6\u60c5</button>` }
    ]);
    renderTable("requisitionTable", visibleRequisitions, [
        { title: "ID", key: "requisitionId" },
        { title: "\u7f16\u53f7", key: "requisitionNo" },
        { title: "\u5de5\u5355", key: "workOrderId" },
        { title: "\u4ed3\u5e93", key: "warehouseId" },
        { title: "\u72b6\u6001", key: "requestStatus" },
        { title: "\u64cd\u4f5c", render: renderRequisitionActions }
    ]);
    renderTable("pickingTable", visiblePicking, [
        { title: "ID", key: "pickingTaskId" },
        { title: "\u7f16\u53f7", key: "pickingTaskNo" },
        { title: "\u9886\u6599", key: "requisitionId" },
        { title: "\u72b6\u6001", key: "taskStatus" },
        { title: "\u64cd\u4f5c", render: renderPickingActions }
    ]);
    renderTable("deliveryTable", visibleDelivery, [
        { title: "ID", key: "deliveryTaskId" },
        { title: "\u7f16\u53f7", key: "deliveryTaskNo" },
        { title: "\u673a\u5668\u4eba", key: "robotId" },
        { title: "\u72b6\u6001", key: "deliveryStatus" },
        { title: "\u64cd\u4f5c", render: renderDeliveryActions }
    ]);
    renderTable("transactionTable", visibleTransactions, [
        { title: "ID", key: "transactionId" },
        { title: "\u7f16\u53f7", key: "transactionNo" },
        { title: "\u7269\u6599", key: "materialId" },
        { title: "\u7c7b\u578b", key: "transactionType" },
        { title: "\u6570\u91cf", key: "qty" },
        { title: "\u6765\u6e90", key: "sourceDocType" },
        { title: "\u64cd\u4f5c", render: row => `<button onclick="showTransactionDetail(${row.transactionId})">\u8be6\u60c5</button>` }
    ]);
    updateWarehouseSectionCounts(data);
}

function prioritizeRows(rows, predicate, limit) {
    return [...rows]
        .sort((a, b) => Number(Boolean(predicate(b))) - Number(Boolean(predicate(a))))
        .slice(0, limit);
}

function renderWarehouseFocus(data) {
    const grid = document.querySelector("#warehouse .grid");
    if (!grid) return;
    let focus = document.getElementById("warehouseFocus");
    if (!focus) {
        focus = document.createElement("div");
        focus.id = "warehouseFocus";
        focus.className = "tool wide workflow-focus";
        grid.prepend(focus);
    }
    const req = data.requisitions.filter(row => row.requestStatus === "CREATED").length;
    const pick = data.pickingTasks.filter(row => row.taskStatus === "CREATED").length;
    const arrived = data.deliveryTasks.filter(row => row.deliveryStatus === "ARRIVED").length;
    const pending = data.deliveryTasks.filter(row => row.deliveryStatus === "PENDING").length;
    focus.innerHTML = `
        <h3>\u5f53\u524d\u5148\u5904\u7406\u8fd9\u4e9b</h3>
        <div class="workflow-steps">
            <button type="button" onclick="scrollWarehouseSection('requisitionTable')"><strong>${req}</strong><span>\u5f85\u5ba1\u6838\u9886\u6599</span></button>
            <button type="button" onclick="scrollWarehouseSection('pickingTable')"><strong>${pick}</strong><span>\u5f85\u5b8c\u6210\u62e3\u8d27</span></button>
            <button type="button" onclick="scrollWarehouseSection('deliveryTable')"><strong>${arrived}</strong><span>\u5f85\u786e\u8ba4\u6536\u6599</span></button>
            <button type="button" onclick="scrollWarehouseSection('deliveryTable')"><strong>${pending}</strong><span>\u914d\u9001\u5728\u9014</span></button>
        </div>`;
}

function scrollWarehouseSection(id) {
    document.getElementById(id)?.closest(".tool")?.scrollIntoView({ behavior: "smooth", block: "center" });
    showMessage("\u5df2\u5b9a\u4f4d\u5230\u5bf9\u5e94\u64cd\u4f5c\u533a", "ok");
}

function updateWarehouseSectionCounts(data) {
    setToolCount("materialTable", data.materials.length, 5);
    setToolCount("warehouseTable", data.warehouses.length, 5);
    setToolCount("locationTable", data.locations.length, 5);
    setToolCount("inventoryTable", data.inventory.length, 5);
    setToolCount("robotTable", data.robots.length, 5);
    setToolCount("requisitionTable", data.requisitions.length, 8);
    setToolCount("pickingTable", data.pickingTasks.length, 8);
    setToolCount("deliveryTable", data.deliveryTasks.length, 8);
    setToolCount("transactionTable", data.transactions.length, 8);
}

function setToolCount(tableId, total, shown) {
    const title = document.getElementById(tableId)?.closest(".tool")?.querySelector("h3, h4");
    if (!title) return;
    const base = title.dataset.baseTitle || title.textContent.replace(/\s*\(.+\)$/, "");
    title.dataset.baseTitle = base;
    title.textContent = total > shown ? `${base}\uff08\u663e\u793a ${shown} / \u5171 ${total}\uff09` : base;
}

function escapeJs(value) {
    return String(value ?? "").replaceAll("\\", "\\\\").replaceAll("'", "\\'");
}

function fillMaterial(materialId) {
    const input = document.querySelector("#requisitionForm [name='materialId']");
    if (input) input.value = materialId;
    showMessage("\u5df2\u9009\u62e9\u7269\u6599", "ok");
}

function fillInventory(materialId, batchNo) {
    const material = document.querySelector("#requisitionForm [name='materialId']");
    const batch = document.querySelector("#requisitionForm [name='batchNo']");
    if (material) material.value = materialId;
    if (batch) batch.value = batchNo;
    showMessage("\u5df2\u5e26\u5165\u7269\u6599\u548c\u6279\u6b21", "ok");
}

function renderRequisitionActions(row) {
    const actions = [`<button onclick="showRequisitionDetail(${row.requisitionId})">\u8be6\u60c5</button>`];
    if (row.requestStatus === "CREATED" && hasPermission("warehouse.requisition.approve")) {
        actions.push(`<button onclick="approveRequisition(${row.requisitionId})">\u5ba1\u6838</button>`);
    }
    return actions.join("");
}

function renderPickingActions(row) {
    const actions = [`<button onclick="showPickingDetail(${row.pickingTaskId})">\u8be6\u60c5</button>`];
    if (row.taskStatus === "CREATED" && hasPermission("warehouse.picking.execute")) {
        actions.push(`<button onclick="completePicking(${row.pickingTaskId})">\u5b8c\u6210\u62e3\u8d27</button>`);
    }
    return actions.join("");
}

function renderDeliveryActions(row) {
    const actions = [`<button onclick="showDeliveryDetail(${row.deliveryTaskId})">\u8be6\u60c5</button>`];
    if (row.deliveryStatus === "PENDING" && hasPermission("warehouse.delivery.execute")) {
        actions.push(`<button onclick="arriveDelivery(${row.deliveryTaskId})">\u5230\u8fbe</button>`);
    }
    if (row.deliveryStatus === "ARRIVED" && hasPermission("warehouse.delivery.execute")) {
        actions.push(`<button onclick="confirmReceipt(${row.deliveryTaskId})">\u6536\u6599</button>`);
    }
    return actions.join("");
}

async function showWarehouseResource(path, title) {
    try {
        renderDetail("warehouseDetail", await getJson(path), title);
        showMessage(`${title}\u5df2\u52a0\u8f7d`, "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function showMaterialDetail(id) { return showWarehouseResource(`/materials/${id}`, "\u7269\u6599\u8be6\u60c5"); }
function showWarehouseDetail(id) { return showWarehouseResource(`/warehouses/${id}`, "\u4ed3\u5e93\u8be6\u60c5"); }
function showLocationDetail(id) { return showWarehouseResource(`/warehouses/locations/${id}`, "\u5e93\u4f4d\u8be6\u60c5"); }
function showInventoryDetail(id) { return showWarehouseResource(`/inventory/${id}`, "\u5e93\u5b58\u8be6\u60c5"); }
function showRobotDetail(id) { return showWarehouseResource(`/robots/${id}`, "\u673a\u5668\u4eba\u8be6\u60c5"); }
function showRequisitionDetail(id) { return showWarehouseResource(`/requisitions/${id}`, "\u9886\u6599\u4efb\u52a1\u8be6\u60c5"); }
function showPickingDetail(id) { return showWarehouseResource(`/picking-tasks/${id}`, "\u62e3\u8d27\u4efb\u52a1\u8be6\u60c5"); }
function showDeliveryDetail(id) { return showWarehouseResource(`/robot-delivery-tasks/${id}`, "\u914d\u9001\u4efb\u52a1\u8be6\u60c5"); }
function showTransactionDetail(id) { return showWarehouseResource(`/inventory/transactions/${id}`, "\u5e93\u5b58\u6d41\u6c34\u8be6\u60c5"); }

async function seedWarehouse() {
    try {
        const suffix = Date.now();
        const batchNo = `BATCH-DEMO-${suffix}`;
        const material = await postJson("/materials", { materialCode: `MAT-DEMO-${suffix}`, materialName: "\u5929\u7136\u6a61\u80f6", materialType: "RAW", specification: "RSS3", unit: "kg" });
        const warehouse = await postJson("/warehouses", { warehouseCode: `WH-DEMO-${suffix}`, warehouseName: "\u539f\u6750\u6599\u4ed3", warehouseType: "RAW" });
        const location = await postJson("/warehouses/locations", { warehouseId: warehouse.warehouseId, locationCode: `LOC-DEMO-${suffix}`, locationName: "A-01" });
        await postJson("/inventory", { materialId: material.materialId, warehouseId: warehouse.warehouseId, locationId: location.locationId, batchNo, availableQty: 500 });
        await postJson("/robots", { robotCode: `ROB-DEMO-${suffix}`, robotName: "\u914d\u9001\u673a\u5668\u4eba\u4e00\u53f7", warehouseId: warehouse.warehouseId, batteryLevel: 88, currentLocation: "\u539f\u6750\u6599\u4ed3" });
        renderDetail("warehouseDetail", { material, warehouse, location }, "\u65b0\u5efa\u6f14\u793a\u6570\u636e");
        showMessage("\u6f14\u793a\u6570\u636e\u5df2\u751f\u6210", "ok");
        await refreshWarehouse();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function approveRequisition(id) {
    try {
        await postJson(`/requisitions/${id}/approve`);
        showMessage("\u9886\u6599\u4efb\u52a1\u5df2\u5ba1\u6838", "ok");
        await refreshWarehouse();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function completePicking(id) {
    try {
        await postJson(`/picking-tasks/${id}/complete`);
        showMessage("\u62e3\u8d27\u5df2\u5b8c\u6210\uff0c\u673a\u5668\u4eba\u914d\u9001\u4efb\u52a1\u5df2\u751f\u6210", "ok");
        await refreshWarehouse();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function arriveDelivery(id) {
    try {
        await postJson(`/robot-delivery-tasks/${id}/arrive`);
        showMessage("\u914d\u9001\u5df2\u5230\u8fbe\uff0c\u7b49\u5f85\u786e\u8ba4\u6536\u6599", "ok");
        await refreshWarehouse();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function confirmReceipt(id) {
    try {
        await postJson(`/robot-delivery-tasks/${id}/confirm-receipt`);
        showMessage("\u6536\u6599\u5df2\u786e\u8ba4\uff0c\u5e93\u5b58\u5df2\u6263\u51cf", "ok");
        await refreshWarehouse();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

document.getElementById("seedWarehouse")?.addEventListener("click", seedWarehouse);
document.getElementById("refreshWarehouse")?.addEventListener("click", () => refreshWarehouse({ notify: true }));
document.getElementById("requisitionForm")?.addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.target);
    try {
        if (!form.get("workOrderId") || !form.get("warehouseId") || !form.get("materialId")) {
            showMessage("\u8bf7\u5148\u9009\u62e9\u5de5\u5355\u3001\u4ed3\u5e93\u548c\u7269\u6599", "error");
            return;
        }
        await postJson("/requisitions", {
            workOrderId: Number(form.get("workOrderId")),
            warehouseId: Number(form.get("warehouseId")),
            requestedBy: getCurrentSession().user.userId,
            items: [{
                materialId: Number(form.get("materialId")),
                requiredQty: Number(form.get("requiredQty")),
                unit: "kg",
                batchNo: String(form.get("batchNo"))
            }]
        });
        showMessage("\u9886\u6599\u4efb\u52a1\u5df2\u521b\u5efa", "ok");
        await refreshWarehouse();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
});

function relabelWarehouseStaticText() {
    const panel = document.getElementById("warehouse");
    if (!panel) return;
    panel.querySelector("h2").textContent = "\u4ed3\u50a8\u7269\u6d41";
    document.getElementById("seedWarehouse").textContent = "\u751f\u6210\u6f14\u793a\u6570\u636e";
    document.getElementById("refreshWarehouse").textContent = "\u5237\u65b0";
    const titles = panel.querySelectorAll(".tool > h3");
    const names = ["\u57fa\u7840\u6570\u636e", "\u521b\u5efa\u9886\u6599\u4efb\u52a1", "\u9886\u6599\u4efb\u52a1", "\u62e3\u8d27\u4efb\u52a1", "\u914d\u9001\u4efb\u52a1", "\u5e93\u5b58\u6d41\u6c34", "\u4ed3\u50a8\u8be6\u60c5"];
    titles.forEach((title, index) => {
        if (names[index]) title.textContent = names[index];
    });
    document.querySelector("#requisitionForm button[type='submit']").textContent = "\u521b\u5efa\u9886\u6599";
    organizeWarehouseLayout();
}

relabelWarehouseStaticText();

function organizeWarehouseLayout() {
    const grid = document.querySelector("#warehouse .grid");
    const basic = document.querySelector("#warehouse .basic-grid")?.closest(".tool");
    const detail = document.getElementById("warehouseDetail");
    if (!grid || !basic || basic.dataset.organized === "1") return;
    basic.dataset.organized = "1";
    basic.classList.add("collapsible-basic");
    const title = basic.querySelector("h3");
    const content = basic.querySelector(".basic-grid");
    if (title && content) {
        const toggle = document.createElement("button");
        toggle.type = "button";
        toggle.className = "basic-toggle";
        toggle.textContent = "\u5c55\u5f00\u57fa\u7840\u6570\u636e";
        content.hidden = true;
        title.after(toggle);
        toggle.addEventListener("click", () => {
            content.hidden = !content.hidden;
            toggle.textContent = content.hidden ? "\u5c55\u5f00\u57fa\u7840\u6570\u636e" : "\u6536\u8d77\u57fa\u7840\u6570\u636e";
            showMessage(content.hidden ? "\u57fa\u7840\u6570\u636e\u5df2\u6536\u8d77" : "\u57fa\u7840\u6570\u636e\u5df2\u5c55\u5f00", "ok");
        });
    }
    if (detail?.nextSibling) {
        grid.insertBefore(basic, detail.nextSibling);
    } else {
        grid.appendChild(basic);
    }
}
