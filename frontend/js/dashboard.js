async function loadDashboard() {
    try {
        const dashboard = await getJson("/dashboard/my-summary");
        applyDashboardProfile(dashboard);
        renderDashboardMetrics(dashboard.metrics || []);
        renderDashboardTodos(dashboard.todos || []);
    } catch (error) {
        showMessage(error.message, "error");
    }
}

function applyDashboardProfile(dashboard) {
    document.getElementById("dashboard-role-name").textContent = dashboard.roleName || dashboard.primaryRole;
    document.getElementById("dashboard-scope").textContent = `数据范围：${dashboard.dataScope || ""}`;
    document.getElementById("dashboard-restrictions").innerHTML = `
        <h4>明确禁止</h4>
        <ul class="boundary-list">${(dashboard.prohibitedActions || []).map(item => `<li>${escapeHtml(item)}</li>`).join("")}</ul>`;

    const visibleModules = new Set(dashboard.visibleModules || ["dashboard"]);
    document.querySelectorAll(".sidebar button[data-tab]").forEach(button => {
        const visible = visibleModules.has(button.dataset.tab) && !button.classList.contains("permission-hidden");
        button.classList.toggle("module-hidden", !visible);
    });
    document.querySelectorAll(".panel").forEach(panel => {
        if (panel.id !== "dashboard" && !visibleModules.has(panel.id)) panel.classList.remove("active");
    });
}

function renderDashboardMetrics(metrics) {
    const container = document.getElementById("dashboard-metrics");
    container.innerHTML = metrics.map(metric => `
        <button type="button" class="metric metric-${escapeHtml(metric.level || "normal")}" data-metric-tab="${escapeHtml(metric.targetTab || "dashboard")}">
            <span>${escapeHtml(metric.label)}</span>
            <strong>${escapeHtml(metric.value || "0")}<small>${escapeHtml(metric.unit || "")}</small></strong>
        </button>
    `).join("");
    container.querySelectorAll("[data-metric-tab]").forEach(button => {
        button.addEventListener("click", () => switchTab(button.dataset.metricTab));
    });
}

function renderDashboardTodos(todos) {
    const allowed = todos.filter(todo => !todo.requiredPermission || hasPermission(todo.requiredPermission));
    document.getElementById("dashboard-todo-count").textContent = allowed.length ? `${allowed.length} 类待处理` : "全部处理完成";
    const container = document.getElementById("dashboard-todos");
    if (!allowed.length) {
        container.innerHTML = `<div class="empty-state">当前没有需要你处理的事务</div>`;
        return;
    }
    container.innerHTML = allowed.map(todo => `
        <button type="button" class="todo-item priority-${escapeHtml(todo.priority || "MEDIUM").toLowerCase()}"
                data-todo-tab="${escapeHtml(todo.targetTab)}" data-todo-anchor="${escapeHtml(todo.targetAnchor || "")}">
            <span class="todo-count">${escapeHtml(todo.count)}</span>
            <span class="todo-copy"><strong>${escapeHtml(todo.title)}</strong><small>${escapeHtml(todo.description)}</small></span>
            <span class="todo-go">处理</span>
        </button>
    `).join("");
    container.querySelectorAll("[data-todo-tab]").forEach(button => {
        button.addEventListener("click", () => switchTab(button.dataset.todoTab, button.dataset.todoAnchor));
    });
}

async function loadTraces() {
    try {
        const rows = await getJson("/product-traces");
        renderTable("trace-table", rows, [
            { key: "traceId", label: "ID" }, { key: "traceCode", label: "追溯码" },
            { key: "orderId", label: "订单" }, { key: "taskId", label: "任务" },
            { key: "workOrderId", label: "工单" }, { key: "batchNo", label: "批次" },
            { key: "traceStatus", label: "状态" }
        ]);
    } catch (error) { showMessage(error.message, "error"); }
}

async function loadFeedback(workOrderId = 1) {
    try {
        const rows = await getJson(`/management-feedback?workOrderId=${workOrderId}`);
        renderTable("feedback-table", rows, [
            { key: "feedbackId", label: "ID" }, { key: "feedbackNo", label: "编号" },
            { key: "workOrderId", label: "工单" }, { key: "feedbackType", label: "类型" },
            { key: "feedbackContent", label: "内容" }, { key: "feedbackStatus", label: "状态" }
        ], [{ name: "close-feedback", label: "关闭", idKey: "feedbackId", permission: "feedback.close", handler: closeFeedback }]);
    } catch (error) { showMessage(error.message, "error"); }
}

async function closeFeedback(id) {
    await postJson(`/management-feedback/${id}/close`);
    showMessage("反馈已关闭");
    const workOrderId = document.querySelector("#feedback-filter-form [name='workOrderId']").value || 1;
    loadFeedback(workOrderId);
    loadDashboard();
}

function bindDashboardEvents() {
    document.getElementById("refresh-dashboard")?.addEventListener("click", loadDashboard);
    document.getElementById("refresh-trace")?.addEventListener("click", loadTraces);
    document.getElementById("refresh-feedback")?.addEventListener("click", () => {
        const workOrderId = document.querySelector("#feedback-filter-form [name='workOrderId']").value || 1;
        loadFeedback(workOrderId);
    });
    document.getElementById("trace-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        await postJson("/product-traces", { ...formToObject(event.target), createdAt: nowIsoLocal() });
        showMessage("追溯记录已创建"); event.target.reset(); loadTraces();
    });
    document.getElementById("trace-search-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const traceCode = event.target.traceCode.value.trim();
        if (!traceCode) return;
        const trace = await getJson(`/product-traces/${encodeURIComponent(traceCode)}`);
        document.getElementById("trace-detail").innerHTML = `<div class="detail"><strong>${escapeHtml(trace.traceCode)}</strong><div>订单：${escapeHtml(trace.orderId)}，任务：${escapeHtml(trace.taskId)}，工单：${escapeHtml(trace.workOrderId)}</div><div>批次：${escapeHtml(trace.batchNo)}，状态：${escapeHtml(trace.traceStatus)}</div></div>`;
    });
    document.getElementById("feedback-filter-form")?.addEventListener("submit", event => {
        event.preventDefault(); loadFeedback(event.target.workOrderId.value || 1);
    });
    document.getElementById("feedback-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const payload = { ...formToObject(event.target), feedbackStatus: "OPEN", createdAt: nowIsoLocal(), closedAt: null };
        await postJson("/management-feedback", payload);
        showMessage("管理反馈已创建"); event.target.reset(); loadFeedback(payload.workOrderId || 1);
    });
}
