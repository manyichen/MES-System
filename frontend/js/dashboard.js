async function loadDashboard() {
    try {
        const rows = await getJson("/dashboard/summary");
        const metrics = rows && rows.length ? rows : [
            { metricName: "订单进度", metricValue: "待接入", metricType: "ORDER" },
            { metricName: "生产数量", metricValue: "待接入", metricType: "PRODUCTION" },
            { metricName: "质量结果", metricValue: "待接入", metricType: "QUALITY" },
            { metricName: "设备状态", metricValue: "待接入", metricType: "EQUIPMENT" }
        ];
        document.getElementById("dashboard-metrics").innerHTML = metrics.map(metric => `
            <div class="metric">
                <span>${metric.metricName || metric.metricKey}</span>
                <strong>${metric.metricValue || "-"}</strong>
                <small>${metric.metricType || ""}</small>
            </div>
        `).join("");
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function loadTraces() {
    try {
        const rows = await getJson("/product-traces");
        renderTable("trace-table", rows, [
            { key: "traceId", label: "ID" },
            { key: "traceCode", label: "追溯码" },
            { key: "orderId", label: "订单" },
            { key: "taskId", label: "任务" },
            { key: "workOrderId", label: "工单" },
            { key: "batchNo", label: "批次" },
            { key: "traceStatus", label: "状态" }
        ]);
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function loadFeedback(workOrderId = 1) {
    try {
        const rows = await getJson(`/management-feedback?workOrderId=${workOrderId}`);
        renderTable("feedback-table", rows, [
            { key: "feedbackId", label: "ID" },
            { key: "feedbackNo", label: "编号" },
            { key: "workOrderId", label: "工单" },
            { key: "feedbackType", label: "类型" },
            { key: "feedbackContent", label: "内容" },
            { key: "feedbackStatus", label: "状态" }
        ], [
            { name: "close-feedback", label: "关闭", idKey: "feedbackId", handler: closeFeedback }
        ]);
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function closeFeedback(id) {
    await postJson(`/management-feedback/${id}/close`);
    showMessage("反馈已关闭");
    const workOrderId = document.querySelector("#feedback-filter-form [name='workOrderId']").value || 1;
    loadFeedback(workOrderId);
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
        const payload = {
            ...formToObject(event.target),
            createdAt: nowIsoLocal()
        };
        await postJson("/product-traces", payload);
        showMessage("追溯记录已创建");
        event.target.reset();
        loadTraces();
    });

    document.getElementById("trace-search-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const traceCode = event.target.traceCode.value.trim();
        if (!traceCode) return;
        const trace = await getJson(`/product-traces/${encodeURIComponent(traceCode)}`);
        document.getElementById("trace-detail").innerHTML = `
            <div class="detail">
                <strong>${trace.traceCode}</strong>
                <div>订单：${trace.orderId || ""}，任务：${trace.taskId || ""}，工单：${trace.workOrderId || ""}</div>
                <div>批次：${trace.batchNo || ""}，状态：${trace.traceStatus || ""}</div>
            </div>
        `;
    });

    document.getElementById("feedback-filter-form")?.addEventListener("submit", event => {
        event.preventDefault();
        loadFeedback(event.target.workOrderId.value || 1);
    });

    document.getElementById("feedback-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const payload = {
            ...formToObject(event.target),
            feedbackStatus: "OPEN",
            createdAt: nowIsoLocal(),
            closedAt: null
        };
        await postJson("/management-feedback", payload);
        showMessage("管理反馈已创建");
        const workOrderId = payload.workOrderId || 1;
        event.target.reset();
        loadFeedback(workOrderId);
    });
}
