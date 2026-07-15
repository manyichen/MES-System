let executiveDashboardState = null;
let executiveAnimationFrame = 0;
let executiveRefreshTimer = 0;

async function loadExecutiveDashboard(options = {}) {
    if (!hasRole("GENERAL_MANAGER")) return;
    try {
        executiveDashboardState = await getJson("/dashboard/executive");
    } catch (primaryError) {
        try {
            executiveDashboardState = await loadLegacyExecutiveDashboard();
        } catch (fallbackError) {
            showMessage(toChineseError(primaryError), "error");
            return;
        }
    }
    try {
        renderExecutiveDashboard();
        if (options.notify) showMessage("\u7ecf\u8425\u9a7e\u9a76\u8231\u6570\u636e\u5df2\u5237\u65b0", "ok");
    } catch (error) { showMessage(toChineseError(error), "error"); }
}

async function loadLegacyExecutiveDashboard() {
    const [orders, workOrders, reports, lines, equipment, inventory, shortages, reworks, repairs] = await Promise.all([
        getJson("/orders").catch(() => []),
        getJson("/work-orders").catch(() => []),
        getJson("/work-reports").catch(() => []),
        getJson("/production-lines").catch(() => []),
        getJson("/equipment").catch(() => []),
        getJson("/inventory").catch(() => []),
        getJson("/shortage-alerts").catch(() => []),
        getJson("/rework-orders").catch(() => []),
        getJson("/equipment-repair-reports").catch(() => [])
    ]);
    const orderQty = sumExecutive(orders, "orderQty");
    const reportedQty = sumExecutive(reports, "reportQty");
    const qualifiedQty = sumExecutive(reports, "qualifiedQty");
    const defectQty = sumExecutive(reports, "defectQty");
    const activeWorkOrders = workOrders.filter(row => !["COMPLETED", "CLOSED", "CANCELLED"].includes(row.workOrderStatus)).length;
    const completedWorkOrders = workOrders.filter(row => ["COMPLETED", "CLOSED"].includes(row.workOrderStatus)).length;
    const activeEquipment = equipment.filter(row => Number(row.enabled) !== 0);
    const faultEquipment = activeEquipment.filter(row => ["FAULT", "REPAIRING", "DOWN"].includes(row.equipmentStatus));
    const availableInventory = inventory.filter(row => Number(row.availableQty) > 0).length;
    const openShortages = shortages.filter(row => ["OPEN", "ACCEPTED"].includes(row.alertStatus));
    const openReworks = reworks.filter(row => !["FINISHED", "CLOSED"].includes(row.reworkStatus));
    const openRepairs = repairs.filter(row => ["REPORTED", "APPROVED"].includes(row.repairStatus));
    const riskCount = openShortages.length + openReworks.length + faultEquipment.length + openRepairs.length;
    const metrics = [
        legacyMetric("order-volume", "\u8ba2\u5355\u8ba1\u5212\u91cf", orderQty, "\u6761", "blue", `\u5f53\u524d\u7d2f\u8ba1 ${orders.length} \u7b14\u5ba2\u6237\u8ba2\u5355`),
        legacyMetric("qualified-output", "\u5408\u683c\u4ea7\u51fa", qualifiedQty, "\u6761", "cyan", `\u7d2f\u8ba1\u62a5\u5de5 ${reportedQty} \u6761`),
        { key: "quality-rate", label: "\u5408\u683c\u7387", value: executivePercent(qualifiedQty, reportedQty), unit: "%", tone: "green", detail: `\u4e0d\u5408\u683c\u54c1 ${defectQty} \u6761` },
        { key: "equipment-availability", label: "\u8bbe\u5907\u53ef\u7528\u7387", value: executivePercent(activeEquipment.length - faultEquipment.length, activeEquipment.length), unit: "%", tone: "blue", detail: `${faultEquipment.length} \u53f0\u8bbe\u5907\u9700\u5173\u6ce8` },
        legacyMetric("active-work-orders", "\u5728\u5236\u5de5\u5355", activeWorkOrders, "\u5f20", "cyan", `\u5df2\u5b8c\u6210 ${completedWorkOrders} \u5f20`),
        legacyMetric("management-risks", "\u7ecf\u8425\u98ce\u9669\u4e8b\u9879", riskCount, "\u9879", riskCount ? "amber" : "green", "\u7f3a\u6599\u3001\u8fd4\u5de5\u3001\u8bbe\u5907\u4e0e\u7ef4\u4fee\u5f85\u95ed\u73af")
    ];
    const productionLines = lines.filter(row => Number(row.enabled) !== 0).map(line => {
        const lineOrders = workOrders.filter(row => Number(row.lineId) === Number(line.lineId));
        const lineEquipment = activeEquipment.filter(row => Number(row.lineId) === Number(line.lineId));
        return {
            lineId: line.lineId, lineCode: line.lineCode, lineName: line.lineName, lineStatus: line.lineStatus,
            dailyCapacity: line.capacityPerDay || line.dailyCapacity || 0,
            activeWorkOrders: lineOrders.filter(row => !["COMPLETED", "CLOSED", "CANCELLED"].includes(row.workOrderStatus)).length,
            plannedQty: sumExecutive(lineOrders, "plannedQty"), actualQty: sumExecutive(lineOrders, "actualQty"),
            equipmentTotal: lineEquipment.length,
            equipmentFaults: lineEquipment.filter(row => ["FAULT", "REPAIRING", "DOWN"].includes(row.equipmentStatus)).length
        };
    });
    const alerts = [
        ...openShortages.map(row => ({ domain: "MATERIAL", title: row.materialName || row.alertNo, detail: row.alertContent, severity: row.severity, occurredAt: row.createdAt })),
        ...openReworks.map(row => ({ domain: "QUALITY", title: row.reworkOrderNo, detail: row.reworkReason, severity: "HIGH", occurredAt: row.createdAt })),
        ...openRepairs.map(row => ({ domain: "EQUIPMENT", title: row.repairReportNo, detail: row.faultDesc, severity: row.faultLevel, occurredAt: row.reportTime }))
    ].sort((left, right) => Date.parse(right.occurredAt || 0) - Date.parse(left.occurredAt || 0)).slice(0, 6);
    const period = `${new Date().getFullYear()} \u5e74\u5ea6 \u00b7 \u622a\u81f3\u5f53\u524d`;
    const departmentReports = [
        legacyReport("\u4ed3\u50a8\u7269\u6d41", "\u4ed3\u5e93\u7ba1\u7406\u5458", period, "\u53ef\u7528\u5e93\u5b58\u6279\u6b21", availableInventory, "\u6279", openShortages.length, openShortages.length ? "\u5b58\u5728\u5f85\u95ed\u73af\u7684\u7269\u6599\u9884\u8b66" : "\u6682\u65e0\u5f85\u534f\u540c\u7684\u7269\u6599\u7f3a\u53e3"),
        legacyReport("\u8f66\u95f4\u751f\u4ea7", "\u8f66\u95f4\u7ba1\u7406\u5458", period, "\u5de5\u5355\u5b8c\u6210\u7387", executivePercent(completedWorkOrders, workOrders.length), "%", activeWorkOrders, `${activeWorkOrders} \u5f20\u5de5\u5355\u5904\u4e8e\u6267\u884c\u4e2d`),
        legacyReport("\u8d28\u91cf\u7ba1\u63a7", "\u8d28\u91cf\u4e3b\u7ba1", period, "\u62a5\u5de5\u5408\u683c\u7387", executivePercent(qualifiedQty, reportedQty), "%", openReworks.length, openReworks.length ? "\u8fd4\u5de5\u8ba2\u5355\u9700\u6301\u7eed\u7763\u529e" : "\u672a\u53d1\u73b0\u672a\u95ed\u73af\u8fd4\u5de5"),
        legacyReport("\u8bbe\u5907\u4fdd\u969c", "\u8bbe\u5907\u4e3b\u7ba1", period, "\u8bbe\u5907\u53ef\u7528\u7387", executivePercent(activeEquipment.length - faultEquipment.length, activeEquipment.length), "%", faultEquipment.length + openRepairs.length, faultEquipment.length ? "\u6545\u969c\u4e0e\u7ef4\u4fee\u5de5\u5355\u5df2\u7eb3\u5165\u7763\u529e" : "\u672a\u53d1\u73b0\u505c\u673a\u8bbe\u5907")
    ];
    return {
        generatedAt: new Date().toISOString(), metrics, productionLines, alerts,
        productionTrend: buildLegacyTrend(reports), departmentReports,
        auditFindings: [
            legacyFinding("\u4ed3\u50a8\u7269\u6d41", "\u7269\u6599\u4fdd\u969c", openShortages.length, `\u5f85\u95ed\u73af\u7f3a\u6599\u9884\u8b66 ${openShortages.length} \u9879`, "\u5173\u6ce8\u5907\u6599\u4e0e\u9884\u8b66\u63a5\u6536\u8fdb\u5ea6"),
            legacyFinding("\u8f66\u95f4\u751f\u4ea7", "\u5de5\u5355\u6267\u884c", activeWorkOrders, `\u5728\u5236\u5de5\u5355 ${activeWorkOrders} \u5f20`, "\u6838\u5bf9\u5de5\u5355\u8fdb\u5ea6\u4e0e\u62a5\u5de5\u7ed3\u679c"),
            legacyFinding("\u8d28\u91cf\u7ba1\u63a7", "\u8fd4\u5de5\u95ed\u73af", openReworks.length, `\u672a\u95ed\u73af\u8fd4\u5de5\u5355 ${openReworks.length} \u5f20`, "\u590d\u6838\u8d23\u4efb\u5f52\u5c5e\u3001\u5904\u7f6e\u65f6\u9650\u4e0e\u9a8c\u8bc1\u8bc1\u636e"),
            legacyFinding("\u8bbe\u5907\u4fdd\u969c", "\u8bbe\u5907\u7a33\u5b9a\u6027", faultEquipment.length + openRepairs.length, `\u6545\u969c\u8bbe\u5907 ${faultEquipment.length} \u53f0\uff0c\u5f85\u5904\u7406\u62a5\u4fee ${openRepairs.length} \u9879`, "\u5ba1\u89c6\u6545\u969c\u54cd\u5e94\u4e0e\u7ef4\u4fee\u5b8c\u7ed3\u60c5\u51b5")
        ]
    };
}

function sumExecutive(rows, key) {
    return rows.reduce((total, row) => total + (Number(row?.[key]) || 0), 0);
}

function executivePercent(numerator, denominator) {
    if (!denominator) return "0.0";
    return (Number(numerator || 0) * 100 / Number(denominator)).toFixed(1);
}

function legacyMetric(key, label, value, unit, tone, detail) {
    return { key, label, value: String(value || 0), unit, tone, detail };
}

function legacyReport(department, ownerRole, period, metricLabel, metricValue, unit, riskCount, summary) {
    return { department, ownerRole, period, metricLabel, metricValue: String(metricValue || 0), unit, riskCount, summary };
}

function legacyFinding(department, title, riskCount, detail, nextStep) {
    return { department, title, detail, nextStep, severity: riskCount === 0 ? "NORMAL" : riskCount >= 3 ? "HIGH" : "MEDIUM" };
}

function buildLegacyTrend(reports) {
    const days = Array.from({ length: 7 }, (_, index) => {
        const date = new Date();
        date.setHours(0, 0, 0, 0);
        date.setDate(date.getDate() - (6 - index));
        return date;
    });
    return days.map(date => {
        const key = date.toISOString().slice(0, 10);
        const rows = reports.filter(report => String(report.reportTime || "").slice(0, 10) === key);
        return {
            day: new Intl.DateTimeFormat("zh-CN", { month: "2-digit", day: "2-digit" }).format(date),
            reportedQty: sumExecutive(rows, "reportQty"),
            qualifiedQty: sumExecutive(rows, "qualifiedQty"),
            defectQty: sumExecutive(rows, "defectQty")
        };
    });
}

function renderExecutiveDashboard() {
    const state = executiveDashboardState;
    if (!state) return;
    document.getElementById("executive-generated-at").textContent = formatExecutiveTime(state.generatedAt);
    document.getElementById("production-live-clock").textContent = formatExecutiveTime(state.generatedAt);
    renderExecutiveMetrics(state.metrics || []);
    renderExecutiveAlerts(state.alerts || []);
    renderProductionLines(state.productionLines || []);
    renderDepartmentReports(state.departmentReports || []);
    renderAuditFindings(state.auditFindings || []);
    document.getElementById("executive-factory-caption").textContent = `${(state.productionLines || []).length} \u6761\u4ea7\u7ebf\u5df2\u7eb3\u5165\u76d1\u6d4b`;
    document.getElementById("production-line-count").textContent = `${(state.productionLines || []).length} \u6761\u4ea7\u7ebf`;
    document.getElementById("department-report-period").textContent = state.departmentReports?.[0]?.period || "--";
    document.getElementById("management-audit-state").textContent = "\u6570\u636e\u5df2\u6821\u9a8c";
    drawExecutiveCanvases(performance.now());
    startExecutiveAnimation();
}

function renderExecutiveMetrics(metrics) {
    const container = document.getElementById("executive-kpi-grid");
    if (!container) return;
    container.innerHTML = metrics.map((metric, index) => `
        <article class="executive-kpi executive-tone-${escapeHtml(metric.tone || "blue")}" style="--metric-delay:${index * 55}ms">
            <div><span>${escapeHtml(metric.label || "-")}</span><strong>${escapeHtml(metric.value || "0")}<small>${escapeHtml(metric.unit || "")}</small></strong></div>
            <p>${escapeHtml(metric.detail || "")}</p>
            <i aria-hidden="true"></i>
        </article>`).join("") || executiveEmpty("\u6682\u65e0\u7ecf\u8425\u6307\u6807");
}

function renderExecutiveAlerts(alerts) {
    const container = document.getElementById("executive-alert-list");
    const counter = document.getElementById("executive-alert-count");
    if (counter) counter.textContent = `${alerts.length} \u9879`;
    if (!container) return;
    if (!alerts.length) {
        container.innerHTML = `<div class="executive-calm"><i></i><strong>\u6682\u65e0\u5f85\u7763\u529e\u7ecf\u8425\u98ce\u9669</strong><span>\u5f53\u524d\u7cfb\u7edf\u6570\u636e\u672a\u68c0\u51fa\u7f3a\u6599\u3001\u8fd4\u5de5\u6216\u5f85\u5904\u7406\u62a5\u4fee</span></div>`;
        return;
    }
    container.innerHTML = alerts.map(alert => `
        <article class="executive-alert severity-${escapeHtml(executiveSeverity(alert.severity))}">
            <i></i><div><span>${escapeHtml(executiveDomainText(alert.domain))}</span><strong>${escapeHtml(alert.title || "\u5f85\u5173\u6ce8\u4e8b\u9879")}</strong><p>${escapeHtml(alert.detail || "\u8bf7\u67e5\u9605\u4e1a\u52a1\u660e\u7ec6")}</p></div><time>${escapeHtml(formatExecutiveTime(alert.occurredAt))}</time>
        </article>`).join("");
}

function renderProductionLines(lines) {
    const container = document.getElementById("production-line-list");
    const key = document.getElementById("production-live-key");
    if (key) key.innerHTML = `<span><i class="line-key-running"></i>\u8fd0\u884c/\u53ef\u8c03\u5ea6</span><span><i class="line-key-idle"></i>\u7a7a\u95f2</span><span><i class="line-key-fault"></i>\u5f85\u5173\u6ce8</span>`;
    if (!container) return;
    if (!lines.length) {
        container.innerHTML = executiveEmpty("\u6682\u65e0\u542f\u7528\u4ea7\u7ebf\u6570\u636e");
        return;
    }
    container.innerHTML = lines.map(line => {
        const total = Math.max(Number(line.plannedQty || 0), Number(line.dailyCapacity || 0), 1);
        const progress = Math.min(100, Math.round(Number(line.actualQty || 0) * 100 / total));
        const health = line.equipmentFaults > 0 ? "fault" : executiveLineTone(line.lineStatus);
        return `<article class="production-line-row line-${escapeHtml(health)}">
            <div class="production-line-title"><span>${escapeHtml(line.lineCode || "\u4ea7\u7ebf")}</span><strong>${escapeHtml(line.lineName || "\u672a\u547d\u540d\u4ea7\u7ebf")}</strong><em>${escapeHtml(executiveLineStatusText(line.lineStatus))}</em></div>
            <div class="production-line-metrics"><span>\u5728\u5236 <strong>${escapeHtml(line.activeWorkOrders || 0)}</strong> \u5f20</span><span>\u8bbe\u5907 <strong>${escapeHtml(line.equipmentTotal || 0)}</strong> \u53f0</span><span>\u5f02\u5e38 <strong>${escapeHtml(line.equipmentFaults || 0)}</strong> \u53f0</span></div>
            <div class="production-line-progress"><i style="width:${progress}%"></i></div>
            <small>\u8ba1\u5212 ${escapeHtml(line.plannedQty || 0)} / \u5b9e\u9645 ${escapeHtml(line.actualQty || 0)} \u6761</small>
        </article>`;
    }).join("");
}

function renderDepartmentReports(reports) {
    const container = document.getElementById("department-report-list");
    if (!container) return;
    container.innerHTML = reports.map((report, index) => `
        <article class="department-report-card" style="--report-delay:${index * 70}ms">
            <div class="department-report-heading"><span>${escapeHtml(report.department)}</span><small>${escapeHtml(report.ownerRole)}</small></div>
            <strong>${escapeHtml(report.metricValue || "0")}<em>${escapeHtml(report.unit || "")}</em></strong>
            <p class="department-report-label">${escapeHtml(report.metricLabel || "\u5e74\u5ea6\u6307\u6807")}</p>
            <p class="department-report-summary">${escapeHtml(report.summary || "\u6682\u65e0\u6570\u636e\u6458\u8981")}</p>
            <footer><span>${escapeHtml(report.period || "\u5e74\u5ea6\u7d2f\u8ba1")}</span><b class="report-risk-${report.riskCount > 0 ? "open" : "calm"}">${escapeHtml(report.riskCount || 0)} \u9879\u9700\u5173\u6ce8</b></footer>
        </article>`).join("") || executiveEmpty("\u6682\u65e0\u4e3b\u7ba1\u5e74\u5ea6\u62a5\u544a\u6570\u636e");
}

function renderAuditFindings(findings) {
    const container = document.getElementById("management-audit-list");
    if (!container) return;
    container.innerHTML = findings.map((finding, index) => `
        <article class="management-audit-item audit-${escapeHtml(executiveSeverity(finding.severity))}" style="--audit-delay:${index * 70}ms">
            <div class="management-audit-index">0${index + 1}</div>
            <div><span>${escapeHtml(finding.department)}</span><h3>${escapeHtml(finding.title)}</h3><p>${escapeHtml(finding.detail)}</p></div>
            <div class="management-audit-next"><span>\u7763\u529e\u5efa\u8bae</span><p>${escapeHtml(finding.nextStep)}</p></div>
            <b>${escapeHtml(executiveSeverityText(finding.severity))}</b>
        </article>`).join("") || executiveEmpty("\u6682\u65e0\u5ba1\u8ba1\u8981\u70b9");
}

function drawExecutiveCanvases(now) {
    if (!executiveDashboardState) return;
    if (document.getElementById("executiveOverview")?.classList.contains("active")) {
        drawExecutiveTrend(now, executiveDashboardState.productionTrend || []);
        drawExecutiveFactory(now, executiveDashboardState.productionLines || []);
    }
    if (document.getElementById("productionLive")?.classList.contains("active")) {
        drawProductionLive(now, executiveDashboardState.productionLines || []);
    }
}

function drawExecutiveTrend(now, points) {
    const canvas = document.getElementById("executive-trend-canvas");
    const context = prepareExecutiveCanvas(canvas);
    if (!context) return;
    const width = context._executiveWidth;
    const height = context._executiveHeight;
    const padding = { top: 24, right: 20, bottom: 28, left: 34 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom;
    const allValues = points.flatMap(point => [Number(point.reportedQty), Number(point.qualifiedQty), Number(point.defectQty)]);
    const max = Math.max(10, ...allValues);
    context.clearRect(0, 0, width, height);
    context.fillStyle = "#071525";
    context.fillRect(0, 0, width, height);
    context.strokeStyle = "rgba(97, 180, 255, .14)";
    context.lineWidth = 1;
    for (let row = 0; row < 5; row += 1) {
        const y = padding.top + chartHeight * row / 4;
        context.beginPath(); context.moveTo(padding.left, y); context.lineTo(width - padding.right, y); context.stroke();
        context.fillStyle = "rgba(152, 186, 220, .65)";
        context.font = "10px Inter, sans-serif";
        context.fillText(String(Math.round(max * (4 - row) / 4)), 3, y + 3);
    }
    if (!points.length) return;
    const sets = [
        { key: "reportedQty", color: "#40b6ff", shade: "rgba(64,182,255,.13)" },
        { key: "qualifiedQty", color: "#39e2ad", shade: "rgba(57,226,173,.1)" },
        { key: "defectQty", color: "#ff9e55", shade: "rgba(255,158,85,.08)" }
    ];
    sets.forEach((set, setIndex) => {
        const progress = Math.min(1, (now % 2400) / 1100);
        context.beginPath();
        points.forEach((point, index) => {
            const x = padding.left + chartWidth * index / Math.max(1, points.length - 1);
            const y = padding.top + chartHeight * (1 - Math.min(max, Number(point[set.key] || 0)) / max);
            const animatedX = padding.left + (x - padding.left) * progress;
            if (index === 0) context.moveTo(animatedX, y); else context.lineTo(animatedX, y);
        });
        context.strokeStyle = set.color;
        context.lineWidth = setIndex === 2 ? 1.6 : 2.2;
        context.shadowColor = set.color;
        context.shadowBlur = 8;
        context.stroke();
        context.shadowBlur = 0;
    });
    points.forEach((point, index) => {
        const x = padding.left + chartWidth * index / Math.max(1, points.length - 1);
        context.fillStyle = "rgba(152, 186, 220, .7)";
        context.font = "10px Inter, sans-serif";
        context.textAlign = "center";
        context.fillText(point.day || "--", x, height - 8);
    });
    context.textAlign = "start";
    const legend = document.getElementById("executive-trend-legend");
    if (legend && !legend.dataset.ready) {
        legend.innerHTML = `<span><i class="legend-report"></i>\u62a5\u5de5\u4ea7\u51fa</span><span><i class="legend-qualified"></i>\u5408\u683c\u4ea7\u51fa</span><span><i class="legend-defect"></i>\u4e0d\u5408\u683c</span>`;
        legend.dataset.ready = "true";
    }
}

function drawExecutiveFactory(now, lines) {
    const canvas = document.getElementById("executive-factory-canvas");
    const context = prepareExecutiveCanvas(canvas);
    if (!context) return;
    const width = context._executiveWidth;
    const height = context._executiveHeight;
    context.clearRect(0, 0, width, height);
    const glow = context.createRadialGradient(width * .52, height * .42, 8, width * .52, height * .42, width * .55);
    glow.addColorStop(0, "rgba(29, 170, 255, .2)"); glow.addColorStop(1, "rgba(4, 12, 27, 0)");
    context.fillStyle = "#061426"; context.fillRect(0, 0, width, height); context.fillStyle = glow; context.fillRect(0, 0, width, height);
    const stations = ["\u539f\u6599\u4fdd\u969c", "\u5bc6\u70bc", "\u6210\u578b", "\u786b\u5316", "\u8d28\u91cf", "\u5165\u5e93"];
    const step = width / (stations.length + 1);
    const pathY = height * .55;
    context.lineWidth = 3;
    const road = context.createLinearGradient(0, pathY, width, pathY);
    road.addColorStop(0, "#0b60b7"); road.addColorStop(.5, "#46e1ff"); road.addColorStop(1, "#0b60b7");
    context.strokeStyle = road; context.shadowColor = "#43dffb"; context.shadowBlur = 13;
    context.beginPath(); context.moveTo(step * .7, pathY); context.bezierCurveTo(width * .35, pathY - height * .18, width * .63, pathY + height * .16, width - step * .7, pathY); context.stroke(); context.shadowBlur = 0;
    stations.forEach((station, index) => {
        const x = step * (index + 1);
        const y = pathY + Math.sin(index * 1.8) * 18;
        const line = lines[index % Math.max(1, lines.length)];
        const fault = Number(line?.equipmentFaults || 0) > 0;
        drawIsometricStation(context, x, y, fault ? "#ff805f" : "#38d8cb", station, line?.activeWorkOrders || 0);
    });
    const pulse = (now / 2100) % 1;
    for (let token = 0; token < 4; token += 1) {
        const t = (pulse + token / 4) % 1;
        const x = step * .7 + (width - step * 1.4) * t;
        const y = pathY + Math.sin(t * Math.PI * 2) * height * .07;
        context.fillStyle = "#e5faff"; context.shadowColor = "#42dffb"; context.shadowBlur = 14;
        context.beginPath(); context.arc(x, y, 3.8, 0, Math.PI * 2); context.fill(); context.shadowBlur = 0;
    }
}

function drawProductionLive(now, lines) {
    const canvas = document.getElementById("production-live-canvas");
    const context = prepareExecutiveCanvas(canvas);
    if (!context) return;
    const width = context._executiveWidth;
    const height = context._executiveHeight;
    context.clearRect(0, 0, width, height);
    context.fillStyle = "#040f1d"; context.fillRect(0, 0, width, height);
    const grid = 28;
    context.strokeStyle = "rgba(48, 142, 211, .1)";
    for (let x = -height; x < width + height; x += grid) { context.beginPath(); context.moveTo(x, height); context.lineTo(x + height, 0); context.stroke(); }
    for (let x = 0; x < width + height; x += grid) { context.beginPath(); context.moveTo(x, 0); context.lineTo(x - height, height); context.stroke(); }
    const available = lines.length ? lines : [{ lineCode: "--", lineName: "\u6682\u65e0\u4ea7\u7ebf", lineStatus: "IDLE", activeWorkOrders: 0, equipmentFaults: 0 }];
    const columns = Math.min(3, available.length);
    const cellWidth = width / columns;
    available.forEach((line, index) => {
        const column = index % columns;
        const row = Math.floor(index / columns);
        const x = cellWidth * (column + .5);
        const y = height * (.32 + row * .38);
        const fault = Number(line.equipmentFaults || 0) > 0;
        drawLiveLine(context, x, y, Math.min(cellWidth * .72, 250), fault ? "#ff7358" : executiveLineColor(line.lineStatus), line, now + index * 300);
    });
}

function drawIsometricStation(context, x, y, color, label, activeWorkOrders) {
    const width = 44;
    const height = 26;
    context.fillStyle = "rgba(8, 37, 65, .95)";
    context.beginPath(); context.moveTo(x, y - height); context.lineTo(x + width, y - height / 2); context.lineTo(x, y); context.lineTo(x - width, y - height / 2); context.closePath(); context.fill();
    context.strokeStyle = color; context.lineWidth = 1.2; context.shadowColor = color; context.shadowBlur = 10; context.stroke(); context.shadowBlur = 0;
    context.fillStyle = "rgba(7, 23, 40, .9)"; context.fillRect(x - 35, y + 12, 70, 19);
    context.fillStyle = "#c5eaff"; context.font = "10px Inter, sans-serif"; context.textAlign = "center"; context.fillText(label, x, y + 25);
    context.fillStyle = color; context.font = "9px Inter, sans-serif"; context.fillText(`${activeWorkOrders} \u5f20\u5728\u5236`, x, y + 41); context.textAlign = "start";
}

function drawLiveLine(context, x, y, width, color, line, now) {
    const height = width * .24;
    context.save(); context.translate(x, y);
    context.fillStyle = "rgba(8, 29, 50, .94)";
    context.beginPath(); context.moveTo(-width / 2, 0); context.lineTo(0, -height / 2); context.lineTo(width / 2, 0); context.lineTo(0, height / 2); context.closePath(); context.fill();
    context.strokeStyle = color; context.lineWidth = 1.4; context.shadowColor = color; context.shadowBlur = 12; context.stroke(); context.shadowBlur = 0;
    context.strokeStyle = "rgba(163, 224, 255, .6)"; context.beginPath(); context.moveTo(-width * .34, 0); context.lineTo(width * .34, 0); context.stroke();
    for (let i = 0; i < 3; i += 1) {
        const movement = ((now / 1300 + i / 3) % 1) * width * .6 - width * .3;
        context.fillStyle = "#e8fbff"; context.shadowColor = color; context.shadowBlur = 8; context.beginPath(); context.arc(movement, 0, 3, 0, Math.PI * 2); context.fill(); context.shadowBlur = 0;
    }
    context.fillStyle = "rgba(5, 17, 31, .92)"; context.fillRect(-width * .38, height * .62, width * .76, 41);
    context.fillStyle = "#d8efff"; context.font = "bold 11px Inter, sans-serif"; context.textAlign = "center"; context.fillText(line.lineCode || "\u4ea7\u7ebf", 0, height * .62 + 15);
    context.fillStyle = "#80a7c6"; context.font = "10px Inter, sans-serif"; context.fillText(`${executiveLineStatusText(line.lineStatus)}  |  ${line.activeWorkOrders || 0} \u5f20\u5728\u5236`, 0, height * .62 + 30); context.restore();
}

function prepareExecutiveCanvas(canvas) {
    if (!canvas || canvas.clientWidth < 10 || canvas.clientHeight < 10) return null;
    const ratio = Math.min(window.devicePixelRatio || 1, 2);
    const width = Math.round(canvas.clientWidth);
    const height = Math.round(canvas.clientHeight);
    if (canvas.width !== width * ratio || canvas.height !== height * ratio) {
        canvas.width = width * ratio;
        canvas.height = height * ratio;
    }
    const context = canvas.getContext("2d");
    context.setTransform(ratio, 0, 0, ratio, 0, 0);
    context._executiveWidth = width;
    context._executiveHeight = height;
    return context;
}

function startExecutiveAnimation() {
    if (executiveAnimationFrame || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const animate = now => {
        executiveAnimationFrame = requestAnimationFrame(animate);
        drawExecutiveCanvases(now);
    };
    executiveAnimationFrame = requestAnimationFrame(animate);
}

function bindExecutiveEvents() {
    ["refresh-executive-overview", "refresh-production-live", "refresh-department-reports", "refresh-management-audit"].forEach(id => {
        document.getElementById(id)?.addEventListener("click", () => loadExecutiveDashboard({ notify: true }));
    });
    window.addEventListener("resize", () => drawExecutiveCanvases(performance.now()));
    if (!executiveRefreshTimer) {
        executiveRefreshTimer = window.setInterval(() => {
            if (hasRole("GENERAL_MANAGER") && document.visibilityState === "visible") loadExecutiveDashboard();
        }, 20000);
    }
}

function executiveEmpty(message) {
    return `<div class="executive-empty">${escapeHtml(message)}</div>`;
}

function executiveSeverity(value) {
    const status = String(value || "").toUpperCase();
    if (["CRITICAL", "HIGH", "URGENT", "FAULT"].includes(status)) return "high";
    if (["MEDIUM", "GENERAL", "WARNING"].includes(status)) return "medium";
    return "normal";
}

function executiveSeverityText(value) {
    return { high: "\u91cd\u70b9\u7763\u529e", medium: "\u6301\u7eed\u5173\u6ce8", normal: "\u72b6\u6001\u7a33\u5b9a" }[executiveSeverity(value)];
}

function executiveDomainText(domain) {
    return { MATERIAL: "\u7269\u6599\u4fdd\u969c", QUALITY: "\u8d28\u91cf\u98ce\u9669", EQUIPMENT: "\u8bbe\u5907\u4fdd\u969c" }[String(domain || "").toUpperCase()] || "\u7ecf\u8425\u98ce\u9669";
}

function executiveLineTone(status) {
    const key = String(status || "").toUpperCase();
    if (["FAULT", "REPAIRING", "DOWN", "DISABLED"].includes(key)) return "fault";
    if (["IDLE", "PAUSED"].includes(key)) return "idle";
    return "running";
}

function executiveLineColor(status) {
    return { fault: "#ff7358", idle: "#ffbd5a", running: "#45e2bc" }[executiveLineTone(status)];
}

function executiveLineStatusText(status) {
    return { RUNNING: "\u8fd0\u884c\u4e2d", IDLE: "\u7a7a\u95f2", FAULT: "\u6545\u969c", REPAIRING: "\u7ef4\u4fee\u4e2d", DOWN: "\u505c\u673a", DISABLED: "\u5df2\u505c\u7528" }[String(status || "").toUpperCase()] || "\u53ef\u8c03\u5ea6";
}

function formatExecutiveTime(value) {
    if (!value) return "--:--:--";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value);
    return new Intl.DateTimeFormat("zh-CN", { hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false }).format(date);
}
