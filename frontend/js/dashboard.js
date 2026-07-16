async function loadDashboard(options = {}) {
    try {
        const dashboard = await getJson("/dashboard/my-summary");
        applyDashboardProfile(dashboard);
        renderDashboardMetrics(dashboard.metrics || []);
        renderDashboardTodos(dashboard.todos || []);
        if (options.notify) showMessage("首页看板已刷新", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function applyDashboardProfile(dashboard) {
    document.getElementById("dashboard-role-name").textContent = dashboard.roleName || dashboard.primaryRole;
    document.getElementById("dashboard-scope").textContent = `数据范围：${dashboard.dataScope || ""}`;
    document.getElementById("dashboard-restrictions").innerHTML = `
        <h4>权限边界</h4>
        <ul class="boundary-list">${(dashboard.prohibitedActions || []).map(item => `<li>${escapeHtml(item)}</li>`).join("")}</ul>`;

    const visibleModules = new Set(dashboard.visibleModules || ["dashboard"]);
    document.querySelectorAll(".sidebar button[data-tab]").forEach(button => {
        const visible = (button.dataset.tab === "profile" || visibleModules.has(button.dataset.tab))
            && !button.classList.contains("permission-hidden");
        button.classList.toggle("module-hidden", !visible);
    });
    document.querySelectorAll(".panel").forEach(panel => {
        if (panel.id !== "dashboard" && !visibleModules.has(panel.id)) panel.classList.remove("active");
    });
}

function renderDashboardMetrics(metrics) {
    const container = document.getElementById("dashboard-metrics");
    const symbols = ["◆", "●", "■", "▲", "▶", "◇", "□", "○"];
    container.innerHTML = metrics.map((metric, index) => `
        <button type="button" class="metric metric-${escapeHtml(metric.level || "normal")}" data-metric-tab="${escapeHtml(metric.targetTab || "dashboard")}">
            <span class="metric-icon">${symbols[index % symbols.length]}</span>
            <span class="metric-label">${escapeHtml(metric.label)}</span>
            <strong>${escapeHtml(metric.value ?? "0")}<small>${escapeHtml(metric.unit || "")}</small></strong>
            <span class="metric-trend"><i style="--h:35%"></i><i style="--h:52%"></i><i style="--h:42%"></i><i style="--h:68%"></i><i style="--h:58%"></i><i style="--h:82%"></i></span>
        </button>
    `).join("");
    container.querySelectorAll("[data-metric-tab]").forEach(button => {
        button.addEventListener("click", () => {
            switchTab(button.dataset.metricTab);
            showMessage("已跳转到对应模块", "ok");
        });
    });
}

function renderDashboardTodos(todos) {
    const allowed = todos.filter(todo => !todo.requiredPermission || hasPermission(todo.requiredPermission));
    document.getElementById("dashboard-todo-count").textContent = allowed.length ? `${allowed.length} 项` : "暂无待办";
    const container = document.getElementById("dashboard-todos");
    if (!allowed.length) {
        container.innerHTML = `<div class="empty-state">暂无待办事项</div>`;
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
        button.addEventListener("click", () => {
            switchTab(button.dataset.todoTab, button.dataset.todoAnchor);
            showMessage("已定位到待处理事项", "ok");
        });
    });
}

async function loadTraces(options = {}) {
    try {
        const rows = await getJson("/product-traces");
        renderTable("trace-table", rows, [
            { key: "traceId", label: "ID" },
            { key: "traceCode", label: "追溯码" },
            { key: "orderId", label: "订单" },
            { key: "taskId", label: "任务" },
            { key: "workOrderId", label: "工单" },
            { key: "batchNo", label: "批次" },
            { key: "traceStatus", label: "状态", render: row => traceStatusText(row.traceStatus) }
        ]);
        if (options.notify) showMessage("追溯记录已刷新", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function loadFeedback(workOrderId = 1, options = {}) {
    try {
        const rows = await getJson(`/management-feedback?workOrderId=${workOrderId}`);
        renderTable("feedback-table", rows, [
            { key: "feedbackId", label: "ID" },
            { key: "feedbackNo", label: "编号" },
            { key: "workOrderId", label: "工单" },
            { key: "feedbackType", label: "类型" },
            { key: "feedbackContent", label: "内容" },
            { key: "feedbackStatus", label: "状态", render: row => feedbackStatusText(row.feedbackStatus) }
        ], [
            { name: "close-feedback", label: "关闭", idKey: "feedbackId", permission: "feedback.close", visible: row => row.feedbackStatus === "OPEN", handler: closeFeedback }
        ]);
        if (options.notify) showMessage("管理反馈已刷新", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function closeFeedback(id) {
    try {
        await postJson(`/management-feedback/${id}/close`);
        showMessage("反馈已关闭", "ok");
        const workOrderId = document.querySelector("#feedback-filter-form [name='workOrderId']").value || 1;
        await loadFeedback(workOrderId);
        await loadDashboard();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function bindDashboardEvents() {
    updateDashboardClock();
    window.setInterval(updateDashboardClock, 1000);
    document.getElementById("refresh-dashboard")?.addEventListener("click", () => loadDashboard({ notify: true }));
    document.getElementById("refresh-trace")?.addEventListener("click", () => loadTraces({ notify: true }));
    document.getElementById("refresh-feedback")?.addEventListener("click", () => {
        const workOrderId = document.querySelector("#feedback-filter-form [name='workOrderId']").value || 1;
        loadFeedback(workOrderId, { notify: true });
    });
    document.getElementById("trace-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            await postJson("/product-traces", { ...formToObject(event.target), createdAt: nowIsoLocal() });
            showMessage("追溯记录已创建", "ok");
            event.target.reset();
            await loadTraces();
        } catch (error) {
            showMessage(toChineseError(error), "error");
        }
    });
    document.getElementById("trace-search-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const traceCode = event.target.traceCode.value.trim();
        if (!traceCode) return;
        const trace = await getJson(`/product-traces/${encodeURIComponent(traceCode)}`);
        document.getElementById("trace-detail").innerHTML = `<div class="detail"><strong>${escapeHtml(trace.traceCode)}</strong><div>订单：${escapeHtml(trace.orderId)}，任务：${escapeHtml(trace.taskId)}，工单：${escapeHtml(trace.workOrderId)}</div><div>批次：${escapeHtml(trace.batchNo)}，状态：${escapeHtml(displayText(trace.traceStatus))}</div></div>`;
    });
    document.getElementById("feedback-filter-form")?.addEventListener("submit", event => {
        event.preventDefault();
        loadFeedback(event.target.workOrderId.value || 1, { notify: true });
    });
    document.getElementById("feedback-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            const payload = { ...formToObject(event.target), feedbackStatus: "OPEN", createdAt: nowIsoLocal(), closedAt: null };
            await postJson("/management-feedback", payload);
            showMessage("管理反馈已创建", "ok");
            event.target.reset();
            await loadFeedback(payload.workOrderId || 1);
        } catch (error) {
            showMessage(toChineseError(error), "error");
        }
    });
}

function renderTraceChain(chain) {
    const trace = chain.trace || {};
    const workReports = chain.workReports || [];
    const inspections = chain.qualityInspections || [];
    const reworks = chain.reworkOrders || [];
    const qualityTraces = chain.qualityTraces || [];
    document.getElementById("trace-detail").innerHTML = `
        <div class="detail">
            <strong>${escapeHtml(trace.traceCode || "")}</strong>
            <div>订单：${escapeHtml(trace.orderId)}，任务：${escapeHtml(trace.taskId)}，工单：${escapeHtml(trace.workOrderId)}</div>
            <div>批次：${escapeHtml(trace.batchNo)}，状态：${traceStatusText(trace.traceStatus)}</div>
            <div>报工 ${workReports.length} 条，质检 ${inspections.length} 条，返工 ${reworks.length} 条，质量追溯 ${qualityTraces.length} 条</div>
        </div>`;
}

function updateDashboardClock() {
    const clock = document.getElementById("dashboard-clock");
    if (clock) clock.textContent = new Intl.DateTimeFormat("zh-CN", { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false }).format(new Date());
}

function traceStatusText(status) {
    return {
        NORMAL: "正常",
        QUALITY_RISK: "质量风险",
        REWORKED: "已返工",
        SCRAPPED: "已报废"
    }[status] || escapeHtml(status || "");
}

function feedbackStatusText(status) {
    return {
        OPEN: "待处理",
        CLOSED: "已关闭"
    }[status] || escapeHtml(status || "");
}
