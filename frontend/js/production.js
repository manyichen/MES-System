const REPORTABLE_WORK_ORDER_STATUSES = new Set(["DISPATCHED", "RECEIVED", "RUNNING"]);

async function refreshProduction(options = {}) {
    try {
        const workOrders = await getJson("/work-orders");
        const [reports, wages] = await Promise.all([getJson("/work-reports"), getJson("/piecework-wages")]);
        refreshReportableWorkOrdersFrom(workOrders);
        renderProductionFocus(workOrders, reports, wages);
        renderTable("reportTable", reports, [
            { title: "ID", key: "reportId" },
            { title: "\u7f16\u53f7", key: "reportNo" },
            { title: "\u5de5\u5355", key: "workOrderId" },
            { title: "\u6279\u6b21", key: "batchNo" },
            { title: "\u5408\u683c", key: "qualifiedQty" },
            { title: "\u72b6\u6001", key: "reportStatus" },
            { title: "\u64cd\u4f5c", render: renderReportActions }
        ]);
        if (Array.isArray(wages)) {
            renderTable("wageTable", wages, [
                { title: "ID", key: "wageId" },
                { title: "\u62a5\u5de5", key: "reportId" },
                { title: "\u5355\u4ef7", key: "pieceRate" },
                { title: "\u91d1\u989d", key: "wageAmount" },
                { title: "\u72b6\u6001", key: "settlementStatus" },
                { title: "\u64cd\u4f5c", render: row => `<button onclick="showWageDetail(${row.wageId})">\u8be6\u60c5</button>` }
            ]);
        } else {
            renderTable("wageTable", [wages], [
                { title: "\u8bb0\u5f55\u6570", key: "recordCount" },
                { title: "\u5458\u5de5\u6570", key: "operatorCount" },
                { title: "\u5408\u683c\u4ea7\u91cf", key: "qualifiedQty" },
                { title: "\u5de5\u8d44\u6c47\u603b", key: "wageAmount" }
            ]);
        }
        if (options.notify) showMessage("\u751f\u4ea7\u62a5\u5de5\u6570\u636e\u5df2\u5237\u65b0", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function refreshReportableWorkOrders() {
    refreshReportableWorkOrdersFrom(await getJson("/work-orders"));
}

function refreshReportableWorkOrdersFrom(workOrders) {
    const select = document.getElementById("reportWorkOrderSelect");
    if (!select) return;
    const reportable = workOrders.filter(order => REPORTABLE_WORK_ORDER_STATUSES.has(order.workOrderStatus));
    select.innerHTML = "";
    if (!reportable.length) {
        const option = document.createElement("option");
        option.value = "";
        option.textContent = "\u6682\u65e0\u53ef\u62a5\u5de5\u5de5\u5355";
        select.appendChild(option);
        select.disabled = true;
        return;
    }
    select.disabled = false;
    for (const order of reportable) {
        const option = document.createElement("option");
        option.value = order.workOrderId;
        option.textContent = `${order.workOrderNo || "WO-" + order.workOrderId} / ${order.workOrderStatus} / \u8ba1\u5212 ${order.plannedQty ?? "-"}`;
        option.dataset.batchNo = order.batchNo || "";
        select.appendChild(option);
    }
    syncReportBatchNo();
}

function renderProductionFocus(workOrders, reports, wages) {
    const grid = document.querySelector("#production .grid");
    if (!grid) return;
    let focus = document.getElementById("productionFocus");
    if (!focus) {
        focus = document.createElement("div");
        focus.id = "productionFocus";
        focus.className = "tool wide workflow-focus b-focus";
        grid.prepend(focus);
    }
    const reportable = workOrders.filter(order => REPORTABLE_WORK_ORDER_STATUSES.has(order.workOrderStatus)).length;
    const submitted = reports.filter(row => row.reportStatus === "SUBMITTED").length;
    const approved = reports.filter(row => row.reportStatus === "APPROVED").length;
    const wageCount = Array.isArray(wages) ? wages.length : Number(wages?.recordCount || 0);
    focus.innerHTML = `
        <h3>\u751f\u4ea7\u6267\u884c\u5de5\u4f5c\u53f0</h3>
        <p class="focus-hint">\u64cd\u4f5c\u5de5\u4f18\u5148\u9009\u62e9\u53ef\u62a5\u5de5\u5de5\u5355\uff0c\u8f66\u95f4\u7ba1\u7406\u5458\u4f18\u5148\u5904\u7406\u5f85\u5ba1\u6838\u62a5\u5de5\u3002</p>
        <div class="workflow-steps">
            <button type="button" onclick="scrollBSection('reportForm')"><strong>${reportable}</strong><span>\u53ef\u62a5\u5de5\u5de5\u5355</span></button>
            <button type="button" onclick="scrollBSection('reportTable')"><strong>${submitted}</strong><span>\u5f85\u5ba1\u6838\u62a5\u5de5</span></button>
            <button type="button" onclick="scrollBSection('reportTable')"><strong>${approved}</strong><span>\u5df2\u5ba1\u6838\u62a5\u5de5</span></button>
            <button type="button" onclick="scrollBSection('wageTable')"><strong>${wageCount}</strong><span>\u8ba1\u4ef6\u5de5\u8d44\u8bb0\u5f55</span></button>
        </div>`;
}

function syncReportBatchNo() {
    const select = document.getElementById("reportWorkOrderSelect");
    const batchInput = document.querySelector("#reportForm [name='batchNo']");
    if (!select || !batchInput) return;
    const selected = select.options[select.selectedIndex];
    if (selected?.dataset.batchNo) batchInput.value = selected.dataset.batchNo;
}

function renderReportActions(row) {
    const actions = [`<button onclick="showReportDetail(${row.reportId})">\u8be6\u60c5</button>`];
    if (row.reportStatus === "SUBMITTED" && hasPermission("production.report.review")) {
        actions.push(`<button onclick="approveReport(${row.reportId})">\u5ba1\u6838</button>`);
    }
    return actions.join("");
}

async function showReportDetail(id) {
    try {
        renderDetail("productionDetail", await getJson(`/work-reports/${id}`), "\u62a5\u5de5\u5355\u8be6\u60c5");
        showMessage("\u62a5\u5de5\u8be6\u60c5\u5df2\u52a0\u8f7d", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function showWageDetail(id) {
    try {
        renderDetail("productionDetail", await getJson(`/piecework-wages/${id}`), "\u8ba1\u4ef6\u5de5\u8d44\u8be6\u60c5");
        showMessage("\u8ba1\u4ef6\u5de5\u8d44\u8be6\u60c5\u5df2\u52a0\u8f7d", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function approveReport(id) {
    try {
        await postJson(`/work-reports/${id}/approve`);
        showMessage("\u62a5\u5de5\u5df2\u5ba1\u6838\uff0c\u8ba1\u4ef6\u5de5\u8d44\u5df2\u751f\u6210", "ok");
        await refreshProduction();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

document.getElementById("refreshProduction")?.addEventListener("click", () => refreshProduction({ notify: true }));
document.getElementById("reportWorkOrderSelect")?.addEventListener("change", syncReportBatchNo);
document.getElementById("reportForm")?.addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.target);
    try {
        if (!form.get("workOrderId")) {
            showMessage("\u6682\u65e0\u53ef\u62a5\u5de5\u5de5\u5355\uff0c\u8bf7\u5148\u6d3e\u53d1\u5e76\u63a5\u6536\u5de5\u5355", "error");
            return;
        }
        await postJson("/work-reports", {
            workOrderId: Number(form.get("workOrderId")),
            batchNo: String(form.get("batchNo") || ""),
            reportQty: Number(form.get("reportQty")),
            qualifiedQty: Number(form.get("qualifiedQty")),
            defectQty: Number(form.get("defectQty")),
            workHours: Number(form.get("workHours"))
        });
        showMessage("\u62a5\u5de5\u5df2\u63d0\u4ea4", "ok");
        await refreshProduction();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
});

function relabelProductionStaticText() {
    const panel = document.getElementById("production");
    if (!panel) return;
    panel.querySelector("h2").textContent = "\u751f\u4ea7\u62a5\u5de5";
    document.getElementById("refreshProduction").textContent = "\u5237\u65b0";
    const titles = panel.querySelectorAll(".tool > h3");
    const names = ["\u63d0\u4ea4\u62a5\u5de5", "\u62a5\u5de5\u5355", "\u8ba1\u4ef6\u5de5\u8d44", "\u751f\u4ea7\u8be6\u60c5"];
    titles.forEach((title, index) => {
        if (names[index]) title.textContent = names[index];
    });
    document.querySelector("#reportForm button[type='submit']").textContent = "\u63d0\u4ea4\u62a5\u5de5";
}

relabelProductionStaticText();
