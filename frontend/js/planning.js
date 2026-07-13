let lastWorkOrderId = null;
let planningCache = { orders: [], tasks: [], workOrders: [], products: [], lines: [], routes: [], operators: [] };

const TXT = {
    refreshPlanning: "\u8ba1\u5212\u5de5\u5355\u6570\u636e\u5df2\u5237\u65b0",
    noProduct: "\u8bf7\u5148\u7ef4\u62a4\u4ea7\u54c1\u4e3b\u6570\u636e",
    noLine: "\u8bf7\u5148\u7ef4\u62a4\u4ea7\u7ebf\u4e3b\u6570\u636e",
    noRoute: "\u8bf7\u5148\u7ef4\u62a4\u5de5\u827a\u8def\u7ebf",
    demoDone: "\u6f14\u793a\u4e3b\u7ebf\u6570\u636e\u5df2\u751f\u6210",
    kittingDone: "\u9f50\u5957\u5206\u6790\u5df2\u5b8c\u6210",
    releaseDone: "\u751f\u4ea7\u4efb\u52a1\u5df2\u53d1\u5e03",
    dispatchDone: "\u751f\u4ea7\u5de5\u5355\u5df2\u6d3e\u53d1\u7ed9\u64cd\u4f5c\u5de5",
    dispatchCancel: "\u5df2\u53d6\u6d88\u6d3e\u53d1",
    receiveDone: "\u751f\u4ea7\u5de5\u5355\u5df2\u63a5\u6536\uff0c\u53ef\u4ee5\u8fdb\u5165\u751f\u4ea7\u62a5\u5de5",
    logsLoaded: "\u5de5\u5355\u65e5\u5fd7\u5df2\u52a0\u8f7d",
    orderCreated: "\u5ba2\u6237\u8ba2\u5355\u5df2\u521b\u5efa",
    taskCreated: "\u751f\u4ea7\u4efb\u52a1\u5df2\u521b\u5efa",
    workOrderCreated: "\u751f\u4ea7\u5de5\u5355\u5df2\u521b\u5efa",
    chooseOperator: "\u9009\u62e9\u63a5\u5355\u64cd\u4f5c\u5de5",
    cancel: "\u53d6\u6d88",
    confirmDispatch: "\u786e\u8ba4\u6d3e\u53d1",
    noSelectData: "\u6682\u65e0\u53ef\u9009\u6570\u636e"
};

async function refreshPlanning(options = {}) {
    try {
        const canReadPlanning = hasPermission("planning.read");
        const canReadUsers = hasPermission("user.read");
        const [orders, tasks, workOrders, products, lines, routes, users] = await Promise.all([
            canReadPlanning ? getJson("/orders") : Promise.resolve([]),
            canReadPlanning ? getJson("/production-tasks") : Promise.resolve([]),
            getJson("/work-orders"),
            canReadPlanning ? getJson("/products").catch(() => []) : Promise.resolve([]),
            canReadPlanning ? getJson("/production-lines").catch(() => []) : Promise.resolve([]),
            canReadPlanning ? getJson("/process-routes").catch(() => []) : Promise.resolve([]),
            canReadUsers ? getJson("/users").catch(() => []) : Promise.resolve([])
        ]);
        planningCache = {
            orders,
            tasks,
            workOrders,
            products,
            lines,
            routes,
            operators: users.filter(user => user.roleCode === "PRODUCTION_OPERATOR" && user.enabled !== false)
        };
        renderPlanningSelectors();
        renderPlanningTables();
        if (workOrders.length) {
            lastWorkOrderId = workOrders[0].workOrderId;
            await loadWorkOrderLogs(lastWorkOrderId, false);
        }
        if (options.notify) showMessage(TXT.refreshPlanning, "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function renderPlanningSelectors() {
    replaceInputWithSelect("orderForm", "productId", planningCache.products, "productId",
        item => `${item.productName || item.productCode || "\u4ea7\u54c1"} / ID ${item.productId}`);
    replaceInputWithSelect("taskForm", "orderId", planningCache.orders, "orderId",
        item => `${item.orderNo || "\u8ba2\u5355"} / ${item.customerName || ""} / ${statusText(item.orderStatus || "")}`);
    replaceInputWithSelect("workOrderForm", "taskId", planningCache.tasks.filter(item => item.taskStatus === "RELEASED"), "taskId",
        item => `${item.taskNo || "\u4efb\u52a1"} / ${statusText(item.taskStatus || "")} / ${item.planQty ?? "-"}`);
    replaceInputWithSelect("workOrderForm", "lineId", planningCache.lines, "lineId",
        item => `${item.lineName || item.lineCode || "\u4ea7\u7ebf"} / ID ${item.lineId}`, true);
    replaceInputWithSelect("workOrderForm", "processId", planningCache.routes, "processId",
        item => `${item.processName || item.routeName || "\u5de5\u5e8f"} / ID ${item.processId}`, true);
}

function replaceInputWithSelect(formId, name, rows, valueKey, labelFn, allowEmpty = false) {
    const form = document.getElementById(formId);
    const current = form?.elements[name];
    if (!form || !current || current.tagName === "SELECT") return;
    const select = document.createElement("select");
    select.name = name;
    select.required = current.required && !allowEmpty;
    if (allowEmpty || !rows.length) {
        const option = document.createElement("option");
        option.value = "";
        option.textContent = allowEmpty ? "\u81ea\u52a8\u5339\u914d" : TXT.noSelectData;
        select.appendChild(option);
    }
    for (const row of rows) {
        const option = document.createElement("option");
        option.value = row[valueKey];
        option.textContent = labelFn(row);
        select.appendChild(option);
    }
    current.replaceWith(select);
}

function renderPlanningTables() {
    renderPlanningFocus();
    renderTable("orderTable", planningCache.orders, [
        { title: "ID", key: "orderId" },
        { title: "\u7f16\u53f7", key: "orderNo" },
        { title: "\u5ba2\u6237", key: "customerName" },
        { title: "\u4ea7\u54c1", key: "productId" },
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
    renderTable("workOrderTable", planningCache.workOrders, [
        { title: "ID", key: "workOrderId" },
        { title: "\u7f16\u53f7", key: "workOrderNo" },
        { title: "\u4efb\u52a1", key: "taskId" },
        { title: "\u4ea7\u7ebf", key: "lineId" },
        { title: "\u5de5\u5e8f", key: "processId" },
        { title: "\u72b6\u6001", key: "workOrderStatus" },
        { title: "\u64cd\u4f5c", render: renderWorkOrderActions }
    ]);
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
    const pendingTasks = planningCache.tasks.filter(row => row.taskStatus !== "RELEASED").length;
    const releasedTasks = planningCache.tasks.filter(row => row.taskStatus === "RELEASED").length;
    const createdWorkOrders = planningCache.workOrders.filter(row => row.workOrderStatus === "CREATED").length;
    const dispatchedWorkOrders = planningCache.workOrders.filter(row => row.workOrderStatus === "DISPATCHED").length;
    focus.innerHTML = `
        <h3>\u8ba1\u5212\u5de5\u5355\u5de5\u4f5c\u53f0</h3>
        <p class="focus-hint">\u5148\u5904\u7406\u4e0b\u9762\u6570\u91cf\u5927\u4e8e 0 \u7684\u4efb\u52a1\uff0c\u518d\u67e5\u770b\u660e\u7ec6\u8868\u3002</p>
        <div class="workflow-steps">
            <button type="button" onclick="jumpPlanningWorkbench('taskTable')"><strong>${pendingTasks}</strong><span>\u5f85\u9f50\u5957/\u53d1\u5e03\u4efb\u52a1</span></button>
            <button type="button" onclick="jumpPlanningWorkbench('workOrderForm')"><strong>${releasedTasks}</strong><span>\u53ef\u521b\u5efa\u5de5\u5355\u4efb\u52a1</span></button>
            <button type="button" onclick="jumpPlanningWorkbench('workOrderTable')"><strong>${createdWorkOrders}</strong><span>\u5f85\u6d3e\u53d1\u5de5\u5355</span></button>
            <button type="button" onclick="jumpPlanningWorkbench('workOrderTable')"><strong>${dispatchedWorkOrders}</strong><span>\u5df2\u6d3e\u53d1\u5f85\u63a5\u6536</span></button>
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
    if (!hasPermission("planning.task.release")) return "";
    const actions = [`<button onclick="analyzeTask(${row.taskId})">\u9f50\u5957\u5206\u6790</button>`];
    if (row.taskStatus === "RELEASED") {
        actions.push(`<button type="button" disabled>\u5df2\u53d1\u5e03</button>`);
    } else if (row.kittingStatus !== "READY") {
        const label = row.kittingStatus === "SHORTAGE" ? "\u7f3a\u6599\u4e0d\u53ef\u53d1\u5e03" : "\u5148\u9f50\u5957";
        actions.push(`<button type="button" disabled>${label}</button>`);
    } else {
        actions.push(`<button onclick="releaseTask(${row.taskId})">\u53d1\u5e03</button>`);
    }
    return actions.join("");
}

function renderWorkOrderActions(row) {
    const actions = [`<button onclick="loadWorkOrderLogs(${row.workOrderId})">\u65e5\u5fd7</button>`];
    if (row.workOrderStatus === "CREATED" && hasPermission("planning.work_order.dispatch")) {
        actions.unshift(`<button onclick="dispatchWorkOrder(${row.workOrderId})">\u6d3e\u53d1</button>`);
    }
    if (row.workOrderStatus === "DISPATCHED" && canReceiveWorkOrder(row)) {
        actions.unshift(`<button onclick="receiveWorkOrder(${row.workOrderId})">\u63a5\u6536</button>`);
    }
    return actions.join("");
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
        const task = await postJson("/production-tasks", { orderId: order.orderId, plannerId: getCurrentSession()?.user?.userId || 1, planQty: 100, targetLineId: line.lineId });
        await postJson(`/production-tasks/${task.taskId}/kitting`);
        await postJson(`/production-tasks/${task.taskId}/release`);
        const workOrder = await postJson("/work-orders", { taskId: task.taskId, lineId: line.lineId, processId: route.processId, plannedQty: 100 });
        lastWorkOrderId = workOrder.workOrderId;
        showMessage(TXT.demoDone, "ok");
        await refreshPlanning();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
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

async function loadWorkOrderLogs(id, notify = true) {
    try {
        const logs = await getJson(`/work-orders/${id}/logs`);
        renderTable("workOrderLogTable", logs, [
            { title: "ID", key: "logId" },
            { title: "\u5de5\u5355", key: "workOrderId" },
            { title: "\u64cd\u4f5c", key: "operationType" },
            { title: "\u539f\u72b6\u6001", key: "fromStatus" },
            { title: "\u65b0\u72b6\u6001", key: "toStatus" },
            { title: "\u8bf4\u660e", key: "remark" }
        ]);
        if (notify) showMessage(TXT.logsLoaded, "ok");
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
        await postJson("/orders", { customerName: form.get("customerName"), productId: Number(form.get("productId")), orderQty: Number(form.get("orderQty")) });
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
        await postJson("/production-tasks", { orderId: Number(form.get("orderId")), planQty: Number(form.get("planQty")) || null });
        showMessage(TXT.taskCreated, "ok");
        await refreshPlanning();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
});
document.getElementById("workOrderForm")?.addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.target);
    try {
        await postJson("/work-orders", { taskId: Number(form.get("taskId")), lineId: Number(form.get("lineId")) || null, processId: Number(form.get("processId")) || null });
        showMessage(TXT.workOrderCreated, "ok");
        await refreshPlanning();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
});

function relabelPlanningStaticText() {
    const panel = document.getElementById("planning");
    if (!panel) return;
    panel.querySelector("h2").textContent = "\u8ba1\u5212\u5de5\u5355";
    document.getElementById("seedPlanning").textContent = "\u751f\u6210\u6f14\u793a\u4e3b\u7ebf";
    document.getElementById("refreshPlanning").textContent = "\u5237\u65b0";
    const titles = panel.querySelectorAll(".tool > h3");
    const names = ["\u521b\u5efa\u5ba2\u6237\u8ba2\u5355", "\u521b\u5efa\u751f\u4ea7\u4efb\u52a1", "\u521b\u5efa\u751f\u4ea7\u5de5\u5355", "\u5ba2\u6237\u8ba2\u5355", "\u751f\u4ea7\u4efb\u52a1", "\u751f\u4ea7\u5de5\u5355", "\u5de5\u5355\u64cd\u4f5c\u65e5\u5fd7"];
    titles.forEach((title, index) => {
        if (names[index]) title.textContent = names[index];
    });
    document.querySelector("#orderForm button[type='submit']").textContent = "\u521b\u5efa\u8ba2\u5355";
    document.querySelector("#taskForm button[type='submit']").textContent = "\u521b\u5efa\u4efb\u52a1";
    document.querySelector("#workOrderForm button[type='submit']").textContent = "\u521b\u5efa\u5de5\u5355";
}

relabelPlanningStaticText();
