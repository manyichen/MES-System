let warehouseCache = { materials: [], warehouses: [], locations: [], inventory: [], workOrders: [], shortageAlerts: [], transactions: [] };
const LOW_STOCK_THRESHOLD = 1;

async function refreshWarehouse(options = {}) {
    try {
        const canReadWarehouse = hasPermission("warehouse.read");
        const canCreateRequisition = hasRole("PRODUCTION_OPERATOR") && hasPermission("warehouse.requisition.create");
        const optionalList = path => getJson(path).catch(() => []);
        const [materials, warehouses, locations, inventory, robots, requisitions, pickingTasks, deliveryTasks, transactions, workOrders, shortageAlerts] = await Promise.all([
            (canReadWarehouse || canCreateRequisition || hasPermission("master.read")) ? optionalList("/materials") : [],
            (canReadWarehouse || canCreateRequisition) ? optionalList("/warehouses") : [],
            canReadWarehouse ? optionalList("/warehouses/locations") : [],
            canReadWarehouse ? optionalList("/inventory") : [],
            canReadWarehouse ? optionalList("/robots") : [],
            canReadWarehouse ? optionalList("/requisitions") : [],
            canReadWarehouse ? optionalList("/picking-tasks") : [],
            canReadWarehouse ? optionalList("/robot-delivery-tasks") : [],
            canReadWarehouse ? optionalList("/inventory/transactions") : [],
            (hasPermission("planning.read") || hasPermission("planning.work_order.read")) ? optionalList("/work-orders") : [],
            (canReadWarehouse || hasPermission("planning.read")) ? optionalList("/shortage-alerts") : []
        ]);
        warehouseCache = { materials, warehouses, locations, inventory, workOrders, shortageAlerts, transactions };
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
    if (!current) return;
    const previousValue = current.value;
    const select = current.tagName === "SELECT" ? current : document.createElement("select");
    select.name = name;
    select.required = current.required;
    select.innerHTML = "";
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
    if ([...select.options].some(option => option.value === previousValue)) select.value = previousValue;
    if (current !== select) current.replaceWith(select);
}

function renderWarehouseTables(data) {
    const visibleMaterials = data.materials.slice(0, 5);
    const visibleWarehouses = data.warehouses.slice(0, 5);
    const visibleLocations = data.locations.slice(0, 5);
    const visibleInventory = prioritizeFinishedInventory(data.inventory, 5);
    const visibleRobots = prioritizeRows(data.robots, row => ["IDLE", "WORKING", "CHARGING"].includes(row.robotStatus), 5);
    const requisitions = sortRequisitionsForReview(data.requisitions);
    const rejectedRequisitions = data.requisitions.filter(row => row.requestStatus === "REJECTED");
    const visibleRejectedRequisitions = rejectedRequisitions.slice(0, 8);
    const visiblePicking = prioritizeRows(data.pickingTasks, row => row.taskStatus === "CREATED", 8);
    const visibleDelivery = prioritizeRows(data.deliveryTasks, row => ["PENDING", "ARRIVED"].includes(row.deliveryStatus), 8);
    const visibleTransactions = [...data.transactions]
        .sort((left, right) => Number(right.transactionId || 0) - Number(left.transactionId || 0))
        .slice(0, 8);
    const shortageAlerts = data.shortageAlerts.filter(isMeaningfulShortageAlert);
    const finishedGoodsReceipts = finishedGoodsReceiptRows(data.transactions);

    renderWarehouseFocus(data);
    renderTable("materialTable", visibleMaterials, [
        { title: "ID", key: "materialId" },
        { title: "\u7f16\u53f7", key: "materialCode" },
        { title: "\u540d\u79f0", key: "materialName" },
        { title: "\u5355\u4f4d", key: "unit" },
        { title: "\u64cd\u4f5c", render: row => `<button onclick="showMaterialDetail(${row.materialId})">\u8be6\u60c5</button>${hasRole("PRODUCTION_OPERATOR") && hasPermission("warehouse.requisition.create") ? `<button onclick="fillMaterial(${row.materialId})">\u4f7f\u7528</button>` : ""}` }
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
        { title: "\u7269\u6599", render: inventoryMaterialLabel },
        { title: "\u4ed3\u5e93", render: inventoryWarehouseLabel },
        { title: "\u5e93\u4f4d", render: inventoryLocationLabel },
        { title: "\u6279\u6b21", key: "batchNo" },
        { title: "\u53ef\u7528", key: "availableQty" },
        { title: "\u9884\u7559", key: "reservedQty" },
        { title: "\u51bb\u7ed3", key: "frozenQty" },
        { title: "\u72b6\u6001", key: "qualityStatus" },
        { title: "\u64cd\u4f5c", render: renderInventoryActions }
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
    ]);
    renderFinishedGoodsReceiptPanel(finishedGoodsReceipts);
    renderTable("warehouseShortageAlertTable", shortageAlerts, [
        { title: "预警编号", key: "alertNo" },
        { title: "生产任务", key: "taskId" },
        { title: "缺料物料", key: "materialName" },
        { title: "需求 / 可用 / 缺口", render: row => `${row.requiredQty ?? 0} / ${row.availableQty ?? 0} / ${row.shortageQty ?? 0}` },
        { title: "状态", key: "alertStatus" },
        { title: "操作", render: renderShortageAlertActions }
    ]);
    renderWarehousePurchasePanel(data.inventory, shortageAlerts);
    updateWarehouseSectionCounts(data, { rejectedRequisitions, shortageAlerts, finishedGoodsReceipts });
}

function isMeaningfulShortageAlert(row) {
    return Boolean(row?.materialId || row?.materialCode || row?.materialName)
        && Number(row?.shortageQty || 0) > 0;
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

function prioritizeFinishedInventory(rows, limit) {
    return [...rows]
        .sort((left, right) => Number(isFinishedInventory(right)) - Number(isFinishedInventory(left))
            || Number(right.inventoryId || 0) - Number(left.inventoryId || 0))
        .slice(0, limit);
}

function isFinishedInventory(row) {
    const material = findMaterial(row.materialId);
    const warehouse = findWarehouse(row.warehouseId);
    return ["FINISHED", "FG"].includes(String(material?.materialType || "").toUpperCase())
        || ["FINISHED", "FG"].includes(String(warehouse?.warehouseType || "").toUpperCase())
        || String(material?.materialCode || "").toUpperCase().startsWith("FG-");
}

function isFinishedGoodsTransaction(row) {
    return row?.transactionType === "FINISHED_GOODS_IN" || row?.sourceDocType === "QUALITY_INSPECTION";
}

function finishedGoodsReceiptRows(transactions = []) {
    return [...transactions]
        .filter(isFinishedGoodsTransaction)
        .sort((left, right) => Number(right.transactionId || 0) - Number(left.transactionId || 0));
}

function renderWarehouseFocus(data) {
    const grid = document.querySelector("#warehouse .grid");
    if (!grid) return;
    let focus = document.getElementById("warehouseFocus");
    if (hasRole("PRODUCTION_OPERATOR")) {
        focus?.remove();
        return;
    }
    if (!focus) {
        focus = document.createElement("div");
        focus.id = "warehouseFocus";
        focus.className = "tool wide workflow-focus";
        grid.prepend(focus);
    }
    const req = data.requisitions.filter(row => ["CREATED", "RECEIVED"].includes(row.requestStatus)).length;
    const pick = data.pickingTasks.filter(row => row.taskStatus === "CREATED").length;
    const arrived = data.deliveryTasks.filter(row => row.deliveryStatus === "ARRIVED").length;
    const pending = data.deliveryTasks.filter(row => row.deliveryStatus === "PENDING").length;
    const canApproveRequisition = hasRole("WAREHOUSE_ADMIN") && hasPermission("warehouse.requisition.approve");
    const requisitionLabel = canApproveRequisition ? "\u5f85\u5ba1\u6838\u9886\u6599" : "\u5f85\u5904\u7406\u9886\u6599";
    const steps = [
        `<button type="button" onclick="scrollWarehouseSection('requisitionTable')"><strong>${req}</strong><span>${requisitionLabel}</span></button>`
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

function renderFinishedGoodsReceiptPanel(rows = []) {
    const grid = document.querySelector("#warehouse .grid");
    if (!grid) return;
    let panel = document.getElementById("finishedGoodsReceiptPanel");
    if (!panel) {
        panel = document.createElement("div");
        panel.id = "finishedGoodsReceiptPanel";
        panel.className = "tool wide";
        panel.innerHTML = `<h3>成品入库</h3><div id="finishedGoodsReceiptTable"></div>`;
        const transactionSection = document.getElementById("transactionTable")?.closest(".tool");
        if (transactionSection) {
            transactionSection.before(panel);
        } else {
            grid.appendChild(panel);
        }
    }
    renderTable("finishedGoodsReceiptTable", rows.slice(0, 8), [
        { title: "流水号", key: "transactionNo" },
        { title: "成品", render: row => inventoryMaterialLabel({ materialId: row.materialId }) },
        { title: "批次", render: row => transactionInventory(row)?.batchNo || "-" },
        { title: "入库仓库", render: row => inventoryWarehouseLabel(transactionInventory(row) || {}) },
        { title: "库位", render: row => inventoryLocationLabel(transactionInventory(row) || {}) },
        { title: "数量", key: "qty" },
        { title: "来源质检单", render: row => row.sourceDocId ? `质检单 ${escapeHtml(row.sourceDocId)}` : "-" },
        { title: "入库时间", key: "createdAt" },
        { title: "操作", render: row => `<button onclick="showTransactionDetail(${row.transactionId})">详情</button>` }
    ]);
}

function scrollWarehouseSection(id) {
    document.getElementById(id)?.closest(".tool")?.scrollIntoView({ behavior: "smooth", block: "center" });
    showMessage("\u5df2\u5b9a\u4f4d\u5230\u5bf9\u5e94\u64cd\u4f5c\u533a", "ok");
}

function findMaterial(id) {
    return warehouseCache.materials.find(item => Number(item.materialId) === Number(id));
}

function findWarehouse(id) {
    return warehouseCache.warehouses.find(item => Number(item.warehouseId) === Number(id));
}

function findLocation(id) {
    return warehouseCache.locations.find(item => Number(item.locationId) === Number(id));
}

function transactionInventory(row) {
    return warehouseCache.inventory.find(item => Number(item.inventoryId) === Number(row?.inventoryId));
}

function inventoryMaterialLabel(row) {
    const material = findMaterial(row.materialId);
    if (!material) return `\u7269\u6599 ID ${row.materialId ?? "-"}`;
    return escapeHtml(`${material.materialName || material.materialCode || "\u7269\u6599"} / ${material.materialCode || "ID " + material.materialId}`);
}

function inventoryWarehouseLabel(row) {
    const warehouse = findWarehouse(row.warehouseId);
    if (!warehouse) return `\u4ed3\u5e93 ID ${row.warehouseId ?? "-"}`;
    return escapeHtml(`${warehouse.warehouseName || warehouse.warehouseCode || "\u4ed3\u5e93"} / ${warehouse.warehouseCode || "ID " + warehouse.warehouseId}`);
}

function inventoryLocationLabel(row) {
    const location = findLocation(row.locationId);
    if (!location) return `\u5e93\u4f4d ID ${row.locationId ?? "-"}`;
    return escapeHtml(`${location.locationName || location.locationCode || "\u5e93\u4f4d"} / ${location.locationCode || "ID " + location.locationId}`);
}

function buildInventoryDetail(row) {
    const material = findMaterial(row.materialId);
    const warehouse = findWarehouse(row.warehouseId);
    const location = findLocation(row.locationId);
    return {
        inventoryId: row.inventoryId,
        materialName: material?.materialName || `\u7269\u6599 ID ${row.materialId ?? "-"}`,
        materialCode: material?.materialCode || "-",
        specification: material?.specification || "-",
        unit: material?.unit || "-",
        materialId: row.materialId,
        warehouseName: warehouse?.warehouseName || `\u4ed3\u5e93 ID ${row.warehouseId ?? "-"}`,
        warehouseCode: warehouse?.warehouseCode || "-",
        warehouseId: row.warehouseId,
        locationName: location?.locationName || `\u5e93\u4f4d ID ${row.locationId ?? "-"}`,
        locationCode: location?.locationCode || "-",
        locationId: row.locationId,
        batchNo: row.batchNo,
        availableQty: row.availableQty,
        reservedQty: row.reservedQty,
        frozenQty: row.frozenQty,
        qualityStatus: row.qualityStatus,
        updatedAt: row.lastCheckTime
    };
}

function updateWarehouseSectionCounts(data, derived = {}) {
    const rejectedRequisitionCount = derived.rejectedRequisitions?.length ?? data.requisitions.filter(row => row.requestStatus === "REJECTED").length;
    const shortageAlertCount = derived.shortageAlerts?.length ?? data.shortageAlerts.filter(isMeaningfulShortageAlert).length;
    const finishedGoodsReceiptCount = derived.finishedGoodsReceipts?.length ?? finishedGoodsReceiptRows(data.transactions).length;
    setToolCount("materialTable", data.materials.length, 5);
    setToolCount("warehouseTable", data.warehouses.length, 5);
    setToolCount("locationTable", data.locations.length, 5);
    setToolCount("inventoryTable", data.inventory.length, 5);
    setToolCount("robotTable", data.robots.length, 5);
    setToolCount("requisitionTable", data.requisitions.length, data.requisitions.length);
    setToolCount("rejectedRequisitionTable", rejectedRequisitionCount, 8);
    setToolCount("pickingTable", data.pickingTasks.length, 8);
    setToolCount("deliveryTable", data.deliveryTasks.length, 8);
    setToolCount("finishedGoodsReceiptTable", finishedGoodsReceiptCount, 8);
    setToolCount("transactionTable", data.transactions.length, 8);
    setToolCount("warehouseShortageAlertTable", shortageAlertCount, shortageAlertCount);
}

function sortRequisitionsForReview(rows) {
    const rank = row => row.requestStatus === "CREATED" ? 0 : row.requestStatus === "RECEIVED" ? 1 : row.requestStatus === "REJECTED" ? 3 : 2;
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

function renderInventoryActions(row) {
    const actions = [`<button onclick="showInventoryDetail(${row.inventoryId})">\u8be6\u60c5</button>`];
    if (hasPermission("warehouse.requisition.create")) {
        actions.push(`<button onclick="fillInventory(${row.materialId}, '${escapeJs(row.batchNo)}')">\u4f7f\u7528</button>`);
    }
    if (canPurchaseMaterial()) {
        actions.push(`<button onclick="openWarehousePurchaseDialog({ materialId: ${row.materialId}, warehouseId: ${row.warehouseId}, locationId: ${row.locationId}, batchNo: '${escapeJs(row.batchNo)}', qty: ${Math.max(Number(row.availableQty || 0) <= 0 ? 1000 : Number(row.availableQty || 0), 1000)}, reason: 'inventory replenishment' })">采购补料</button>`);
    }
    return actions.join("");
}

function canPurchaseMaterial() {
    return hasPermission("warehouse.purchase.request") || hasPermission("warehouse.inventory.adjust") || hasPermission("warehouse.requisition.create");
}

function renderWarehousePurchasePanel(inventoryRows = [], shortageAlerts = []) {
    const grid = document.querySelector("#warehouse .grid");
    if (!grid || !canPurchaseMaterial()) return;
    let panel = document.getElementById("warehousePurchasePanel");
    if (!panel) {
        panel = document.createElement("div");
        panel.id = "warehousePurchasePanel";
        panel.className = "tool wide";
        panel.innerHTML = `<h3>缺料采购</h3><div id="warehousePurchaseTable"></div>`;
        const shortageSection = document.getElementById("warehouseShortageAlertTable")?.closest(".tool");
        if (shortageSection) {
            shortageSection.after(panel);
        } else {
            grid.appendChild(panel);
        }
    }
    const zeroInventoryRows = inventoryRows
        .filter(row => Number(row.availableQty || 0) <= LOW_STOCK_THRESHOLD)
        .map(row => ({
            materialId: row.materialId,
            materialName: materialName(row.materialId),
            warehouseId: row.warehouseId,
            warehouseName: warehouseName(row.warehouseId),
            locationId: row.locationId,
            batchNo: row.batchNo,
            shortageQty: 1000,
            source: "库存不足"
        }));
    const alertRows = shortageAlerts
        .filter(isMeaningfulShortageAlert)
        .map(row => ({
            materialId: row.materialId,
            materialName: row.materialName || materialName(row.materialId),
            warehouseId: defaultWarehouseForPurchase(),
            warehouseName: warehouseName(defaultWarehouseForPurchase()),
            locationId: null,
            batchNo: row.batchNo || "",
            shortageQty: Math.max(Number(row.shortageQty || 0), 1),
            source: "PMC缺料预警"
        }));
    const rows = [...alertRows, ...zeroInventoryRows].slice(0, 8);
    renderTable("warehousePurchaseTable", rows, [
        { title: "来源", key: "source" },
        { title: "物料", key: "materialName" },
        { title: "目标仓库", key: "warehouseName" },
        { title: "批次", render: row => row.batchNo || "-" },
        { title: "建议采购", key: "shortageQty" },
        { title: "操作", render: row => `<button onclick="openWarehousePurchaseDialog({ materialId: ${row.materialId}, warehouseId: ${row.warehouseId}, locationId: ${row.locationId || "null"}, batchNo: '${escapeJs(row.batchNo)}', qty: ${row.shortageQty}, reason: '${escapeJs(row.source)}' })">采购补料</button>` }
    ]);
}

function defaultWarehouseForPurchase() {
    const formWarehouse = Number(document.querySelector("#requisitionForm [name='warehouseId']")?.value || 0);
    if (formWarehouse) return formWarehouse;
    return Number(warehouseCache.warehouses[0]?.warehouseId || 0);
}

function defaultLocationForPurchase(warehouseId) {
    return Number(warehouseCache.locations.find(item => Number(item.warehouseId) === Number(warehouseId))?.locationId || 0) || null;
}

function renderShortageAlertActions(row) {
    const actions = [];
    if (row.alertStatus === "OPEN" && hasPermission("warehouse.inventory.adjust")) {
        actions.push(`<button onclick="acceptShortageAlert(${row.alertId})">接收</button>`);
    }
    if (canPurchaseMaterial()) {
        actions.push(`<button onclick="purchaseShortageAlert(${row.alertId})">采购补料</button>`);
    }
    return actions.join("") || (row.alertStatus === "ACCEPTED" ? "已接收" : "-");
}

function purchaseShortageAlert(alertId) {
    const row = warehouseCache.shortageAlerts.find(item => Number(item.alertId) === Number(alertId));
    if (!row) {
        showMessage("未找到缺料预警", "error");
        return;
    }
    openWarehousePurchaseDialog({
        materialId: row.materialId,
        warehouseId: defaultWarehouseForPurchase(),
        locationId: null,
        batchNo: row.batchNo || "",
        qty: Math.max(Number(row.shortageQty || 0), 1),
        reason: "shortage alert"
    });
}

function openWarehousePurchaseDialog(seed = {}) {
    const materialId = Number(seed.materialId || 0);
    const warehouseId = Number(seed.warehouseId || defaultWarehouseForPurchase());
    const locationId = Number(seed.locationId || defaultLocationForPurchase(warehouseId) || 0);
    if (!materialId || !warehouseId) {
        showMessage("请先选择物料和仓库", "error");
        return;
    }
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal-card requisition-review-modal">
            <h3>采购补料</h3>
            <div class="review-summary">
                <div><span>物料</span><strong>${escapeHtml(materialName(materialId))}</strong></div>
                <div><span>目标仓库</span><strong>${escapeHtml(warehouseName(warehouseId))}</strong></div>
            </div>
            <label>采购数量<input id="purchaseQty" type="number" min="0.01" step="0.01" value="${Number(seed.qty || 1000)}"></label>
            <label>批次号<input id="purchaseBatchNo" value="${escapeHtml(seed.batchNo || "")}" placeholder="不填则自动生成采购批次"></label>
            <label>原因<input id="purchaseReason" value="${escapeHtml(seed.reason || "manual replenishment")}"></label>
            <div class="modal-actions">
                <button type="button" id="purchaseCancel">取消</button>
                <button type="button" id="purchaseSubmit">调用外部采购</button>
            </div>
        </div>`;
    document.body.appendChild(mask);
    mask.querySelector("#purchaseCancel").addEventListener("click", () => mask.remove());
    mask.querySelector("#purchaseSubmit").addEventListener("click", async () => {
        await submitWarehousePurchase({
            materialId,
            warehouseId,
            locationId: locationId || null,
            qty: Number(mask.querySelector("#purchaseQty").value || 0),
            batchNo: mask.querySelector("#purchaseBatchNo").value.trim() || null,
            reason: mask.querySelector("#purchaseReason").value.trim() || "manual replenishment"
        });
        mask.remove();
    });
}

async function submitWarehousePurchase(payload) {
    try {
        if (!payload.materialId || !payload.warehouseId || !payload.qty || payload.qty <= 0) {
            showMessage("请填写物料、仓库和采购数量", "error");
            return;
        }
        const result = await postJson("/inventory/external-purchase", payload);
        renderDetail("warehouseDetail", {
            purchaseNo: result.purchaseNo,
            supplierStatus: result.supplierStatus,
            material: materialName(payload.materialId),
            warehouse: warehouseName(payload.warehouseId),
            qty: payload.qty,
            batchNo: result.inventory?.batchNo || payload.batchNo || "-"
        }, "采购补料结果");
        showMessage("外部采购已完成，库存已入库", "ok");
        await refreshWarehouse();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function renderRequisitionStatus(row) {
    const status = row.requestStatus || "";
    const label = status === "CREATED" ? "待仓库接收" : status === "RECEIVED" ? "已接收待审批" : statusText(status);
    return `<span class="status status-${escapeHtml(status.toLowerCase())}">${escapeHtml(label)}</span>`;
}

function renderRequisitionActions(row) {
    const actions = [`<button onclick="openRequisitionReview(${row.requisitionId})">\u8be6\u60c5</button>`];
    const canReview = hasRole("WAREHOUSE_ADMIN") && hasPermission("warehouse.requisition.approve");
    if (row.requestStatus === "CREATED" && canReview) {
        actions.push(`<button onclick="receiveRequisition(${row.requisitionId})">接收</button>`);
    }
    if (row.requestStatus === "RECEIVED" && canReview) {
        actions.push(`<button onclick="openRequisitionReview(${row.requisitionId})">\u5ba1\u6279</button>`);
    }
    return actions.join("");
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
    if (row.deliveryStatus === "ARRIVED") {
        actions.push("待申请人确认");
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
async function showInventoryDetail(id) {
    try {
        const inventory = await getJson(`/inventory/${id}`);
        renderDetail("warehouseDetail", buildInventoryDetail(inventory), "\u5e93\u5b58\u8be6\u60c5");
        showMessage("\u5e93\u5b58\u8be6\u60c5\u5df2\u52a0\u8f7d", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}
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
    const isReceived = requisition.requestStatus === "RECEIVED";
    const canHandleRequisition = hasRole("WAREHOUSE_ADMIN") && hasPermission("warehouse.requisition.approve");
    const canReceive = isCreated && canHandleRequisition;
    const canReview = isReceived && canHandleRequisition;
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
            <h3>${canReview ? "\u9886\u6599\u5ba1\u6838" : canReceive ? "领料接收" : "\u9886\u6599\u8be6\u60c5"}</h3>
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
                ${itemRows.map(item => `<div class="${item.enough ? "pass" : "fail"}"><span>${escapeHtml(item.name)}</span><strong>\u9700\u6c42 ${escapeHtml(item.required)} / \u76ee\u6807\u4ed3\u53ef\u7528 ${escapeHtml(item.available)}</strong><small>${escapeHtml(item.note ? `${item.batch}\uff1b${item.note}` : item.batch)}</small></div>`).join("") || `<p>\u6682\u65e0\u660e\u7ec6</p>`}
            </div>
            ${remark}
            ${canReview ? `<label>\u9a73\u56de\u539f\u56e0<textarea id="requisitionRejectReason" rows="3" placeholder="\u586b\u5199\u539f\u56e0\uff0c\u4fbf\u4e8e\u7533\u8bf7\u4eba\u4fee\u6b63"></textarea></label>` : ""}
            <div class="modal-actions">
                <button type="button" id="reviewCancel">\u53d6\u6d88</button>
                ${canReceive ? `<button type="button" id="reviewReceive">接收任务</button>` : ""}
                ${canReview ? `<button type="button" id="reviewReject">\u9a73\u56de</button>
                <button type="button" id="reviewApprove" ${canApprove ? "" : "disabled"}>\u5ba1\u6838\u901a\u8fc7</button>` : ""}
            </div>
        </div>`;
    document.body.appendChild(mask);
    mask.querySelector("#reviewCancel").addEventListener("click", () => mask.remove());
    mask.querySelector("#reviewReceive")?.addEventListener("click", async () => {
        await receiveRequisition(requisition.requisitionId);
        mask.remove();
    });
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
            pass: requisition.requestStatus === "RECEIVED",
            blocking: true,
            label: "\u5355\u636e\u72b6\u6001",
            detail: requisition.requestStatus === "RECEIVED" ? "已接收，可审批" : `\u5f53\u524d\u72b6\u6001\uff1a${statusText(requisition.requestStatus || "")}`
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
        const otherAvailable = warehouseCache.inventory
            .filter(inv => Number(inv.warehouseId) !== Number(requisition.warehouseId))
            .filter(inv => Number(inv.materialId) === Number(item.materialId))
            .filter(inv => !item.batchNo || inv.batchNo === item.batchNo)
            .reduce((sum, inv) => sum + (Number(inv.availableQty) || 0), 0);
        return {
            name: materialName(item.materialId),
            required,
            available,
            enough: available >= required,
            batch: item.batchNo ? `\u6279\u6b21\uff1a${item.batchNo}` : "\u4e0d\u9650\u6279\u6b21",
            note: available < required && otherAvailable > 0 ? `\u5176\u4ed6\u4ed3\u5e93\u53ef\u7528 ${otherAvailable}\uff0c\u8bf7\u9a73\u56de\u540e\u8ba9\u7533\u8bf7\u4eba\u91cd\u9009\u4ed3\u5e93` : ""
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

function findSeedRow(rows, key, value) {
    return rows.find(row => String(row[key]) === String(value));
}

async function ensureSeedRow(rows, key, value, create) {
    const existing = findSeedRow(rows, key, value);
    if (existing) return existing;
    const created = await create();
    rows.push(created);
    return created;
}

async function upsertSeedInventory(inventoryRows, item) {
    const existing = inventoryRows.find(row =>
        Number(row.materialId) === Number(item.materialId)
        && Number(row.warehouseId) === Number(item.warehouseId)
        && Number(row.locationId) === Number(item.locationId)
        && String(row.batchNo) === String(item.batchNo)
    );
    if (existing) {
        const availableQty = Math.max(Number(existing.availableQty || 0), Number(item.availableQty || 0));
        return putJson(`/inventory/${existing.inventoryId}`, { availableQty, qualityStatus: "QUALIFIED" });
    }
    const created = await postJson("/inventory", item);
    inventoryRows.push(created);
    return created;
}

async function seedWarehouse() {
    try {
        const [materials, warehouses, locations, inventory, robots] = await Promise.all([
            getJson("/materials"),
            getJson("/warehouses"),
            getJson("/warehouses/locations"),
            getJson("/inventory"),
            getJson("/robots")
        ]);
        const warehouse = await ensureSeedRow(warehouses, "warehouseCode", "WH-RAW-01", () =>
            postJson("/warehouses", { warehouseCode: "WH-RAW-01", warehouseName: "\u539f\u6750\u6599\u4ed3", warehouseType: "RAW" }));
        const locationA01 = await ensureSeedRow(locations, "locationCode", "RAW-A01", () =>
            postJson("/warehouses/locations", { warehouseId: warehouse.warehouseId, locationCode: "RAW-A01", locationName: "\u539f\u6750\u6599 A01" }));
        const locationA02 = await ensureSeedRow(locations, "locationCode", "RAW-A02", () =>
            postJson("/warehouses/locations", { warehouseId: warehouse.warehouseId, locationCode: "RAW-A02", locationName: "\u539f\u6750\u6599 A02" }));
        const materialSeeds = [
            { materialCode: "MAT-NR-RSS3", materialName: "\u5929\u7136\u6a61\u80f6 RSS3", materialType: "RAW", specification: "RSS3", unit: "kg", shelfLifeDays: 365 },
            { materialCode: "MAT-SBR-1502", materialName: "\u4e01\u82ef\u6a61\u80f6 SBR1502", materialType: "RAW", specification: "SBR1502", unit: "kg", shelfLifeDays: 365 },
            { materialCode: "MAT-CB-N330", materialName: "\u70ad\u9ed1 N330", materialType: "RAW", specification: "N330", unit: "kg", shelfLifeDays: 540 },
            { materialCode: "MAT-STEEL-CORD", materialName: "\u94a2\u4e1d\u5e18\u7ebf", materialType: "RAW", specification: "0.30mm", unit: "kg", shelfLifeDays: 720 }
        ];
        const seededMaterials = [];
        for (const seed of materialSeeds) {
            seededMaterials.push(await ensureSeedRow(materials, "materialCode", seed.materialCode, () => postJson("/materials", seed)));
        }
        await upsertSeedInventory(inventory, {
            materialId: seededMaterials[0].materialId,
            warehouseId: warehouse.warehouseId,
            locationId: locationA01.locationId,
            batchNo: "BATCH-DEMO-001",
            availableQty: 5000
        });
        await upsertSeedInventory(inventory, {
            materialId: seededMaterials[1].materialId,
            warehouseId: warehouse.warehouseId,
            locationId: locationA01.locationId,
            batchNo: "BATCH-SBR-202607",
            availableQty: 3800
        });
        await upsertSeedInventory(inventory, {
            materialId: seededMaterials[2].materialId,
            warehouseId: warehouse.warehouseId,
            locationId: locationA02.locationId,
            batchNo: "BATCH-20260709-001",
            availableQty: 3200
        });
        await upsertSeedInventory(inventory, {
            materialId: seededMaterials[3].materialId,
            warehouseId: warehouse.warehouseId,
            locationId: locationA02.locationId,
            batchNo: "BATCH-SC-202607",
            availableQty: 2600
        });
        await ensureSeedRow(robots, "robotCode", "ROB-RAW-01", () =>
            postJson("/robots", { robotCode: "ROB-RAW-01", robotName: "\u539f\u6750\u6599\u4ed3\u914d\u9001\u673a\u5668\u4eba\u4e00\u53f7", warehouseId: warehouse.warehouseId, batteryLevel: 88, currentLocation: "\u539f\u6750\u6599\u4ed3\u5f85\u547d\u533a" }));
        renderDetail("warehouseDetail", { warehouse, locationA01, locationA02, materials: seededMaterials }, "\u6f14\u793a\u5e93\u5b58\u5df2\u8865\u5145");
        showMessage("\u6f14\u793a\u5e93\u5b58\u5df2\u8865\u5145\uff0c\u53ef\u76f4\u63a5\u5ba1\u6279\u9886\u6599\u4efb\u52a1", "ok");
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

async function receiveRequisition(id) {
    try {
        await postJson(`/requisitions/${id}/receive`);
        showMessage("领料任务已接收，请继续审批", "ok");
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
    const names = ["\u57fa\u7840\u6570\u636e", "\u53d1\u8d77\u9886\u6599", "\u9886\u6599\u4efb\u52a1", "\u5df2\u9a73\u56de\u9886\u6599\u4efb\u52a1", "\u62e3\u8d27\u4efb\u52a1", "\u914d\u9001\u4efb\u52a1", "\u5e93\u5b58\u6d41\u6c34", "\u4ed3\u50a8\u8be6\u60c5"];
    titles.forEach((title, index) => {
        if (names[index]) title.textContent = names[index];
    });
    document.querySelector("#requisitionForm button[type='submit']").textContent = "\u63d0\u4ea4\u9886\u6599\u7533\u8bf7";
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
