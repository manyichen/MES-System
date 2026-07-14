const params = new URLSearchParams(location.search);
const token = params.get("token") || "";

loadPublicTrace();

async function loadPublicTrace() {
    const status = document.getElementById("public-status");
    const hint = document.getElementById("public-hint");
    const content = document.getElementById("public-content");
    if (!token) return fail("二维码内容不完整", "缺少轮胎追溯令牌");
    try {
        const response = await fetch(`./api/public/tire-traces/${encodeURIComponent(token)}`);
        const payload = await response.json();
        if (!response.ok || payload.success === false) throw new Error(payload.message || "轮胎追溯信息不存在");
        const tire = payload.data;
        status.textContent = tire.tireStatus === "IN_STOCK" ? "正品信息已核验" : statusText(tire.tireStatus);
        hint.textContent = "该二维码已成功关联生产、质检和入库记录";
        content.classList.remove("loading");
        content.innerHTML = `
            <div class="serial-block"><span>轮胎唯一序列号</span><strong>${escapeHtml(tire.serialNo)}</strong><small>${escapeHtml(tire.traceCode)}</small></div>
            <div class="product-title"><span>产品信息</span><h2>${escapeHtml(tire.productName || "轮胎产品")}</h2><p>${escapeHtml(tire.productModel || "规格未填写")}</p></div>
            <dl>
                ${row("产品编码", tire.productCode)}${row("生产工单", tire.workOrderNo)}${row("生产批次", tire.batchNo)}
                ${row("生产产线", tire.productionLine)}${row("质检单", tire.inspectionNo)}${row("质检结论", tire.inspectionResult)}
                ${row("合格时间", formatTime(tire.qualifiedAt))}${row("入库仓库", tire.warehouseName)}
                ${row("入库库位", tire.locationName)}${row("入库时间", formatTime(tire.inboundAt))}
            </dl>
            <a class="document-link" href="${escapeHtml(tire.documentUrl)}" target="_blank" rel="noopener">查看产品追溯 PDF</a>`;
    } catch (error) {
        fail("无法核验该二维码", error.message || "请联系仓库或质量管理人员");
    }
}

function fail(title, message) {
    document.body.classList.add("trace-error");
    document.getElementById("public-status").textContent = title;
    document.getElementById("public-hint").textContent = message;
    const content = document.getElementById("public-content");
    content.classList.remove("loading");
    content.innerHTML = `<div class="error-card"><strong>!</strong><p>二维码可能已失效、内容不完整，或追溯服务暂时不可用。</p></div>`;
}

function row(label, value) {
    return `<div><dt>${label}</dt><dd>${escapeHtml(value || "—")}</dd></div>`;
}

function statusText(value) {
    return { IN_STOCK: "已完成质检入库", VOID: "该轮胎追溯码已作废" }[value] || "追溯信息已读取";
}

function formatTime(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { hour12: false });
}

function escapeHtml(value) {
    return String(value ?? "").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#39;");
}
