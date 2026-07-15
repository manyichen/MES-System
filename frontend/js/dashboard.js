let tireLabelRows = [];
let tirePreviewUrls = [];

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

    const isGeneralManager = dashboard.primaryRole === "GENERAL_MANAGER" || hasRole("GENERAL_MANAGER");
    const visibleModules = isGeneralManager
        ? new Set(["executiveOverview", "productionLive", "departmentReports", "managementAudit"])
        : new Set(dashboard.visibleModules || ["dashboard"]);
    document.body.classList.toggle("executive-session", isGeneralManager);
    document.querySelectorAll(".sidebar button[data-tab]").forEach(button => {
        const visible = (button.dataset.tab === "profile" || visibleModules.has(button.dataset.tab))
            && !button.classList.contains("permission-hidden");
        button.classList.toggle("module-hidden", !visible);
    });
    document.querySelectorAll(".general-manager-nav.nav-group").forEach(group => {
        group.classList.toggle("module-hidden", !isGeneralManager);
    });
    document.querySelectorAll(".panel").forEach(panel => {
        if (panel.id !== "profile" && !visibleModules.has(panel.id)) panel.classList.remove("active");
    });
    if (typeof refreshNavigationGroupVisibility === "function") refreshNavigationGroupVisibility();
    if (isGeneralManager && !document.getElementById("executiveOverview")?.classList.contains("active")) {
        window.requestAnimationFrame(() => switchTab("executiveOverview"));
    }
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
        const [rows] = await Promise.all([getJson("/product-traces"), loadTireLabels()]);
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

async function loadTireLabels() {
    tireLabelRows = await getJson("/tire-labels");
    renderTable("tire-label-table", tireLabelRows, [
        { key: "tireId", label: "ID" },
        { key: "serialNo", label: "轮胎序列号" },
        { key: "productName", label: "产品" },
        { key: "productModel", label: "规格" },
        { key: "batchNo", label: "批次" },
        { key: "warehouseName", label: "入库仓库" },
        { key: "tireStatus", label: "状态" },
        { key: "printCount", label: "打印次数" }
    ], [
        { name: "preview-tire-label", label: "预览", idKey: "tireId", handler: previewTireLabel },
        { name: "open-tire-pdf", label: "PDF", idKey: "tireId", handler: openTirePdf },
        { name: "open-public-trace", label: "扫码页", idKey: "tireId", handler: openPublicTrace },
        { name: "print-tire-label", label: "模拟打印", idKey: "tireId", permission: "trace.tire.print", handler: printTireLabel }
    ]);
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
    document.getElementById("tire-label-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const status = document.getElementById("tire-label-form-status");
        if (status) status.textContent = "正在读取质检与入库信息并生成文件…";
        try {
            const result = await postJson("/tire-labels/generate", { ...formToObject(event.target), publicBaseUrl: window.location.origin });
            if (status) status.textContent = `已生成 ${result.generatedQuantity} 条轮胎二维码，剩余可生成 ${result.remainingQuantity} 条。`;
            showMessage(`已为 ${result.generatedQuantity} 条轮胎生成独立二维码`, "ok");
            await loadTireLabels();
            if (result.tires?.length) await previewTireLabel(result.tires[0].tireId);
        } catch (error) {
            const message = toChineseError(error);
            if (status) status.textContent = `生成失败：${message}`;
            showMessage(message, "error");
        }
    });
    document.getElementById("trace-search-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            const traceCode = event.target.traceCode.value.trim();
            if (!traceCode) {
                showMessage("请输入追溯码或ID", "error");
                return;
            }
            const chain = await getJson(`/product-traces/${encodeURIComponent(traceCode)}`);
            renderTraceChain(chain);
            showMessage("追溯链路已加载", "ok");
        } catch (error) {
            showMessage(toChineseError(error), "error");
        }
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

async function previewTireLabel(id) {
    try {
        releaseTirePreviewUrls();
        const row = tireLabelRows.find(item => String(item.tireId) === String(id));
        const [qrBlob, labelBlob] = await Promise.all([
            getBlob(`/tire-labels/${id}/qrcode`),
            getBlob(`/tire-labels/${id}/label`)
        ]);
        const qrUrl = URL.createObjectURL(qrBlob);
        const labelUrl = URL.createObjectURL(labelBlob);
        tirePreviewUrls = [qrUrl, labelUrl];
        const container = document.getElementById("tire-label-preview");
        container.innerHTML = `<div class="section-heading"><h3>二维码与标签预览</h3><span>${escapeHtml(row?.serialNo || id)}</span></div>
            <div class="tire-preview-grid"><figure><img src="${qrUrl}" alt="轮胎二维码"><figcaption>二维码原图</figcaption></figure>
            <figure class="label-preview"><img src="${labelUrl}" alt="轮胎打印标签"><figcaption>打印标签</figcaption></figure></div>
            <div class="tire-preview-actions"><button type="button" data-preview-pdf>查看PDF</button><button type="button" class="secondary" data-preview-public>打开微信扫码页</button></div>`;
        container.querySelector("[data-preview-pdf]")?.addEventListener("click", () => openTirePdf(id));
        container.querySelector("[data-preview-public]")?.addEventListener("click", () => openPublicTrace(id));
        container.scrollIntoView({ behavior: "smooth", block: "center" });
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function openTirePdf(id) {
    try {
        const blob = await getBlob(`/tire-labels/${id}/document`);
        openBlob(blob);
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function openPublicTrace(id) {
    const row = tireLabelRows.find(item => String(item.tireId) === String(id));
    if (!row?.targetUrl) return showMessage("该轮胎尚未生成扫码地址", "error");
    window.open(row.targetUrl, "_blank", "noopener");
}

async function printTireLabel(id) {
    const printWindow = window.open("", "_blank", "width=520,height=760");
    if (!printWindow) return showMessage("浏览器已拦截打印窗口，请允许弹出窗口后重试", "error");
    printWindow.document.write("<p style='font-family:sans-serif;padding:24px'>正在准备轮胎标签…</p>");
    try {
        const blob = await getBlob(`/tire-labels/${id}/label`);
        await postJson(`/tire-labels/${id}/print`, { remark: "MES 页面模拟打印" });
        const url = URL.createObjectURL(blob);
        printWindow.document.open();
        printWindow.document.write(`<!doctype html><html><head><title>轮胎标签打印</title><style>html,body{margin:0;background:#fff}body{display:grid;place-items:center;min-height:100vh}img{width:40mm;height:auto}@media print{@page{size:45mm 65mm;margin:2mm}body{min-height:0}img{width:40mm}}</style></head><body><img src="${url}" onload="setTimeout(()=>window.print(),200)"></body></html>`);
        printWindow.document.close();
        showMessage("已打开模拟打印窗口并记录打印任务", "ok");
        await loadTireLabels();
    } catch (error) {
        printWindow.close();
        showMessage(toChineseError(error), "error");
    }
}

function openBlob(blob) {
    const url = URL.createObjectURL(blob);
    window.open(url, "_blank", "noopener");
    window.setTimeout(() => URL.revokeObjectURL(url), 60000);
}

function releaseTirePreviewUrls() {
    tirePreviewUrls.forEach(url => URL.revokeObjectURL(url));
    tirePreviewUrls = [];
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
