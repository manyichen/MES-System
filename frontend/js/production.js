const REPORTABLE_WORK_ORDER_STATUSES = new Set(["DISPATCHED", "RECEIVED", "RUNNING", "IN_PROGRESS"]);

let productionCache = {
    workOrders: [],
    reportableWorkOrders: [],
    reports: [],
    wages: [],
    products: [],
    tasks: [],
    lines: [],
    routes: []
};

async function refreshProduction(options = {}) {
    try {
        const optionalList = path => getJson(path).catch(() => []);
        const [workOrders, reports, wages, products, tasks, lines, routes] = await Promise.all([
            getJson("/work-orders"),
            getJson("/work-reports"),
            getJson("/piecework-wages"),
            optionalList("/products"),
            optionalList("/production-tasks"),
            optionalList("/production-lines"),
            optionalList("/process-routes")
        ]);
        const safeReports = Array.isArray(reports) ? reports : [];
        const safeWages = Array.isArray(wages) ? wages : [];
        const sortedReports = sortReportsForReview(safeReports);
        const reportableWorkOrders = canCreateWorkReport()
            ? (Array.isArray(workOrders) ? workOrders : []).filter(isReportableWorkOrder)
            : [];
        productionCache = {
            workOrders: Array.isArray(workOrders) ? workOrders : [],
            reportableWorkOrders,
            reports: sortedReports,
            wages: safeWages,
            products: Array.isArray(products) ? products : [],
            tasks: Array.isArray(tasks) ? tasks : [],
            lines: Array.isArray(lines) ? lines : [],
            routes: Array.isArray(routes) ? routes : []
        };
        applyProductionRolePresentation();
        renderProductionFocus(reportableWorkOrders, sortedReports, wages);
        renderReportableWorkOrderTable(reportableWorkOrders, sortedReports);
        renderWorkReportTable(sortedReports);
        renderWageTable(wages);
        if (options.notify) showMessage("生产报工数据已刷新", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function isReportableWorkOrder(order) {
    return REPORTABLE_WORK_ORDER_STATUSES.has(order.workOrderStatus);
}

function sortReportsForReview(reports) {
    return [...reports].sort((left, right) => {
        const leftRank = left.reportStatus === "SUBMITTED" ? 0 : 1;
        const rightRank = right.reportStatus === "SUBMITTED" ? 0 : 1;
        const rankDiff = leftRank - rightRank;
        if (rankDiff !== 0) return rankDiff;
        return Number(right.reportId || 0) - Number(left.reportId || 0);
    });
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
    const submitted = reports.filter(row => row.reportStatus === "SUBMITTED").length;
    const approved = reports.filter(row => row.reportStatus === "APPROVED").length;
    const wageCount = Array.isArray(wages) ? wages.length : Number(wages?.recordCount || 0);
    const todayReports = reports.filter(row => reportDateKey(row.reportTime) === todayDateKey());
    const todayReportQty = sumBy(todayReports, "reportQty");
    const todayQualifiedQty = sumBy(todayReports, "qualifiedQty");
    const todayDefectQty = sumBy(todayReports, "defectQty");
    const isWorkshopManager = hasRole("WORKSHOP_MANAGER");
    const focusHint = isWorkshopManager
        ? "车间管理员优先处理待审核报工，并查看本车间产线的生产与计件汇总。"
        : "操作工优先选择可报工工单，车间管理员优先处理待审核报工。";
    const workflowButtons = [
        canCreateWorkReport() ? `<button type="button" onclick="scrollProductionSection('reportableWorkOrderTable')"><strong>${workOrders.length}</strong><span>可报工工单</span></button>` : "",
        canReviewReport() ? `<button type="button" onclick="scrollProductionSection('reportTable')"><strong>${submitted}</strong><span>待审核报工</span></button>` : "",
        `<button type="button" onclick="scrollProductionSection('reportTable')"><strong>${approved}</strong><span>已审核报工</span></button>`,
        `<button type="button" onclick="scrollProductionSection('wageTable')"><strong>${wageCount}</strong><span>${isWorkshopManager ? "计件工资汇总" : "计件工资记录"}</span></button>`,
        `<button type="button" onclick="scrollProductionSection('reportTable')"><strong>${todayReportQty}</strong><span>今日报工数量</span></button>`,
        `<button type="button" onclick="scrollProductionSection('reportTable')"><strong>${todayQualifiedQty}</strong><span>今日合格数</span></button>`,
        `<button type="button" onclick="scrollProductionSection('reportTable')"><strong>${todayDefectQty}</strong><span>今日不良数</span></button>`
    ].filter(Boolean).join("");
    focus.innerHTML = `
        <h3>生产执行工作台</h3>
        <p class="focus-hint">${focusHint}</p>
        <div class="workflow-steps">
            ${workflowButtons}
        </div>`;
}

function renderReportableWorkOrderTable(workOrders, reports) {
    renderTable("reportableWorkOrderTable", workOrders, [
        { title: "工单编号", key: "workOrderNo", render: row => escapeHtml(row.workOrderNo || `WO-${row.workOrderId}`) },
        { title: "产品/任务", render: row => escapeHtml(productTaskLabel(row)) },
        { title: "产线/工序", render: row => escapeHtml(lineProcessLabel(row)) },
        { title: "批次号", key: "batchNo" },
        { title: "计划数量", key: "plannedQty" },
        { title: "已报工数量", render: row => escapeHtml(reportedQtyForWorkOrder(row.workOrderId, reports)) },
        { title: "当前状态", key: "workOrderStatus", render: row => escapeHtml(workOrderStatusForProduction(row.workOrderStatus)) },
        { title: "操作", render: row => canCreateWorkReport() ? `<button type="button" onclick="openReportFormForWorkOrder(${Number(row.workOrderId)})">报工</button>` : "-" }
    ]);
}

function renderWorkReportTable(reports) {
    renderTable("reportTable", reports, [
        { title: "报工编号", key: "reportNo", render: row => escapeHtml(row.reportNo || `WR-${row.reportId}`) },
        { title: "工单编号", render: row => escapeHtml(workOrderNoForReport(row)) },
        { title: "批次", key: "batchNo" },
        { title: "报工数量", key: "reportQty" },
        { title: "合格数量", key: "qualifiedQty" },
        { title: "不良数量", key: "defectQty" },
        { title: "工时", key: "workHours" },
        { title: "状态", key: "reportStatus", render: row => escapeHtml(reportStatusText(row.reportStatus)) },
        { title: "操作", render: renderReportActions }
    ]);
}

function renderWageTable(wages) {
    if (Array.isArray(wages)) {
        renderTable("wageTable", wages, [
            { title: "工单编号", render: row => escapeHtml(workOrderNoForWage(row)) },
            { title: "报工单编号", render: row => escapeHtml(reportNoForWage(row)) },
            { title: "合格数量", key: "qualifiedQty" },
            { title: "单价", key: "pieceRate" },
            { title: "金额", key: "wageAmount" },
            { title: "结算状态", key: "settlementStatus" },
            { title: "生成时间", key: "createdAt", render: row => escapeHtml(formatProductionDateTime(row.createdAt)) },
            { title: "操作", render: row => `<button type="button" onclick="showWageDetail(${Number(row.wageId)})">详情</button>` }
        ]);
        return;
    }
    renderTable("wageTable", wages ? [wages] : [], [
        { title: "记录数", key: "recordCount" },
        { title: "员工数", key: "operatorCount" },
        { title: "合格产量", key: "qualifiedQty" },
        { title: "工资汇总", key: "wageAmount" }
    ]);
}

function openReportFormForWorkOrder(workOrderId) {
    if (!canCreateWorkReport()) {
        showMessage("当前角色不能提交生产报工", "error");
        return;
    }
    const order = productionCache.reportableWorkOrders.find(item => Number(item.workOrderId) === Number(workOrderId));
    if (!order) {
        showMessage("未找到可报工工单，请刷新后重试", "error");
        return;
    }
    showWorkOrderReportDialog(order);
}

function showWorkOrderReportDialog(order) {
    const remainingQty = Math.max(0, Number(order.plannedQty || 0) - reportedQtyForWorkOrder(order.workOrderId, productionCache.reports));
    const defaultQty = remainingQty || Number(order.plannedQty || 0);
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal-card report-edit-modal">
            <h3>填写报工信息</h3>
            <form id="workOrderReportDialogForm" class="production-report-form">
                <input type="hidden" name="workOrderId" value="${escapeHtml(order.workOrderId)}">
                <div class="edit-summary">
                    ${detailField("工单编号", order.workOrderNo || `WO-${order.workOrderId}`)}
                    ${detailField("产品/任务", productTaskLabel(order))}
                    ${detailField("产线/工序", lineProcessLabel(order))}
                    ${detailField("当前状态", workOrderStatusForProduction(order.workOrderStatus))}
                </div>
                <label>批次号 <input name="batchNo" value="${escapeHtml(order.batchNo || "")}"></label>
                <label>报工数量 <input name="reportQty" type="number" min="0" required value="${escapeHtml(defaultQty)}"></label>
                <label>合格数量 <input name="qualifiedQty" type="number" min="0" required value="${escapeHtml(defaultQty)}"></label>
                <label>不良数量 <input name="defectQty" type="number" min="0" required value="0"></label>
                <label>工时 <input name="workHours" type="number" min="0" step="0.1" value="8"></label>
                <label class="wide-field">备注/异常说明 <textarea name="remark" rows="3"></textarea></label>
                <div class="modal-actions">
                    <button type="button" id="workOrderReportCancel">取消</button>
                    <button type="submit">提交报工</button>
                </div>
            </form>
        </div>`;
    document.body.appendChild(mask);
    mask.querySelector("#workOrderReportCancel")?.addEventListener("click", () => mask.remove());
    mask.addEventListener("click", event => {
        if (event.target === mask) mask.remove();
    });
    mask.querySelector("#workOrderReportDialogForm")?.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            await postJson("/work-reports", reportPayloadFromForm(new FormData(event.target)));
            showMessage("报工已提交", "ok");
            mask.remove();
            await refreshProduction();
        } catch (error) {
            showMessage(toChineseError(error), "error");
        }
    });
}

function renderReportActions(row) {
    const actions = [`<button type="button" onclick="showReportDetail(${Number(row.reportId)})">详情</button>`];
    if (canEditReport(row)) {
        const label = row.reportStatus === "REJECTED" ? "重新提交" : "修改";
        actions.push(`<button type="button" onclick="openReportEdit(${Number(row.reportId)})">${label}</button>`);
    } else if (isPendingReport(row) && canReviewReport()) {
        actions.push(`<button type="button" onclick="openReportReview(${Number(row.reportId)})">审核</button>`);
    }
    return `<div class="row-actions">${actions.join("")}</div>`;
}

function canEditReport(row) {
    return hasPermission("production.report.update_own") && ["SUBMITTED", "REJECTED"].includes(row.reportStatus);
}

function isPendingReport(row) {
    const status = String(row.reportStatus || "").trim();
    return status === "SUBMITTED" || status === "已提交" || status === "待审核"
        || statusText(status) === "已提交" || statusText(status) === "待审核";
}

function canReviewReport() {
    return hasPermission("production.report.review");
}

function canCreateWorkReport() {
    return hasPermission("production.report.create") && !hasRole("WORKSHOP_MANAGER");
}

async function showReportDetail(id) {
    try {
        const report = await getJson(`/work-reports/${id}`);
        const wages = hasRole("WORKSHOP_MANAGER")
            ? []
            : await getJson(`/piecework-wages/by-report/${id}`).catch(() =>
                Array.isArray(productionCache.wages)
                    ? productionCache.wages.filter(wage => Number(wage.reportId) === Number(id))
                    : []);
        const workOrder = await workOrderForReport(report);
        showReportDetailDialog(report, workOrder, Array.isArray(wages) ? wages : []);
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function workOrderForReport(report) {
    const cached = productionCache.workOrders.find(item => Number(item.workOrderId) === Number(report.workOrderId));
    if (cached) return cached;
    return getJson(`/work-orders/${report.workOrderId}`).catch(() => null);
}

function showReportDetailDialog(report, workOrder, wages) {
    const sections = [
        {
            title: "工单基础信息",
            rows: [
                detailField("工单编号", workOrder?.workOrderNo || report.workOrderId),
                detailField("产品/任务", workOrder ? productTaskLabel(workOrder) : "-"),
                detailField("产线/工序", workOrder ? lineProcessLabel(workOrder) : "-"),
                detailField("批次号", report.batchNo || workOrder?.batchNo || "-"),
                detailField("计划数量", workOrder?.plannedQty ?? "-"),
                detailField("当前状态", workOrder ? workOrderStatusForProduction(workOrder.workOrderStatus) : "-")
            ]
        },
        {
            title: "报工明细",
            rows: [
                detailField("报工编号", report.reportNo || report.reportId),
                detailField("报工数量", report.reportQty),
                detailField("合格数量", report.qualifiedQty),
                detailField("不良数量", report.defectQty),
                detailField("工时", report.workHours),
                detailField("报工时间", formatProductionDateTime(report.reportTime)),
                detailField("备注/异常说明", report.remark || "-")
            ]
        },
        {
            title: "审核结果",
            rows: [
                detailField("状态", reportStatusText(report.reportStatus)),
                detailField("驳回原因", report.reportStatus === "REJECTED" ? (report.rejectReason || "暂未记录") : "-")
            ]
        }
    ];
    if (!hasRole("WORKSHOP_MANAGER")) {
        sections.push({
            title: "对应计件工资记录",
            rows: wages.length
                ? wages.flatMap(wage => [
                    detailField("计件记录", wage.wageId),
                    detailField("合格数量", wage.qualifiedQty),
                    detailField("单价", wage.pieceRate),
                    detailField("金额", wage.wageAmount),
                    detailField("结算状态", statusText(wage.settlementStatus || "")),
                    detailField("生成时间", formatProductionDateTime(wage.createdAt))
                ])
                : [detailField("计件工资", "暂无记录")]
        });
    }
    showProductionRecordDialog("报工单详情", sections);
}

function showProductionRecordDialog(title, sections) {
    const body = sections.map(section => `
        <section class="production-detail-section">
            <h4>${escapeHtml(section.title)}</h4>
            <div class="production-detail-grid">${section.rows.join("")}</div>
        </section>`).join("");
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal-card production-detail-modal">
            <div class="production-detail-head">
                <div>
                    <span>记录详情</span>
                    <h3>${escapeHtml(title)}</h3>
                </div>
                <button type="button" class="detail-close" aria-label="关闭">×</button>
            </div>
            <div class="production-detail-body">${body || "<p>暂无详情</p>"}</div>
            <div class="modal-actions">
                <button type="button" id="productionDetailClose">关闭</button>
            </div>
        </div>`;
    document.body.appendChild(mask);
    const close = () => mask.remove();
    mask.querySelector(".detail-close")?.addEventListener("click", close);
    mask.querySelector("#productionDetailClose")?.addEventListener("click", close);
    mask.addEventListener("click", event => {
        if (event.target === mask) close();
    });
}

async function showWageDetail(id) {
    try {
        const wage = await getJson(`/piecework-wages/${id}`);
        const report = productionCache.reports.find(item => Number(item.reportId) === Number(wage.reportId))
            || await getJson(`/work-reports/${wage.reportId}`).catch(() => null);
        const workOrder = report ? await workOrderForReport(report) : null;
        showProductionRecordDialog("计件工资详情", [
            {
                title: "工单与报工",
                rows: [
                    detailField("工单编号", workOrder?.workOrderNo || report?.workOrderId || "-"),
                    detailField("报工单编号", report?.reportNo || wage.reportId),
                    detailField("批次", report?.batchNo || workOrder?.batchNo || "-"),
                    detailField("报工状态", report ? reportStatusText(report.reportStatus) : "-")
                ]
            },
            {
                title: "计件工资",
                rows: [
                    detailField("计件记录ID", wage.wageId),
                    detailField("合格数量", wage.qualifiedQty),
                    detailField("单价", wage.pieceRate),
                    detailField("金额", wage.wageAmount),
                    detailField("结算状态", statusText(wage.settlementStatus || "")),
                    detailField("生成时间", formatProductionDateTime(wage.createdAt))
                ]
            }
        ]);
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function openReportEdit(id) {
    try {
        const report = await getJson(`/work-reports/${id}`);
        showReportEditDialog(report);
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function showReportEditDialog(report) {
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    const title = report.reportStatus === "REJECTED" ? "重新提交报工" : "修改报工";
    mask.innerHTML = `
        <div class="modal-card report-edit-modal">
            <h3>${escapeHtml(title)}</h3>
            <form id="reportEditForm" class="production-report-form">
                <input type="hidden" name="workOrderId" value="${escapeHtml(report.workOrderId)}">
                <div class="edit-summary">${detailField("报工单", report.reportNo || report.reportId)}${detailField("工单", workOrderNoForReport(report))}</div>
                <label>批次号 <input name="batchNo" value="${escapeHtml(report.batchNo || "")}"></label>
                <label>报工数量 <input name="reportQty" type="number" min="0" required value="${escapeHtml(report.reportQty ?? 0)}"></label>
                <label>合格数量 <input name="qualifiedQty" type="number" min="0" required value="${escapeHtml(report.qualifiedQty ?? 0)}"></label>
                <label>不良数量 <input name="defectQty" type="number" min="0" required value="${escapeHtml(report.defectQty ?? 0)}"></label>
                <label>工时 <input name="workHours" type="number" min="0" step="0.1" value="${escapeHtml(report.workHours ?? 0)}"></label>
                <label class="wide-field">备注/异常说明 <textarea name="remark" rows="3">${escapeHtml(report.remark || "")}</textarea></label>
                <div class="modal-actions">
                    <button type="button" id="reportEditCancel">取消</button>
                    <button type="submit">${report.reportStatus === "REJECTED" ? "重新提交" : "保存修改"}</button>
                </div>
            </form>
        </div>`;
    document.body.appendChild(mask);
    mask.querySelector("#reportEditCancel")?.addEventListener("click", () => mask.remove());
    mask.querySelector("#reportEditForm")?.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            await putJson(`/work-reports/${report.reportId}`, reportPayloadFromForm(new FormData(event.target)));
            showMessage(report.reportStatus === "REJECTED" ? "报工单已重新提交" : "报工单已更新", "ok");
            mask.remove();
            await refreshProduction();
        } catch (error) {
            showMessage(toChineseError(error), "error");
        }
    });
}

async function openReportReview(id) {
    try {
        const report = await getJson(`/work-reports/${id}`);
        showReportReviewDialog(report);
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function showReportReviewDialog(report) {
    const mask = document.createElement("div");
    mask.className = "modal-mask";
    mask.innerHTML = `
        <div class="modal-card report-review-modal">
            <h3>报工审核</h3>
            <div class="review-summary">
                ${reportReviewField("报工单", report.reportNo || report.reportId)}
                ${reportReviewField("工单", workOrderNoForReport(report))}
                ${reportReviewField("操作工", report.operatorId)}
                ${reportReviewField("批次", report.batchNo || "-")}
                ${reportReviewField("报工数量", report.reportQty)}
                ${reportReviewField("合格数", report.qualifiedQty)}
                ${reportReviewField("不良数", report.defectQty)}
                ${reportReviewField("工时", report.workHours)}
                ${reportReviewField("状态", reportStatusText(report.reportStatus || ""))}
                ${reportReviewField("报工时间", formatProductionDateTime(report.reportTime))}
            </div>
            <label>驳回理由<textarea id="reportRejectReason" rows="3" placeholder="驳回时必须填写原因，便于操作工修正"></textarea></label>
            <div class="modal-actions">
                <button type="button" id="reportReviewCancel">取消</button>
                <button type="button" id="reportReviewReject">驳回</button>
                <button type="button" id="reportReviewApprove">通过</button>
            </div>
        </div>`;
    document.body.appendChild(mask);
    mask.querySelector("#reportReviewCancel").addEventListener("click", () => mask.remove());
    mask.querySelector("#reportReviewApprove").addEventListener("click", async () => {
        await approveReport(report.reportId);
        mask.remove();
    });
    mask.querySelector("#reportReviewReject").addEventListener("click", async () => {
        const reason = mask.querySelector("#reportRejectReason").value.trim();
        if (!reason) {
            showMessage("请填写驳回理由", "error");
            return;
        }
        await rejectReport(report.reportId, reason);
        mask.remove();
    });
}

function reportReviewField(label, value) {
    return `<div><span>${escapeHtml(label)}</span><strong>${escapeHtml(value ?? "-")}</strong></div>`;
}

async function approveReport(id) {
    try {
        await postJson(`/work-reports/${id}/approve`);
        showMessage("报工已审核，计件工资已生成", "ok");
        await refreshProduction();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function rejectReport(id, reason) {
    try {
        await postJson(`/work-reports/${id}/reject`, { reason });
        showMessage("报工单已驳回", "ok");
        await refreshProduction();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

document.getElementById("refreshProduction")?.addEventListener("click", () => refreshProduction({ notify: true }));

function reportPayloadFromForm(form) {
    const reportQty = Number(form.get("reportQty")) || 0;
    const qualifiedQty = Number(form.get("qualifiedQty")) || 0;
    const defectQty = Number(form.get("defectQty")) || 0;
    if (qualifiedQty + defectQty > reportQty) {
        throw new Error("合格数量与不良数量之和不能超过报工数量");
    }
    return {
        workOrderId: Number(form.get("workOrderId")),
        batchNo: String(form.get("batchNo") || ""),
        reportQty,
        qualifiedQty,
        defectQty,
        workHours: Number(form.get("workHours")) || 0,
        remark: String(form.get("remark") || "").trim()
    };
}

function productTaskLabel(order) {
    const product = productionCache.products.find(item => Number(item.productId) === Number(order.productId));
    const task = productionCache.tasks.find(item => Number(item.taskId) === Number(order.taskId));
    const productText = product
        ? [product.productCode, product.productModel || product.productName].filter(Boolean).join(" / ")
        : (order.productId ? `产品${order.productId}` : "产品-");
    const taskText = task?.taskNo || (order.taskId ? `任务${order.taskId}` : "任务-");
    return `${productText} / ${taskText}`;
}

function lineProcessLabel(order) {
    const line = productionCache.lines.find(item => Number(item.lineId) === Number(order.lineId));
    const route = productionCache.routes.find(item => Number(item.processId) === Number(order.processId));
    const lineText = line
        ? [line.lineCode, line.lineName].filter(Boolean).join(" / ")
        : (order.lineId ? `产线${order.lineId}` : "产线-");
    const processText = route?.processName || route?.routeName || (order.processId ? `工序${order.processId}` : "工序-");
    return `${lineText} / ${processText}`;
}

function reportedQtyForWorkOrder(workOrderId, reports = productionCache.reports) {
    return reports
        .filter(report => Number(report.workOrderId) === Number(workOrderId) && report.reportStatus !== "REJECTED")
        .reduce((sum, report) => sum + (Number(report.reportQty) || 0), 0);
}

function workOrderNoForReport(report) {
    const order = productionCache.workOrders.find(item => Number(item.workOrderId) === Number(report.workOrderId));
    return order?.workOrderNo || `WO-${report.workOrderId}`;
}

function reportForWage(wage) {
    return productionCache.reports.find(report => Number(report.reportId) === Number(wage.reportId));
}

function workOrderNoForWage(wage) {
    const report = reportForWage(wage);
    return report ? workOrderNoForReport(report) : "-";
}

function reportNoForWage(wage) {
    const report = reportForWage(wage);
    return report?.reportNo || `WR-${wage.reportId}`;
}

function reportStatusText(status) {
    return {
        SUBMITTED: "待审核",
        APPROVED: "已审核",
        REJECTED: "已驳回"
    }[status] || statusText(status);
}

function workOrderStatusForProduction(status) {
    if (status === "RUNNING" || status === "IN_PROGRESS") return "生产中";
    return statusText(status);
}

function detailField(label, value) {
    return `<div class="detail-row"><span>${escapeHtml(label)}</span><strong>${escapeHtml(value ?? "-")}</strong></div>`;
}

function sumBy(rows, key) {
    return rows.reduce((sum, row) => sum + (Number(row[key]) || 0), 0);
}

function todayDateKey() {
    const value = new Date();
    const month = String(value.getMonth() + 1).padStart(2, "0");
    const day = String(value.getDate()).padStart(2, "0");
    return `${value.getFullYear()}-${month}-${day}`;
}

function reportDateKey(value) {
    return value ? String(value).slice(0, 10) : "";
}

function formatProductionDateTime(value) {
    if (!value) return "-";
    return String(value).replace("T", " ").slice(0, 19);
}

function scrollProductionSection(id) {
    const target = document.getElementById(id);
    const panel = document.getElementById("production");
    if (!target || !panel) return;
    const workspaceView = target.closest("[data-workspace-view]");
    if (workspaceView?.dataset.workspaceView && typeof selectModuleView === "function") {
        selectModuleView(panel, workspaceView.dataset.workspaceView);
    }
    window.setTimeout(() => target.closest(".tool")?.scrollIntoView({ behavior: "smooth", block: "center" }), 80);
}

function applyProductionRolePresentation() {
    const isWorkshopManager = hasRole("WORKSHOP_MANAGER");
    document.getElementById("reportableWorkOrderTool")?.classList.toggle("permission-hidden", isWorkshopManager || !canCreateWorkReport());
    setProductionToolTitle("reportTable", isWorkshopManager ? "报工审核" : "我的报工单");
    setProductionToolTitle("wageTable", isWorkshopManager ? "计件工资汇总" : "计件工资");
    if (typeof updateModuleWorkspace === "function") updateModuleWorkspace(document.getElementById("production"));
}

function setProductionToolTitle(tableId, title) {
    const tool = document.getElementById(tableId)?.closest(".tool");
    const heading = tool?.querySelector(":scope > h3");
    if (heading) heading.textContent = title;
    const panel = tool?.closest(".panel");
    const view = tool?.closest("[data-workspace-view]");
    const target = view?.dataset.workspaceView;
    const tabLabel = target ? panel?.querySelector(`[data-workspace-target="${target}"] span`) : null;
    if (tabLabel) tabLabel.textContent = title;
}

function relabelProductionStaticText() {
    const panel = document.getElementById("production");
    if (!panel) return;
    panel.querySelector("h2").textContent = "生产报工";
    document.getElementById("refreshProduction").textContent = "刷新";
}

relabelProductionStaticText();
