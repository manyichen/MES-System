let lastWorkOrderId = null;
let aiPlanningConfirmedTaskId = null;
let planningCache = { orders: [], tasks: [], workOrders: [], products: [], lines: [], routes: [], operators: [], shortageAlerts: [], workOrderLogs: [] };

const TXT = {
    refreshPlanning: "\u8ba1\u5212\u5de5\u5355\u6570\u636e\u5df2\u5237\u65b0",
    noProduct: "\u8bf7\u5148\u7ef4\u62a4\u4ea7\u54c1\u4e3b\u6570\u636e",
    noLine: "\u8bf7\u5148\u7ef4\u62a4\u4ea7\u7ebf\u4e3b\u6570\u636e",
    noRoute: "\u8bf7\u5148\u7ef4\u62a4\u5de5\u827a\u8def\u7ebf",
    demoDone: "\u6f14\u793a\u4e3b\u7ebf\u6570\u636e\u5df2\u751f\u6210",
    kittingDone: "\u9f50\u5957\u5206\u6790\u5df2\u5b8c\u6210",
    releaseDone: "\u751f\u4ea7\u4efb\u52a1\u5df2\u53d1\u5e03",
    aiAdviceDone: "AI 排产建议已生成",
    dispatchDone: "\u751f\u4ea7\u5de5\u5355\u5df2\u6d3e\u53d1\u7ed9\u64cd\u4f5c\u5de5",
    dispatchCancel: "\u5df2\u53d6\u6d88\u6d3e\u53d1",
    receiveDone: "\u751f\u4ea7\u5de5\u5355\u5df2\u63a5\u6536\uff0c\u53ef\u4ee5\u8fdb\u5165\u751f\u4ea7\u62a5\u5de5",
    orderCreated: "\u5ba2\u6237\u8ba2\u5355\u5df2\u521b\u5efa",
    taskCreated: "\u751f\u4ea7\u4efb\u52a1\u5df2\u521b\u5efa",
    workOrderCreated: "\u751f\u4ea7\u5de5\u5355\u5df2\u521b\u5efa",
    workOrderDetail: "\u5de5\u5355\u8be6\u60c5\u4e0e\u6d3e\u5de5",
    chooseOperator: "\u9009\u62e9\u63a5\u5355\u64cd\u4f5c\u5de5",
    cancel: "\u53d6\u6d88",
    confirmDispatch: "\u786e\u8ba4\u6d3e\u53d1",
    noSelectData: "\u6682\u65e0\u53ef\u9009\u6570\u636e"
};

async function refreshPlanning(options = {}) {
    try {
        const canReadPlanning = hasPermission("planning.read");
        const canReadUsers = hasPermission("user.read");
        const canDispatchWorkOrder = hasPermission("planning.work_order.dispatch");
        const canReadWorkOrder = hasPermission("planning.work_order.read") || canReadPlanning;
        const [orders, tasks, workOrders, products, lines, routes, users, shortageAlerts, workOrderLogs] = await Promise.all([
            canReadPlanning ? getJson("/orders") : Promise.resolve([]),
            canReadPlanning ? getJson("/production-tasks") : Promise.resolve([]),
            getJson("/work-orders"),
            canReadPlanning ? getJson("/products").catch(() => []) : Promise.resolve([]),
            canReadPlanning ? getJson("/production-lines").catch(() => []) : Promise.resolve([]),
            canReadPlanning ? getJson("/process-routes").catch(() => []) : Promise.resolve([]),
            canDispatchWorkOrder ? getJson("/work-orders/operators").catch(() => []) :
                (canReadUsers ? getJson("/users").catch(() => []) : Promise.resolve([])),
            canReadPlanning ? getJson("/shortage-alerts").catch(() => []) : Promise.resolve([]),
            canReadWorkOrder ? getJson("/work-orders/logs").catch(() => []) : Promise.resolve([])
        ]);
        planningCache = {
            orders,
            tasks,
            workOrders,
            products,
            lines,
            routes,
            shortageAlerts,
            workOrderLogs,
            operators: users.filter(user => user.roleCode === "PRODUCTION_OPERATOR" && user.enabled !== false)
        };
        renderPlanningSelectors();
        renderPlanningTables();
        if (workOrders.length) lastWorkOrderId = workOrders[0].workOrderId;
        if (options.notify) showMessage(TXT.refreshPlanning, "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function renderPlanningSelectors() {
    replaceInputWithSelect("orderForm", "productId", planningCache.products, "productId",
        item => [item.productCode, item.productModel || item.productName].filter(Boolean).join(" / "), true);
    replaceInputWithSelect("taskForm", "orderId", planningCache.orders, "orderId",
        item => `${item.orderNo || "\u8ba2\u5355"} / ${item.customerName || "\u672a\u547d\u540d\u5ba2\u6237"}`, true);
    const taskSelect = document.getElementById("workOrderForm")?.elements.taskId;
    const selectedTaskId = Number(taskSelect?.value);
    const readyTasks = planningCache.tasks.filter(item => item.taskStatus === "READY" && item.kittingStatus === "READY");
    replaceInputWithSelect("workOrderForm", "taskId", readyTasks, "taskId", item => `${item.taskNo || "\u4efb\u52a1"} / ${planningCache.orders.find(order => Number(order.orderId) === Number(item.orderId))?.customerName || "\u672a\u547d\u540d\u5ba2\u6237"}`, true);
    const selectedTask = planningCache.tasks.find(item => Number(item.taskId) === selectedTaskId) || planningCache.tasks.find(item => Number(item.taskId) === Number(document.getElementById("workOrderForm")?.elements.taskId?.value));
    const availableLines = planningCache.lines.filter(item => Number(item.enabled) !== 0 && item.lineStatus !== "FAULT" && item.lineStatus !== "DISABLED");
    const routes = buildProcessRouteOptions(selectedTask?.productId);
    replaceInputWithSelect("workOrderForm", "lineId", availableLines, "lineId", item => [item.lineCode, item.lineName].filter(Boolean).join(" / "), true);
    replaceInputWithSelect("workOrderForm", "processId", routes, "processId", item => item.routeName, true);
    renderWorkOrderReadiness();
    renderLineCapacityBoard();
    renderWorkOrderAiPanel();
}

function buildProcessRouteOptions(productId) {
    if (!productId) return [];
    const steps = planningCache.routes.filter(item => Number(item.productId) === Number(productId))
        .sort((a, b) => Number(a.processSeq || 0) - Number(b.processSeq || 0));
    if (!steps.length) return [];
    const product = planningCache.products.find(item => Number(item.productId) === Number(productId));
    return [{
        processId: steps[0].processId,
        routeName: `${product?.productCode || "产品"} 标准工艺路线：${steps.map(step => step.processName || step.processCode).join(" → ")}`,
        steps
    }];
}

function renderLineCapacityBoard() {
    const board = document.getElementById("lineCapacityBoard");
    if (!board) return;
    const active = planningCache.workOrders.filter(row => !["COMPLETED", "CANCELLED"].includes(row.workOrderStatus));
    board.innerHTML = `<h4>产线状态与当前负荷</h4><div class="line-capacity-grid">${planningCache.lines.map(line => {
        const assigned = active.filter(row => Number(row.lineId) === Number(line.lineId));
        const qty = assigned.reduce((sum, row) => sum + (Number(row.plannedQty) || 0) - (Number(row.actualQty) || 0), 0);
        const unavailable = Number(line.enabled) === 0 || ["FAULT", "DISABLED"].includes(line.lineStatus);
        const status = unavailable ? "不可用" : (line.lineStatus === "RUNNING" ? "运行中" : "可排产");
        return `<section class="line-capacity-card ${unavailable ? "is-unavailable" : ""}"><strong>${escapeHtml(line.lineCode || "产线")} / ${escapeHtml(line.lineName || "")}</strong><span>${escapeHtml(status)} · 日产能 ${escapeHtml(line.capacityPerDay ?? "-")}</span><small>在制工单 ${assigned.length} 张，待完成 ${qty} 条</small></section>`;
    }).join("") || "<p>暂无产线数据</p>"}</div>`;
}

function replaceInputWithSelect(formId, name, rows, valueKey, labelFn, allowEmpty = false) {
    const form = document.getElementById(formId);
    const current = form?.elements[name];
    if (!form || !current) return;
    const previousValue = current.value;
    const select = current.tagName === "SELECT" ? current : document.createElement("select");
    select.name = name;
    select.required = current.required;
    select.replaceChildren();
    if (allowEmpty || !rows.length) {
        const option = document.createElement("option");
        option.value = "";
        option.textContent = rows.length ? "\u8bf7\u9009\u62e9" : TXT.noSelectData;
        select.appendChild(option);
    }
    for (const row of rows) {
        const option = document.createElement("option");
        option.value = row[valueKey];
        option.textContent = labelFn(row);
        select.appendChild(option);
    }
    if (rows.some(row => String(row[valueKey]) === String(previousValue))) select.value = previousValue;
    else if (allowEmpty) select.value = "";
    if (current !== select) current.replaceWith(select);
}

function renderPlanningTables() {
    renderPlanningFocus();
    renderWorkOrderAiPanel();
    renderTable("orderTable", planningCache.orders, [
        { title: "ID", key: "orderId" },
        { title: "\u7f16\u53f7", key: "orderNo" },
        { title: "\u5ba2\u6237", key: "customerName" },
        { title: "\u4ea7\u54c1", key: "productCode", render: renderOrderProduct },
        { title: "\u6570\u91cf", key: "orderQty" },
        { title: "\u72b6\u6001", key: "orderStatus" }
    ]);
    renderTable("taskTable", planningCache.tasks, [
        { title: "ID", key: "taskId" },
        { title: "\u7f16\u53f7", key: "taskNo" },
        { title: "\u8ba2\u5355", key: "orderId" },
        { title: "\u6570\u91cf", key: "planQty" },
        { title: "\u9f50\u5957", key: "kittingStatus" },
        { title: "\u72b6\u6001", key: "taskStatus" },
        { title: "\u64cd\u4f5c", render: renderTaskActions }
    ]);
    renderTable("planningShortageAlertTable", planningCache.shortageAlerts, [
        { title: "预警编号", key: "alertNo" },
        { title: "生产任务", key: "taskId" },
        { title: "缺料名称", key: "materialName" },
        { title: "缺料数量", key: "shortageQty" },
        { title: "状态", key: "alertStatus" },
        { title: "仓储接收时间", key: "acceptedAt" }
    ]);
    renderTable("workOrderTable", sortWorkOrdersForDisplay(planningCache.workOrders), [
        { title: "ID", key: "workOrderId" },
        { title: "\u7f16\u53f7", key: "workOrderNo" },
        { title: "\u4efb\u52a1", key: "taskId" },
        { title: "\u4ea7\u7ebf", key: "lineId" },
        { title: "工艺路线", render: renderWorkOrderRoute },
        { title: "\u72b6\u6001", key: "workOrderStatus", render: row => escapeHtml(workOrderStatusText(row.workOrderStatus)) },
        { title: "\u64cd\u4f5c", render: renderWorkOrderActions }
    ]);
    renderWorkOrderLogTable();
}

function sortWorkOrdersForDisplay(workOrders) {
    const unreceivedRank = row => row.workOrderStatus === "DISPATCHED" ? 0 : 1;
    return [...(workOrders || [])].sort((left, right) =>
        unreceivedRank(left) - unreceivedRank(right) || Number(left.workOrderId) - Number(right.workOrderId));
}

const WORK_ORDER_OPERATION_TEXT = {
    CREATE: "\u521b\u5efa\u5de5\u5355",
    DISPATCH: "\u6d3e\u53d1\u5de5\u5355",
    RECEIVE: "\u63a5\u6536\u5de5\u5355",
    REJECT: "\u62d2\u7edd\u63a5\u6536",
    CANCEL: "\u64a4\u9500\u5de5\u5355",
    COMPLETE: "\u5b8c\u6210\u5de5\u5355"
};

function workOrderOperationText(type) {
    return WORK_ORDER_OPERATION_TEXT[type] || statusText(type) || type || "-";
}

function workOrderStatusText(status) {
    return status === "REJECTED" ? "已拒绝接收" : statusText(status);
}

function logStatusText(status) {
    if (!status) return "-";
    return workOrderStatusText(status) || status;
}

function logOperatorLabel(operatorId) {
    if (!operatorId) return "-";
    const session = getCurrentSession();
    if (Number(session?.user?.userId) === Number(operatorId)) {
        return session?.user?.realName || session?.user?.username || `\u7528\u6237 ${operatorId}`;
    }
    return operatorLabel(operatorId);
}

function renderWorkOrderLogTable() {
    if (!document.getElementById("workOrderLogTable")) return;
    renderTable("workOrderLogTable", planningCache.workOrderLogs, [
        { title: "\u5de5\u5355\u7f16\u53f7", key: "workOrderNo", render: row => escapeHtml(row.workOrderNo || `WO-${row.workOrderId}`) },
        { title: "\u64cd\u4f5c", key: "operationType", render: row => escapeHtml(workOrderOperationText(row.operationType)) },
        { title: "\u72b6\u6001\u53d8\u5316", render: row => escapeHtml(`${logStatusText(row.fromStatus)} \u2192 ${logStatusText(row.toStatus)}`) },
        { title: "\u64cd\u4f5c\u4eba", render: row => escapeHtml(logOperatorLabel(row.operatorId)) },
        { title: "\u64cd\u4f5c\u65f6\u95f4", key: "operatedAt", render: row => escapeHtml(formatDateTime(row.operatedAt)) },
        { title: "\u8bf4\u660e", key: "remark" }
    ]);
}

function renderOrderProduct(row) {
    const product = planningCache.products.find(item => item.productId === row.productId);
    const code = row.productCode || product?.productCode || "";
    const model = row.productModel || product?.productModel || product?.productName || "";
    const label = [code, model].filter(Boolean).join(" / ");
    return escapeHtml(label || `ID ${row.productId ?? "-"}`);
}

function renderWorkOrderRoute(row) {
    const task = planningCache.tasks.find(item => Number(item.taskId) === Number(row.taskId));
    const route = buildProcessRouteOptions(task?.productId).find(item => Number(item.processId) === Number(row.processId));
    return escapeHtml(route?.routeName || `工艺路线锚点 ${row.processId ?? "-"}`);
}

function renderPlanningFocus() {
    const grid = document.querySelector("#planning .grid");
    if (!grid) return;
    let focus = document.getElementById("planningFocus");
    if (!focus) {
        focus = document.createElement("div");
        focus.id = "planningFocus";
        focus.className = "tool wide workflow-focus b-focus";
        grid.prepend(focus);
    }
    if (hasRole("PRODUCTION_OPERATOR")) {
        const pendingReceive = planningCache.workOrders.filter(row => row.workOrderStatus === "DISPATCHED").length;
        focus.innerHTML = `
        <h3>计划工单工作台</h3>
        <p class="focus-hint">先处理待接收的生产工单，再查看工单操作日志。</p>
        <div class="workflow-steps">
            <button type="button" onclick="scrollBSection('workOrderTable')"><strong>${pendingReceive}</strong><span>待接收工单</span></button>
        </div>`;
        return;
    }
    const pendingTasks = planningCache.tasks.filter(row => row.taskStatus !== "RELEASED").length;
    const releasedTasks = planningCache.tasks.filter(row => row.taskStatus === "RELEASED").length;
    const createdWorkOrders = planningCache.workOrders.filter(row => row.workOrderStatus === "CREATED").length;
    const dispatchedWorkOrders = planningCache.workOrders.filter(row => row.workOrderStatus === "DISPATCHED").length;
    focus.innerHTML = `
        <h3>\u8ba1\u5212\u5de5\u5355\u5de5\u4f5c\u53f0</h3>
        <p class="focus-hint">\u5148\u5904\u7406\u4e0b\u9762\u6570\u91cf\u5927\u4e8e 0 \u7684\u4efb\u52a1\uff0c\u518d\u67e5\u770b\u660e\u7ec6\u8868\u3002</p>
        <div class="workflow-steps">
            <button type="button" onclick="scrollBSection('taskTable')"><strong>${pendingTasks}</strong><span>\u5f85\u9f50\u5957/\u53d1\u5e03\u4efb\u52a1</span></button>
            <button type="button" onclick="scrollBSection('workOrderForm')"><strong>${releasedTasks}</strong><span>\u53ef\u521b\u5efa\u5de5\u5355\u4efb\u52a1</span></button>
            <button type="button" onclick="scrollBSection('workOrderTable')"><strong>${createdWorkOrders}</strong><span>\u5f85\u6d3e\u53d1\u5de5\u5355</span></button>
            <button type="button" onclick="scrollBSection('workOrderTable')"><strong>${dispatchedWorkOrders}</strong><span>\u5df2\u6d3e\u53d1\u5f85\u63a5\u6536</span></button>
        </div>`;
}

function scrollBSection(id) {
    jumpPlanningWorkbench(id);
}

function jumpPlanningWorkbench(id) {
    const target = document.getElementById(id);
    const panel = document.getElementById("planning");
    if (!target || !panel) return;

    const actionForm = target.matches("form[data-action-view]") ? target : target.closest("form[data-action-view]");
    if (actionForm) {
        const drawer = actionForm.closest(".module-drawer");
        if (drawer) {
            if (typeof selectActionView === "function") selectActionView(drawer, actionForm.dataset.actionView);
            if (typeof openModuleDrawer === "function") openModuleDrawer(drawer);
        }
        window.setTimeout(() => actionForm.scrollIntoView({ behavior: "smooth", block: "start" }), 80);
        showMessage("\u5df2\u5b9a\u4f4d\u5230\u5bf9\u5e94\u64cd\u4f5c\u533a", "ok");
        return;
    }

    const workspaceView = target.closest("[data-workspace-view]");
    if (workspaceView?.dataset.workspaceView && typeof selectModuleView === "function") {
        selectModuleView(panel, workspaceView.dataset.workspaceView);
    }
    window.setTimeout(() => target.closest(".tool")?.scrollIntoView({ behavior: "smooth", block: "center" }), 80);
    showMessage("\u5df2\u5b9a\u4f4d\u5230\u5bf9\u5e94\u64cd\u4f5c\u533a", "ok");
}

function renderTaskActions(row) {
    const actions = hasPermission("planning.task.release") ? [`<button onclick="analyzeTask(${row.taskId})">齐套分析</button>`] : [];
    if (row.kittingStatus === "SHORTAGE" && hasPermission("planning.task.release")) {
        const published = planningCache.shortageAlerts.some(alert => Number(alert.taskId) === Number(row.taskId) && ["OPEN", "ACCEPTED"].includes(alert.alertStatus));
        actions.push(published ? `<button type="button" disabled>缺料预警已发布</button>` : `<button onclick="publishShortageAlerts(${row.taskId})">发布缺料预警</button>`);
    }
    if (row.taskStatus === "READY" && row.kittingStatus === "READY" && hasPermission("planning.work_order.create")) {
        actions.push(`<button onclick="openWorkOrderPlanning(${row.taskId})">制定工单</button>`);
    } else if (hasPermission("planning.work_order.create")) {
        actions.push(`<button type="button" disabled>${row.kittingStatus === "SHORTAGE" ? "缺料待处理" : "待齐套通过"}</button>`);
    }
    return actions.join("");
}

function getSelectedWorkOrderTask() {
    const taskId = Number(document.getElementById("workOrderForm")?.elements.taskId?.value);
    return planningCache.tasks.find(task => Number(task.taskId) === taskId) || null;
}

function resetAiPlanningState() {
    aiPlanningConfirmedTaskId = null;
}

function renderWorkOrderReadiness() {
    const hint = document.getElementById("workOrderKittingHint");
    const submit = document.querySelector("#workOrderForm button[type='submit']");
    const task = getSelectedWorkOrderTask();
    if (!hint) return;
    if (!task) {
        hint.textContent = "请选择已齐套的生产任务后，再进行 AI 辅助排产和人工确认。";
        hint.className = "work-order-readiness";
        if (submit) submit.disabled = true;
        return;
    }
    const order = planningCache.orders.find(item => Number(item.orderId) === Number(task.orderId));
    hint.textContent = `齐套已通过：${task.taskNo} / ${order?.customerName || "未命名客户"}，交付日期 ${order?.deliveryDate || "未填写"}。`;
    hint.className = "work-order-readiness is-ready";
    if (submit) submit.disabled = false;
}

function renderWorkOrderAiPanel() {
    const panel = document.getElementById("workOrderAiPanel");
    if (!panel) return;
    const task = getSelectedWorkOrderTask();
    if (!hasPermission("planning.work_order.create")) {
        panel.replaceChildren();
        return;
    }
    if (!task) {
        panel.innerHTML = `<p class="work-order-ai-empty">选择生产任务后，可生成 AI 排产建议。</p>`;
        return;
    }
    panel.innerHTML = `
        <div class="work-order-ai-head"><div><span>百炼大模型</span><h4>AI 排产辅助</h4><p>基于订单交期、任务期限、齐套结果、产线状态和在制负荷给出具体排产建议；不会自动创建工单。</p></div><button type="button" id="aiPlanningRun">生成具体建议</button></div>
        <div id="aiPlanningResult" class="ai-planning-result"><p>生成后将展示推荐产线、建议排产时间和交期判断；PMC 可采用或调整后确认。</p></div>`;
    panel.querySelector("#aiPlanningRun")?.addEventListener("click", requestAiPlanningAdvice);
}

function openWorkOrderPlanning(taskId) {
    const form = document.getElementById("workOrderForm");
    const drawer = form?.closest(".module-drawer");
    if (!form || !drawer) return;
    if (typeof selectActionView === "function") selectActionView(drawer, form.dataset.actionView);
    if (typeof openModuleDrawer === "function") openModuleDrawer(drawer);
    form.elements.taskId.value = String(taskId);
    resetAiPlanningState();
    renderPlanningSelectors();
    renderWorkOrderAiPanel();
    window.setTimeout(() => form.scrollIntoView({ behavior: "smooth", block: "start" }), 80);
}

function renderAiPlanningPanel() {
    const grid = document.querySelector("#planning .grid");
    if (!grid) return;
    let panel = document.getElementById("aiPlanningPanel");
    if (!hasPermission("planning.task.release")) {
        panel?.remove();
        return;
    }
    if (!panel) {
        panel = document.createElement("div");
        panel.id = "aiPlanningPanel";
        panel.className = "tool wide ai-planning-panel";
        const focus = document.getElementById("planningFocus");
        if (focus?.nextSibling) grid.insertBefore(panel, focus.nextSibling);
        else grid.prepend(panel);
    }
    const previous = new Set([...panel.querySelectorAll("#aiPlanningTaskIds option:checked")].map(option => option.value));
    const candidates = planningCache.tasks.filter(task => task.taskStatus !== "RELEASED");
    const selected = previous.size ? previous : new Set(candidates.map(task => String(task.taskId)));
    panel.innerHTML = `
        <div class="ai-planning-head">
            <div><span>百炼大模型</span><h3>AI 排产辅助决策</h3><p>根据任务、订单、BOM、库存、产线、齐套和缺料预警生成建议，结果需由 PMC 人工确认。</p></div>
            <button type="button" id="aiPlanningRun">生成建议</button>
        </div>
        <div class="ai-planning-form">
            <label>计划周期<input id="aiPlanningHorizon" type="number" min="1" max="30" value="7"><small>天</small></label>
            <label class="ai-objective">排产目标<input id="aiPlanningObjective" value="优先满足交期，同时降低缺料和产线冲突风险"></label>
            <label class="ai-task-picker">候选任务<select id="aiPlanningTaskIds" multiple size="${Math.min(Math.max(candidates.length, 3), 8)}">${candidates.map(task => `<option value="${escapeHtml(task.taskId)}" ${selected.has(String(task.taskId)) ? "selected" : ""}>${escapeHtml(task.taskNo || "任务")} / ID ${escapeHtml(task.taskId)} / ${escapeHtml(statusText(task.taskStatus || ""))} / ${escapeHtml(statusText(task.kittingStatus || ""))}</option>`).join("")}</select></label>
        </div>
        <div id="aiPlanningResult" class="ai-planning-result">${candidates.length ? "<p>选择候选任务后点击生成建议。</p>" : "<p>暂无未发布任务可分析。</p>"}</div>`;
    panel.querySelector("#aiPlanningRun")?.addEventListener("click", requestAiPlanningAdvice);
}

function openAiPlanningForTask(taskId) {
    document.getElementById("aiPlanningPanel")?.scrollIntoView({ behavior: "smooth", block: "center" });
    document.querySelectorAll("#aiPlanningTaskIds option").forEach(option => {
        option.selected = option.value === String(taskId);
    });
    showMessage("已选择该生产任务，可生成 AI 排产建议", "ok");
}

async function requestAiPlanningAdvice() {
    const selectedTask = getSelectedWorkOrderTask();
    const runButton = document.getElementById("aiPlanningRun");
    const resultBox = document.getElementById("aiPlanningResult");
    if (!selectedTask || selectedTask.taskStatus !== "READY" || selectedTask.kittingStatus !== "READY") {
        showMessage("请先选择齐套通过的生产任务", "error");
        return;
    }
    try {
        runButton.disabled = true;
        runButton.textContent = "生成中...";
        resultBox.innerHTML = "<p>正在综合订单、齐套、库存、产线与在制工单数据生成建议...</p>";
        const data = await postJson("/ai/planning/advice", { taskIds: [Number(selectedTask.taskId)] });
        aiPlanningConfirmedTaskId = Number(selectedTask.taskId);
        renderAiPlanningAdvice(data);
        showMessage(TXT.aiAdviceDone, "ok");
    } catch (error) {
        aiPlanningConfirmedTaskId = null;
        resultBox.innerHTML = `<p class="ai-error">${escapeHtml(toChineseError(error))}</p>`;
        showMessage(toChineseError(error), "error");
    } finally {
        runButton.disabled = false;
        runButton.textContent = "生成具体建议";
    }
    return;

    const panel = document.getElementById("aiPlanningPanel");
    const button = document.getElementById("aiPlanningRun");
    const result = document.getElementById("aiPlanningResult");
    const taskIds = [...document.querySelectorAll("#aiPlanningTaskIds option:checked")].map(option => Number(option.value)).filter(Boolean);
    if (!taskIds.length) {
        showMessage("请至少选择一个候选生产任务", "error");
        return;
    }
    try {
        button.disabled = true;
        button.textContent = "生成中...";
        result.innerHTML = "<p>正在读取排产数据并调用百炼大模型...</p>";
        const data = await postJson("/ai/planning/advice", {
            taskIds,
            horizonDays: Number(document.getElementById("aiPlanningHorizon")?.value) || 7,
            objective: document.getElementById("aiPlanningObjective")?.value || ""
        });
        renderAiPlanningAdvice(data);
        showMessage(TXT.aiAdviceDone, "ok");
        panel?.classList.add("has-result");
    } catch (error) {
        result.innerHTML = `<p class="ai-error">${escapeHtml(toChineseError(error))}</p>`;
        showMessage(toChineseError(error), "error");
    } finally {
        button.disabled = false;
        button.textContent = "生成建议";
    }
}

function renderAiPlanningAdvice(data) {
    const conciseResult = document.getElementById("aiPlanningResult");
    if (conciseResult) {
        const conciseAdvice = data?.advice || {};
        conciseResult.innerHTML = `<div class="ai-brief"><section><span>对应生产订单</span><p>${escapeHtml(conciseAdvice.orderAssignment || "-")}</p></section><section><span>推荐产线与排产窗口</span><p>${escapeHtml(conciseAdvice.recommendedLine || "未返回可用产线")} ｜ ${escapeHtml(conciseAdvice.recommendedStart || "-")} 至 ${escapeHtml(conciseAdvice.recommendedEnd || "-")}</p></section><section><span>交期判断</span><p>${escapeHtml(conciseAdvice.deadlineAssessment || "请人工核对任务完成期限与订单交付日期。")}</p></section><section><span>总体建议</span><p>${escapeHtml(conciseAdvice.overallAdvice || "请结合交期、齐套结果和产线负荷进行人工判断。")}</p></section><section><span>排产方式</span><p>${escapeHtml(conciseAdvice.schedulingMethod || "按交期和优先级排序，在人工确认可用产线与完整工艺路线后生成生产工单。")}</p></section>${conciseAdvice.recommendedLineId ? `<button type="button" id="applyAiLine">采用推荐产线</button>` : ""}</div>`;
        conciseResult.querySelector("#applyAiLine")?.addEventListener("click", () => applyAiRecommendedLine(conciseAdvice.recommendedLineId));
    }
    return;

    const result = document.getElementById("aiPlanningResult");
    if (!result) return;
    const advice = data?.advice || {};
    const tasks = Array.isArray(advice.recommendedTasks) ? advice.recommendedTasks : [];
    const materialRisks = Array.isArray(advice.materialRisks) ? advice.materialRisks : [];
    const capacityRisks = Array.isArray(advice.capacityRisks) ? advice.capacityRisks : [];
    const nextActions = Array.isArray(advice.nextActions) ? advice.nextActions : [];
    const warnings = Array.isArray(data?.validationWarnings) ? data.validationWarnings : [];
    result.innerHTML = `
        <div class="ai-summary">
            <div><span>总体建议</span><strong>${escapeHtml(advice.summary || "已生成建议")}</strong></div>
            <div><span>风险等级</span><strong class="risk-${escapeHtml(String(advice.riskLevel || "MEDIUM").toLowerCase())}">${escapeHtml(displayText(advice.riskLevel || "MEDIUM"))}</strong></div>
            <div><span>模型</span><strong>${escapeHtml(data?.model || "-")}</strong></div>
        </div>
        <p class="ai-strategy">${escapeHtml(advice.strategy || "请结合当前齐套和产线状态人工确认。")}</p>
        ${tasks.length ? `<div class="ai-task-list">${tasks.map(task => `<section>
            <b>#${escapeHtml(task.priority ?? "-")} 任务 ${escapeHtml(task.taskId ?? "-")}</b>
            <span>建议产线：${escapeHtml(task.suggestedLineId ?? "待确认")} ｜ ${escapeHtml(task.suggestedStart || "-")} 至 ${escapeHtml(task.suggestedEnd || "-")}</span>
            <strong>${escapeHtml(task.decision || "")}</strong>
            <p>${escapeHtml(task.reason || "")}</p>
        </section>`).join("")}</div>` : ""}
        ${renderAiList("物料风险", materialRisks)}
        ${renderAiList("产能风险", capacityRisks)}
        ${renderAiList("下一步人工操作", nextActions)}
        ${warnings.length ? renderAiList("校验提醒", warnings, "warning") : ""}`;
}

function renderAiList(title, rows, type = "") {
    if (!rows.length) return "";
    return `<div class="ai-list ${type}"><h4>${escapeHtml(title)}</h4><ul>${rows.map(row => `<li>${escapeHtml(row)}</li>`).join("")}</ul></div>`;
}

function applyAiRecommendedLine(lineId) {
    const selector = document.getElementById("workOrderForm")?.elements.lineId;
    if (!selector || ![...selector.options].some(option => Number(option.value) === Number(lineId))) {
        showMessage("推荐产线当前不可用，请从产线状态看板中人工选择可排产产线", "error");
        return;
    }
    selector.value = String(lineId);
    showMessage("已填入推荐产线，请结合状态看板和完整工艺路线后确认排产", "ok");
}

function renderWorkOrderActions(row) {
    if (hasRole("PRODUCTION_OPERATOR")) {
        const unreceived = row.workOrderStatus === "DISPATCHED";
        return `<button type="button" onclick="showWorkOrderDetail(${row.workOrderId})">${unreceived ? "\u63a5\u6536" : "\u8be6\u60c5"}</button>`;
    }
    return `<button type="button" onclick="showWorkOrderDetail(${row.workOrderId})">\u8be6\u60c5/\u6d3e\u5de5</button>`;
}

function canReceiveWorkOrder(row) {
    const currentUserId = Number(getCurrentSession()?.user?.userId);
    return hasPermission("planning.work_order.receive")
        && hasRole("PRODUCTION_OPERATOR")
        && Number(row.assignedTo) === currentUserId;
}

async function seedPlanning() {
    try {
        const [products, lines, routes] = await Promise.all([getJson("/products"), getJson("/production-lines"), getJson("/process-routes")]);
        if (!products.length) throw new Error(TXT.noProduct);
        if (!lines.length) throw new Error(TXT.noLine);
        if (!routes.length) throw new Error(TXT.noRoute);
        const product = products[0];
        const line = lines[0];
        const route = routes.find(item => item.productId === product.productId) || routes[0];
        const order = await postJson("/orders", { customerName: "\u53cc\u661f\u6f14\u793a\u5ba2\u6237", productId: product.productId, orderQty: 100, priorityLevel: 1 });
        const start = new Date();
        const end = new Date(start.getTime() + 3 * 24 * 60 * 60 * 1000);
        const task = await postJson("/production-tasks", { orderId: order.orderId, plannerId: getCurrentSession()?.user?.userId || 1, planQty: 100, plannedStartTime: toLocalDateTime(start), plannedEndTime: toLocalDateTime(end) });
        await postJson(`/production-tasks/${task.taskId}/kitting`);
        const workOrder = await postJson("/work-orders", { taskId: task.taskId, lineId: line.lineId, processId: route.processId, plannedQty: 100 });
        lastWorkOrderId = workOrder.workOrderId;
        showMessage(TXT.demoDone, "ok");
        await refreshPlanning();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function toLocalDateTime(date) {
    const offset = date.getTimezoneOffset() * 60_000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

async function analyzeTask(id) {
    try {
        await postJson(`/production-tasks/${id}/kitting`);
        showMessage(TXT.kittingDone, "ok");
        await refreshPlanning();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function publishShortageAlerts(id) {
    try {
        await postJson(`/production-tasks/${id}/shortage-alerts`);
        showMessage("缺料名称、需求数量和缺口数量已发布给仓储人员", "ok");
        await refreshPlanning();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function releaseTask(id) {
    try {
        await postJson(`/production-tasks/${id}/release`);
        showMessage(TXT.releaseDone, "ok");
        await refreshPlanning();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function dispatchWorkOrder(id) {
    try {
        const operatorId = await chooseOperator();
        if (!operatorId) {
            showMessage(TXT.dispatchCancel, "info");
            return;
        }
        await postJson(`/work-orders/${id}/dispatch?operatorId=${encodeURIComponent(operatorId)}`);
        showMessage(TXT.dispatchDone, "ok");
        await refreshPlanning();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function showWorkOrderDetail(id) {
    try {
        const workOrder = await getJson(`/work-orders/${id}`);
        const mask = document.createElement("div");
        mask.className = "modal-mask";
        const canDispatch = workOrder.workOrderStatus === "CREATED" && hasPermission("planning.work_order.dispatch");
        const canReceive = workOrder.workOrderStatus === "DISPATCHED" && canReceiveWorkOrder(workOrder);
        mask.innerHTML = `
            <div class="modal-card work-order-detail-modal">
                <h3>${TXT.workOrderDetail}</h3>
                <p class="modal-subtitle">${escapeHtml(workOrder.workOrderNo || `WO-${workOrder.workOrderId}`)} · ${escapeHtml(workOrderStatusText(workOrder.workOrderStatus || ""))}</p>
                <div class="work-order-detail-grid">
                    ${workOrderDetailRow("\u5de5\u5355ID", workOrder.workOrderId)}
                    ${workOrderDetailRow("\u751f\u4ea7\u4efb\u52a1", taskLabel(workOrder.taskId))}
                    ${workOrderDetailRow("\u4ea7\u54c1", productLabel(workOrder.productId))}
                    ${workOrderDetailRow("\u4ea7\u7ebf", lineLabel(workOrder.lineId))}
                    ${workOrderDetailRow("\u5de5\u5e8f", processLabel(workOrder.processId))}
                    ${workOrderDetailRow("\u8ba1\u5212\u6570\u91cf", workOrder.plannedQty)}
                    ${workOrderDetailRow("\u5b9e\u9645\u6570\u91cf", workOrder.actualQty)}
                    ${workOrderDetailRow("\u6279\u6b21", workOrder.batchNo)}
                    ${workOrderDetailRow("\u4f18\u5148\u7ea7", workOrder.priorityLevel)}
                    ${workOrderDetailRow("\u5df2\u6d3e\u7ed9", operatorLabel(workOrder.assignedTo))}
                    ${workOrderDetailRow("\u63a5\u6536\u4eba", operatorLabel(workOrder.acceptedBy))}
                    ${workOrderDetailRow("\u6d3e\u53d1\u65f6\u95f4", formatDateTime(workOrder.dispatchTime))}
                </div>
                ${canDispatch ? `<label class="work-order-dispatch-field">\u5206\u914d\u64cd\u4f5c\u5de5
                    <select id="workOrderDetailOperator">${operatorOptions()}</select>
                </label>` : ""}
                <div class="modal-actions">
                    <button type="button" id="workOrderDetailClose">${canReceive ? "\u53d6\u6d88" : "\u5173\u95ed"}</button>
                    ${canReceive ? `<button type="button" id="workOrderDetailReceive">\u786e\u8ba4\u63a5\u6536</button>` : ""}
                    ${canDispatch ? `<button type="button" id="workOrderDetailDispatch">\u786e\u8ba4\u6d3e\u5de5</button>` : ""}
                </div>
            </div>`;
        document.body.appendChild(mask);
        mask.querySelector("#workOrderDetailClose").addEventListener("click", () => mask.remove());
        mask.addEventListener("click", event => {
            if (event.target === mask) mask.remove();
        });
        mask.querySelector("#workOrderDetailReceive")?.addEventListener("click", async () => {
            mask.remove();
            await receiveWorkOrder(id);
        });
        mask.querySelector("#workOrderDetailDispatch")?.addEventListener("click", async () => {
            const operatorId = mask.querySelector("#workOrderDetailOperator")?.value;
            if (!operatorId) {
                showMessage("\u8bf7\u9009\u62e9\u64cd\u4f5c\u5de5", "error");
                return;
            }
            await postJson(`/work-orders/${id}/dispatch?operatorId=${encodeURIComponent(operatorId)}`);
            showMessage(TXT.dispatchDone, "ok");
            mask.remove();
            await refreshPlanning();
        });
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function workOrderDetailRow(label, value) {
    const display = value === null || value === undefined || value === "" ? "-" : value;
    return `<div><span>${escapeHtml(label)}</span><strong>${escapeHtml(display)}</strong></div>`;
}

function taskLabel(taskId) {
    const task = planningCache.tasks.find(item => Number(item.taskId) === Number(taskId));
    return task ? `${task.taskNo || task.taskId} / ${statusText(task.taskStatus || "")}` : taskId;
}

function productLabel(productId) {
    const product = planningCache.products.find(item => Number(item.productId) === Number(productId));
    return product ? `${product.productCode || product.productId} / ${product.productName || product.productModel || ""}` : productId;
}

function lineLabel(lineId) {
    const line = planningCache.lines.find(item => Number(item.lineId) === Number(lineId));
    return line ? `${line.lineName || line.lineCode || "\u4ea7\u7ebf"} / ID ${line.lineId}` : lineId;
}

function processLabel(processId) {
    const route = planningCache.routes.find(item => Number(item.processId) === Number(processId));
    return route ? `${route.processName || route.routeName || "\u5de5\u5e8f"} / ID ${route.processId}` : processId;
}

function operatorLabel(userId) {
    if (!userId) return "-";
    const user = planningCache.operators.find(item => Number(item.userId) === Number(userId));
    if (user) return `${user.realName || user.username} / ${user.username}`;
    const session = getCurrentSession();
    if (Number(session?.user?.userId) === Number(userId)) {
        return session?.user?.realName || session?.user?.username || `用户 ${userId}`;
    }
    return userId;
}

function operatorOptions() {
    if (!planningCache.operators.length) return `<option value="">${TXT.noSelectData}</option>`;
    return `<option value="">\u8bf7\u9009\u62e9</option>${planningCache.operators.map(user => `<option value="${escapeHtml(user.userId)}">${escapeHtml(user.realName || user.username)} / ${escapeHtml(user.username)} / ID ${escapeHtml(user.userId)}</option>`).join("")}`;
}

function chooseOperator() {
    return new Promise(resolve => {
        if (!planningCache.operators.length) {
            resolve(window.prompt("\u8bf7\u8f93\u5165\u64cd\u4f5c\u5de5\u7528\u6237ID"));
            return;
        }
        const mask = document.createElement("div");
        mask.className = "modal-mask";
        mask.innerHTML = `<div class="modal-card"><h3>${TXT.chooseOperator}</h3><select id="dispatchOperatorSelect">${planningCache.operators.map(user => `<option value="${escapeHtml(user.userId)}">${escapeHtml(user.realName || user.username)} / 账号 ${escapeHtml(user.username)} / 用户ID ${escapeHtml(user.userId)}</option>`).join("")}</select><div class="modal-actions"><button type="button" id="dispatchCancel">${TXT.cancel}</button><button type="button" id="dispatchConfirm">${TXT.confirmDispatch}</button></div></div>`;
        document.body.appendChild(mask);
        mask.querySelector("#dispatchCancel").addEventListener("click", () => {
            mask.remove();
            resolve(null);
        });
        mask.querySelector("#dispatchConfirm").addEventListener("click", () => {
            const value = mask.querySelector("#dispatchOperatorSelect").value;
            mask.remove();
            resolve(value);
        });
    });
}

async function receiveWorkOrder(id) {
    try {
        await postJson(`/work-orders/${id}/receive`);
        showMessage(TXT.receiveDone, "ok");
        await refreshPlanning();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

document.getElementById("seedPlanning")?.addEventListener("click", seedPlanning);
document.getElementById("refreshPlanning")?.addEventListener("click", () => refreshPlanning({ notify: true }));
document.getElementById("orderForm")?.addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.target);
    try {
        await postJson("/orders", { customerName: form.get("customerName"), productId: Number(form.get("productId")), orderQty: Number(form.get("orderQty")), deliveryDate: form.get("deliveryDate"), priorityLevel: Number(form.get("priorityLevel")), sourceSystem: "SALES_API" });
        showMessage(TXT.orderCreated, "ok");
        await refreshPlanning();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
});
document.getElementById("taskForm")?.addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.target);
    try {
        await postJson("/production-tasks", { orderId: Number(form.get("orderId")), planQty: Number(form.get("planQty")) || null, plannedStartTime: form.get("plannedStartTime"), plannedEndTime: form.get("plannedEndTime") });
        showMessage(TXT.taskCreated, "ok");
        await refreshPlanning();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
});
document.getElementById("workOrderForm")?.addEventListener("change", event => {
    if (event.target.name === "taskId") {
        resetAiPlanningState();
        renderPlanningSelectors();
        renderWorkOrderAiPanel();
    }
});

document.getElementById("workOrderForm")?.addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.target);
    const taskId = Number(form.get("taskId"));
    if (aiPlanningConfirmedTaskId !== taskId) {
        showMessage("请先针对当前生产任务生成 AI 排产建议", "error");
        return;
    }
    try {
        await postJson("/work-orders", { taskId, lineId: Number(form.get("lineId")), processId: Number(form.get("processId")) });
        showMessage(TXT.workOrderCreated, "ok");
        aiPlanningConfirmedTaskId = null;
        await refreshPlanning();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
});

function relabelPlanningStaticText() {
    const panel = document.getElementById("planning");
    if (!panel) return;
    panel.querySelector("h2").textContent = "PMC 计划与工单";
    document.getElementById("seedPlanning").textContent = "\u751f\u6210\u6f14\u793a\u4e3b\u7ebf";
    document.getElementById("refreshPlanning").textContent = "\u5237\u65b0";
    const titles = {
        orderTable: "客户订单",
        taskTable: "生产任务",
        planningShortageAlertTable: "缺料协同预警",
        workOrderTable: "生产工单",
        workOrderLogTable: "工单操作日志"
    };
    Object.entries(titles).forEach(([tableId, title]) => {
        const heading = document.getElementById(tableId)?.closest(".tool")?.querySelector("h3");
        if (heading) heading.textContent = title;
    });
    document.querySelector("#orderForm button[type='submit']").textContent = "确认创建生产订单";
    document.querySelector("#taskForm button[type='submit']").textContent = "确认制定生产任务";
    document.querySelector("#workOrderForm button[type='submit']").textContent = "确认排产并生成生产工单";
}

function setDefaultPlanningDates() {
    const orderDate = document.querySelector("#orderForm input[name='deliveryDate']");
    const taskStart = document.querySelector("#taskForm input[name='plannedStartTime']");
    const taskEnd = document.querySelector("#taskForm input[name='plannedEndTime']");
    const now = new Date();
    if (orderDate && !orderDate.value) orderDate.value = toLocalDateTime(new Date(now.getTime() + 14 * 24 * 60 * 60 * 1000)).slice(0, 10);
    if (taskStart && !taskStart.value) taskStart.value = toLocalDateTime(now);
    if (taskEnd && !taskEnd.value) taskEnd.value = toLocalDateTime(new Date(now.getTime() + 3 * 24 * 60 * 60 * 1000));
}

setDefaultPlanningDates();
relabelPlanningStaticText();
