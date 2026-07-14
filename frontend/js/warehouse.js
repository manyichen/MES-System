let warehouseCache = { materials: [], warehouses: [], inventory: [], workOrders: [], shortageAlerts: [] };

async function refreshWarehouse(options = {}) {
    try {
        const [materials, warehouses, locations, inventory, robots, requisitions, pickingTasks, deliveryTasks, transactions, workOrders, shortageAlerts] = await Promise.all([
            getJson("/materials"),
            getJson("/warehouses"),
            getJson("/warehouses/locations"),
            getJson("/inventory"),
            getJson("/robots"),
            getJson("/requisitions"),
            getJson("/picking-tasks"),
            getJson("/robot-delivery-tasks"),
            getJson("/inventory/transactions"),
            getJson("/work-orders").catch(() => []),
            getJson("/shortage-alerts").catch(() => [])
        ]);
        warehouseCache = { materials, warehouses, inventory, workOrders, shortageAlerts };
        renderRequisitionSelectors();
        renderWarehouseTables({ materials, warehouses, locations, inventory, robots, requisitions, pickingTasks, deliveryTasks, transactions, shortageAlerts });
        if (options.notify) showMessage("\u4ed3\u50a8\u7269\u6d41\u6570\u636e\u5df2\u5237\u65b0", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function renderRequisitionSelectors() {
    replaceWarehouseInput("workOrderId", warehouseCache.workOrders.filter(item => ["DISPATCHED", "RECEIVED", "RUNNING"].includes(item.workOrderStatus)),
        "workOrderId", item => `${item.workOrderNo || "\u5de5\u5355"} / ${statusText(item.workOrderStatus || "")} / ID ${item.workOrderId}`);
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
    const requisitions = sortRequisitionsForReview(data.requisitions);
    const rejectedRequisitions = data.requisitions.filter(row => row.requestStatus === "REJECTED");
    const visibleRejectedRequisitions = rejectedRequisitions.slice(0, 8);
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
    renderTable("requisitionTable", requisitions, [
        { title: "ID", key: "requisitionId" },
        { title: "\u7f16\u53f7", key: "requisitionNo" },
        { title: "\u5de5\u5355", key: "workOrderId" },
        { title: "\u4ed3\u5e93", key: "warehouseId" },
        { title: "\u72b6\u6001", render: renderRequisitionStatus },
        { title: "\u64cd\u4f5c", render: renderRequisitionActions }
    ]);
    renderTable("rejectedRequisitionTable", visibleRejectedRequisitions, [
        { title: "ID", key: "requisitionId" },
        { title: "\u7f16\u53f7", key: "requisitionNo" },
        { title: "\u5de5\u5355", key: "workOrderId" },
        { title: "\u4ed3\u5e93", key: "warehouseId" },
        { title: "\u72b6\u6001", render: renderRequisitionStatus },
        { title: "\u64cd\u4f5c", render: renderRejectedRequisitionActions }
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
    ]);    renderTable("warehouseShortageAlertTable", data.shortageAlerts, [
        { title: "预警编号", key: "alertNo" },
        { title: "生产任务", key: "taskId" },
        { title: "缺料物料", key: "materialName" },
        { title: "需求 / 可用 / 缺口", render: row => `${row.requiredQty ?? 0} / ${row.availableQty ?? 0} / ${row.shortageQty ?? 0}` },
        { title: "状态", key: "alertStatus" },
        { title: "操作", render: row => row.alertStatus === "OPEN" && hasPermission("warehouse.inventory.adjust") ? `<button onclick="acceptShortageAlert(${row.alertId})">接收并备料</button>` : (row.alertStatus === "ACCEPTED" ? "已接收，待补充库存" : "-") }
    ]);
    updateWarehouseSectionCounts(data, { rejectedRequisitions });
}

async function acceptShortageAlert(alertId) {
    try {
        await postJson(`/shortage-alerts/${alertId}/accept`);
        showMessage("缺料预警已接收。请录入实际到货或备料库存，随后通知 PMC 重新执行齐套分析。", "ok");
        await refreshWarehouse();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
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
    const steps = [
        `<button type="button" onclick="scrollWarehouseSection('requisitionTable')"><strong>${req}</strong><span>\u5f85\u5ba1\u6838\u9886\u6599</span></button>`
    ];
    if (!hasRole("WORKSHOP_MANAGER")) {
        steps.push(
            `<button type="button" onclick="scrollWarehouseSection('pickingTable')"><strong>${pick}</strong><span>\u5f85\u5b8c\u6210\u62e3\u8d27</span></button>`,
            `<button type="button" onclick="scrollWarehouseSection('deliveryTable')"><strong>${arrived}</strong><span>\u5f85\u786e\u8ba4\u6536\u6599</span></button>`,
            `<button type="button" onclick="scrollWarehouseSection('deliveryTable')"><strong>${pending}</strong><span>\u914d\u9001\u5728\u9014</span></button>`
        );
    }
    focus.innerHTML = `
        <h3>\u5f53\u524d\u5148\u5904\u7406\u8fd9\u4e9b</h3>
        <div class="workflow-steps">
            ${steps.join("")}
        </div>`;
}

function scrollWarehouseSection(id) {
    document.getElementById(id)?.closest(".tool")?.scrollIntoView({ behavior: "smooth", block: "center" });
    showMessage("\u5df2\u5b9a\u4f4d\u5230\u5bf9\u5e94\u64cd\u4f5c\u533a", "ok");
}

function updateWarehouseSectionCounts(data, derived = {}) {
    const rejectedRequisitionCount = derived.rejectedRequisitions?.length ?? data.requisitions.filter(row => row.requestStatus === "REJECTED").length;
    setToolCount("materialTable", data.materials.length, 5);
    setToolCount("warehouseTable", data.warehouses.length, 5);
    setToolCount("locationTable", data.locations.length, 5);
    setToolCount("inventoryTable", data.inventory.length, 5);
    setToolCount("robotTable", data.robots.length, 5);
    setToolCount("requisitionTable", data.requisitions.length, data.requisitions.length);
    setToolCount("rejectedRequisitionTable", rejectedRequisitionCount, 8);
    setToolCount("pickingTable", data.pickingTasks.length, 8);
    setToolCount("deliveryTable", data.deliveryTasks.length, 8);
    setToolCount("transactionTable", data.transactions.length, 8);
}

function sortRequisitionsForReview(rows) {
    const rank = row => row.requestStatus === "CREATED" ? 0 : row.requestStatus === "REJECTED" ? 2 : 1;
    return [...rows].sort((left, right) => {
        const rankDiff = rank(left) - rank(right);
        if (rankDiff !== 0) return rankDiff;
        return Number(left.requisitionId || 0) - Number(right.requisitionId || 0);
    });
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

function renderRequisitionStatus(row) {
    const status = row.requestStatus || "";
    const label = status === "CREATED" ? "\u5f85\u5ba1\u6838" : statusText(status);
    return `<span class="status status-${escapeHtml(status.toLowerCase())}">${escapeHtml(label)}</span>`;
}

function renderRequisitionActions(row) {
    const label = row.requestStatus === "CREATED" ? "\u5ba1\u6838" : "\u8be6\u60c5";
    return `<button onclick="openRequisitionReview(${row.requisitionId})">${label}</button>`;
}

function renderRejectedRequisitionActions(row) {
    return renderRequisitionActions(row);
}

function formatWarehouseDateTime(value) {
    if (!value) return "-";
    return String(value).replace("T", " ").slice(0, 19);
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
function showRequisitionDetail(id) { return openRequisitionReview(id); }
function showPickingDetail(id) { return showWarehouseResource(`/picking-tasks/${id}`, "\u62e3\u8d27\u4efb\u52a1\u8be6\u60c5"); }
function showDeliveryDetail(id) { return showWarehouseResource(`/robot-delivery-tasks/${id}`, "\u914d\u9001\u4efb\u52a1\u8be6\u60c5"); }
function showTransactionDetail(id) { return showWarehouseResource(`/inventory/transactions/${id}`, "\u5e93\u5b58\u6d41\u6c34\u8be6\u60c5"); }

async function showRejectReason(id) {
    try {
        const requisition = await getJson(`/requisitions/${id}`);
        const reason = requisition.remark || "\u6682\u672a\u586b\u5199\u9a73\u56de\u539f\u56e0";
        const mask = document.createElement("div");
        mask.className = "modal-mask";
        mask.innerHTML = `
            <div class="modal-card requisition-reject-modal">
                <h3>\u9a73\u56de\u539f\u56e0</h3>
                <div class="review-summary">
                    <div><span>\u9886\u6599\u5355</span><strong>${escapeHtml(requisition.requisitionNo || requisition.requisitionId)}</strong></div>
                    <div><span>\u72b6\u6001</span><strong>${escapeHtml(statusText(requisition.requestStatus || ""))}</strong></div>
                </div>
                <p class="reject-reason-text">${escapeHtml(reason)}</p>
                <div class="modal-actions">
                    <button type="button" id="rejectReasonClose">\u5173\u95ed</button>
                </div>
            </div>`;
        document.body.appendChild(mask);
        mask.querySelector("#rejectReasonClose").addEventListener("click", () => mask.remove());
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function openRequisitionReview(id) {
    try {
        const requisition = await getJson(`/requisitions/${id}`);
        showRequisitionReviewDialog(requisition);
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function showRequisitionReviewDialog(requisition) {
    const itemRows = reviewItemRows(requisition);
    const checks = buildRequisitionReviewChecks(requisition, itemRows);
    const canApprove = checks.filter(item => item.blocking).every(item => item.pass);
    const isCreated = requisition.requestStatus === "CREATED";
    const canReview = isCreated && hasPermission("warehouse.requisition.approve");
    const remark = requisition.remark ? `<h4>\u5907\u6ce8 / \u9a73\u56de\u539f\u56e0</h4><p class="reject-reason-text">${escapeHtml(requisition.remark)}</p>` : "";
    const reviewChecks = canReview ? `
            <h4>\u5ba1\u6838\u5224\u65ad</h4>
            <div class="review-checks">
                ${checks.map(item => `<div class="${item.pass ? "pass" : "fail"}"><strong>${item.pass ? "\u901a\u8fc7" : "\u9a73\u56de"}</strong><span>${escapeHtml(item.label)}</span><small>${escapeHtml(item.detail)}</small></div>`).join("")}
            </div>` : "";
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal-card requisition-review-modal">
            <h3>${canReview ? "\u9886\u6599\u5ba1\u6838" : "\u9886\u6599\u8be6\u60c5"}</h3>
            <div class="review-summary">
                <div><span>\u9886\u6599\u5355</span><strong>${escapeHtml(requisition.requisitionNo || requisition.requisitionId)}</strong></div>
                <div><span>\u5de5\u5355</span><strong>${escapeHtml(requisition.workOrderId)}</strong></div>
                <div><span>\u4ed3\u5e93</span><strong>${escapeHtml(warehouseName(requisition.warehouseId))}</strong></div>
                <div><span>\u72b6\u6001</span><strong>${renderRequisitionStatus(requisition)}</strong></div>
                <div><span>\u7533\u8bf7\u4eba</span><strong>${escapeHtml(requisition.requestedBy || "-")}</strong></div>
                <div><span>\u7533\u8bf7\u65f6\u95f4</span><strong>${escapeHtml(formatWarehouseDateTime(requisition.requestTime))}</strong></div>
                <div><span>\u5ba1\u6838\u4eba</span><strong>${escapeHtml(requisition.approvedBy || "-")}</strong></div>
                <div><span>\u5ba1\u6838\u65f6\u95f4</span><strong>${escapeHtml(formatWarehouseDateTime(requisition.approvedTime))}</strong></div>
            </div>
            ${reviewChecks}
            <h4>\u7269\u6599\u660e\u7ec6</h4>
            <div class="review-items">
                ${itemRows.map(item => `<div class="${item.enough ? "pass" : "fail"}"><span>${escapeHtml(item.name)}</span><strong>\u9700\u6c42 ${escapeHtml(item.required)} / \u53ef\u7528 ${escapeHtml(item.available)}</strong><small>${escapeHtml(item.batch)}</small></div>`).join("") || `<p>\u6682\u65e0\u660e\u7ec6</p>`}
            </div>
            ${remark}
            ${canReview ? `<label>\u9a73\u56de\u539f\u56e0<textarea id="requisitionRejectReason" rows="3" placeholder="\u586b\u5199\u539f\u56e0\uff0c\u4fbf\u4e8e\u7533\u8bf7\u4eba\u4fee\u6b63"></textarea></label>` : ""}
            <div class="modal-actions">
                <button type="button" id="reviewCancel">\u53d6\u6d88</button>
                ${canReview ? `<button type="button" id="reviewReject">\u9a73\u56de</button>
                <button type="button" id="reviewApprove" ${canApprove ? "" : "disabled"}>\u5ba1\u6838\u901a\u8fc7</button>` : ""}
            </div>
        </div>`;
    document.body.appendChild(mask);
    mask.querySelector("#reviewCancel").addEventListener("click", () => mask.remove());
    mask.querySelector("#reviewReject")?.addEventListener("click", async () => {
        await rejectRequisition(requisition.requisitionId, mask.querySelector("#requisitionRejectReason").value);
        mask.remove();
    });
    mask.querySelector("#reviewApprove")?.addEventListener("click", async () => {
        await approveRequisition(requisition.requisitionId);
        mask.remove();
    });
}

function buildRequisitionReviewChecks(requisition, itemRows) {
    const workOrder = warehouseCache.workOrders.find(item => Number(item.workOrderId) === Number(requisition.workOrderId));
    return [
        {
            pass: requisition.requestStatus === "CREATED",
            blocking: true,
            label: "\u5355\u636e\u72b6\u6001",
            detail: requisition.requestStatus === "CREATED" ? "\u5f85\u5904\u7406\uff0c\u53ef\u5ba1\u6838" : `\u5f53\u524d\u72b6\u6001\uff1a${statusText(requisition.requestStatus || "")}`
        },
        {
            pass: Boolean(requisition.warehouseId),
            blocking: true,
            label: "\u76ee\u6807\u4ed3\u5e93",
            detail: requisition.warehouseId ? warehouseName(requisition.warehouseId) : "\u672a\u6307\u5b9a\u4ed3\u5e93"
        },
        {
            pass: !workOrder || ["DISPATCHED", "RECEIVED", "RUNNING"].includes(workOrder.workOrderStatus),
            blocking: false,
            label: "\u5de5\u5355\u72b6\u6001",
            detail: workOrder ? `${workOrder.workOrderNo || requisition.workOrderId} / ${statusText(workOrder.workOrderStatus || "")}` : "\u672a\u8bfb\u53d6\u5230\u5de5\u5355\u4fe1\u606f\uff0c\u4ec5\u4f9b\u53c2\u8003"
        },
        {
            pass: itemRows.length > 0,
            blocking: true,
            label: "\u9886\u6599\u660e\u7ec6",
            detail: itemRows.length ? `\u5171 ${itemRows.length} \u9879\u7269\u6599` : "\u6ca1\u6709\u7269\u6599\u660e\u7ec6"
        },
        {
            pass: itemRows.length > 0 && itemRows.every(item => item.enough),
            blocking: true,
            label: "\u5e93\u5b58\u53ef\u7528\u91cf",
            detail: itemRows.length && itemRows.every(item => item.enough) ? "\u6240\u6709\u7269\u6599\u53ef\u7528\u91cf\u6ee1\u8db3\u9700\u6c42" : "\u5b58\u5728\u53ef\u7528\u91cf\u4e0d\u8db3\u7684\u7269\u6599"
        }
    ];
}

function reviewItemRows(requisition) {
    return (requisition.items || []).map(item => {
        const required = Number(item.requiredQty) || 0;
        const available = warehouseCache.inventory
            .filter(inv => Number(inv.warehouseId) === Number(requisition.warehouseId))
            .filter(inv => Number(inv.materialId) === Number(item.materialId))
            .filter(inv => !item.batchNo || inv.batchNo === item.batchNo)
            .reduce((sum, inv) => sum + (Number(inv.availableQty) || 0), 0);
        return {
            name: materialName(item.materialId),
            required,
            available,
            enough: available >= required,
            batch: item.batchNo ? `\u6279\u6b21\uff1a${item.batchNo}` : "\u4e0d\u9650\u6279\u6b21"
        };
    });
}

function materialName(id) {
    const material = warehouseCache.materials.find(item => Number(item.materialId) === Number(id));
    return material ? `${material.materialName || material.materialCode} / ID ${id}` : `\u7269\u6599 ID ${id}`;
}

function warehouseName(id) {
    const warehouse = warehouseCache.warehouses.find(item => Number(item.warehouseId) === Number(id));
    return warehouse ? `${warehouse.warehouseName || warehouse.warehouseCode} / ID ${id}` : `ID ${id}`;
}

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

async function rejectRequisition(id, reason) {
    try {
        await postJson(`/requisitions/${id}/reject`, { remark: reason || "\u5ba1\u6838\u9a73\u56de" });
        showMessage("\u9886\u6599\u4efb\u52a1\u5df2\u9a73\u56de", "ok");
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
    applyWorkshopWarehouseVisibility(panel);
    panel.querySelector("h2").textContent = "\u4ed3\u50a8\u7269\u6d41";
    document.getElementById("seedWarehouse").textContent = "\u751f\u6210\u6f14\u793a\u6570\u636e";
    document.getElementById("refreshWarehouse").textContent = "\u5237\u65b0";
    const titles = panel.querySelectorAll(".tool > h3");
    const names = ["\u57fa\u7840\u6570\u636e", "\u521b\u5efa\u9886\u6599\u4efb\u52a1", "\u9886\u6599\u4efb\u52a1", "\u5df2\u9a73\u56de\u9886\u6599\u4efb\u52a1", "\u62e3\u8d27\u4efb\u52a1", "\u914d\u9001\u4efb\u52a1", "\u5e93\u5b58\u6d41\u6c34", "\u4ed3\u50a8\u8be6\u60c5"];
    titles.forEach((title, index) => {
        if (names[index]) title.textContent = names[index];
    });
    document.querySelector("#requisitionForm button[type='submit']").textContent = "\u521b\u5efa\u9886\u6599";
    organizeWarehouseLayout();
}

relabelWarehouseStaticText();

function applyWorkshopWarehouseVisibility(panel = document.getElementById("warehouse")) {
    if (!panel || typeof hasRole !== "function" || !hasRole("WORKSHOP_MANAGER")) return;
    [
        "materialTable",
        "pickingTable",
        "deliveryTable",
        "transactionTable"
    ].forEach(tableId => {
        const section = document.getElementById(tableId)?.closest(".tool");
        section?.classList.add("permission-hidden");
    });
}

function prepareWarehouseForSession() {
    applyWorkshopWarehouseVisibility();
}

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
