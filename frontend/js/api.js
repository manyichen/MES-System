const API_BASE = "./api";

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

async function requestJson(path, options = {}) {
    const session = typeof getCurrentSession === "function" ? getCurrentSession() : null;
    const headers = {
        "Content-Type": "application/json",
        ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
        ...(options.headers || {})
    };
    let response;
    try {
        response = await fetch(`${API_BASE}${path}`, { ...options, headers });
    } catch (error) {
        throw new Error(toChineseError(error));
    }

    const text = await response.text();
    let payload = null;
    if (text) {
        try {
            payload = JSON.parse(text);
        } catch {
            payload = null;
        }
    }

    if (!response.ok) {
        if (response.status === 401 && !path.startsWith("/auth/login")) {
            if (typeof clearCurrentSession === "function") clearCurrentSession();
            document.body.classList.add("auth-locked");
            document.getElementById("login-screen")?.classList.remove("hidden");
        }
        throw new Error(toChineseError(payload?.message || text || `HTTP ${response.status}`));
    }
    if (payload && payload.success === false) {
        throw new Error(toChineseError(payload.message || "请求失败"));
    }
    return payload?.data ?? payload;
}

function toChineseError(error) {
    const raw = String(error?.message || error || "");
    const message = stripHtmlError(raw);
    if (!message || message === "Failed to fetch") return "无法连接后端服务，请确认服务已启动并刷新页面";
    if (message.includes("HTTP 405") || message.includes("Method Not Allowed")) return "当前后端接口不支持这个操作，请重启后端并确认接口已更新";
    if (message.includes("HTTP 500") || message.includes("Internal Server Error")) return "后端处理失败，请检查输入数据是否匹配业务流程";
    if (message.includes("HTTP 404")) return "请求的数据或接口不存在，请检查编号是否正确";
    if (message.includes("Forbidden") || message.includes("HTTP 403")) return "当前账号没有权限执行此操作";
    if (message.includes("Unauthorized") || message.includes("HTTP 401")) return "登录已失效，请重新登录";
    if (message.includes("column \"enabled\" is of type smallint") || (message.includes("enabled") && message.includes("smallint"))) return "设备启用状态格式不正确，请刷新页面后重试";
    if (message.includes("uk_mes_equipment_equipment_code") || (message.includes("equipment_code") && message.includes("already exists"))) return "设备编码已存在，请换一个设备编码";
    if (message.includes("uk_mes_equipment_repair_report_repair_report_no") || (message.includes("repair_report_no") && message.includes("already exists"))) return "报修单号已存在，请换一个报修单号";
    if (message.includes("duplicate key value violates unique constraint")) return "编号已存在，请更换编号后重试";

    if (message.includes("operatorId is required")) return "请选择接单操作工";
    if (message.includes("only CREATED work orders can be changed to DISPATCHED")) return "只有待派发工单才能派发";
    if (message.includes("only DISPATCHED work orders can be changed to RECEIVED")) return "只有已派发给当前操作工的工单才能接收";
    if (message.includes("work order status does not allow work report")) return "该工单状态不允许报工，请先派发并接收工单";
    if (message.includes("work order is not assigned to current operator")) return "该工单未派发给当前操作工，不能报工";
    if (message.includes("work report does not belong to this work order")) return "报工ID不属于当前工单，请选择同一条业务链路的工单和报工";
    if (message.includes("only APPROVED work reports can create quality inspections")) return "只有已审核通过的报工单才能创建质检单";
    if (message.includes("work report not found")) return "报工ID不存在，请检查报工列表";

    if (message.includes("warehouse location is required before completing picking")) return "完成拣货前必须有仓库库位，请先维护库位";
    if (message.includes("only CREATED picking tasks can be completed")) return "只有待拣货状态的任务才能完成拣货";
    if (message.includes("only PENDING delivery tasks can arrive")) return "只有待配送状态的任务才能标记到达";
    if (message.includes("only ARRIVED delivery tasks can be confirmed")) return "只有已到达的配送任务才能确认收料";
    if (message.includes("only CREATED requisitions can be approved")) return "只有待审核的领料任务才能审核";
    if (message.includes("warehouse is required before approving requisition")) return "审核领料前必须选择目标仓库";
    if (message.includes("inventory is not enough")) return "库存不足，请检查物料和批次库存";
    if (message.includes("requisition items are required")) return "领料明细不能为空";
    if (message.includes("work order status does not allow requisition")) return "当前工单状态不允许创建领料任务";
    if (message.includes("materials have already been received for this delivery task")) return "该配送任务已经收料，不能重复确认";

    if (message.includes("Item body is required")) return "请先填写质检项目内容";
    if (message.includes("Judgement status and result are required")) return "请选择质检判定结果";
    if (message.includes("Repair report body is required")) return "请先填写设备报修信息";
    if (message.includes("Maintenance plan body is required")) return "请先填写维护计划信息";
    if (message.includes("Product trace body is required")) return "请先填写产品追溯信息";
    if (message.includes("Product trace not found") || message.includes("产品追溯记录不存在")) return "未找到对应的产品追溯记录";
    if (message.includes("Metric body is required")) return "请先填写看板指标信息";
    if (message.includes("Metric key and name are required")) return "看板指标编码和名称不能为空";
    if (message.includes("database operation failed")) return "数据库操作失败，请检查数据是否完整";
    return message;
}

function stripHtmlError(message) {
    if (!message.includes("<html") && !message.includes("<!doctype")) return message;
    const title = message.match(/<title>(.*?)<\/title>/i)?.[1] || "";
    const serverMessage = message.match(/<p><b>Message<\/b>\s*(.*?)<\/p>/i)?.[1] || "";
    const text = `${title} ${serverMessage}`
        .replace(/<[^>]+>/g, " ")
        .replace(/\s+/g, " ")
        .trim();
    return text || message.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim();
}

function getJson(path) {
    return requestJson(path);
}

function postJson(path, body = {}) {
    return requestJson(path, {
        method: "POST",
        body: JSON.stringify(body)
    });
}

function putJson(path, body = {}) {
    return requestJson(path, {
        method: "PUT",
        body: JSON.stringify(body)
    });
}

function formToObject(form) {
    const data = new FormData(form);
    const result = {};
    for (const [key, value] of data.entries()) {
        if (value === "") {
            result[key] = null;
        } else if (form.elements[key]?.type === "number") {
            result[key] = Number(value);
        } else {
            result[key] = value;
        }
    }
    return result;
}

function nowIsoLocal() {
    return new Date().toISOString().slice(0, 19);
}

function renderTable(containerId, rows = [], columns = [], actions = []) {
    const container = document.getElementById(containerId);
    if (!container) return;
    if (!rows || rows.length === 0) {
        container.innerHTML = "<p>暂无数据</p>";
        return;
    }
    const visibleActions = actions.filter(action => !action.permission || hasPermission(action.permission));
    const head = columns.map(col => `<th>${escapeHtml(col.title || col.label || "")}</th>`).join("")
        + (visibleActions.length ? "<th>操作</th>" : "");
    const body = rows.map(row => {
        const cells = columns.map(col => {
            const value = col.render ? col.render(row) : formatCell(row[col.key], col.key || col.title || col.label);
            return `<td>${value}</td>`;
        }).join("");
        const rowActions = visibleActions.filter(action => !action.visible || action.visible(row));
        const actionCells = visibleActions.length
            ? `<td><div class="row-actions">${rowActions.map(action => {
                const id = row[action.idKey];
                return `<button type="button" data-action="${escapeHtml(action.name)}" data-id="${escapeHtml(id)}">${escapeHtml(action.label)}</button>`;
            }).join("")}</div></td>`
            : "";
        return `<tr>${cells}${actionCells}</tr>`;
    }).join("");

    container.innerHTML = `<table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>`;
    visibleActions.forEach(action => {
        container.querySelectorAll(`[data-action="${action.name}"]`).forEach(button => {
            button.addEventListener("click", () => action.handler(button.dataset.id));
        });
    });
}

function formatCell(value, field = "") {
    if (value === null || value === undefined) return "";
    if (typeof value === "boolean") return value ? "是" : "否";
    return escapeHtml(toDisplayText(value, field));
}

function renderDetail(containerId, data, title = "详情") {
    const container = document.getElementById(containerId);
    if (!container) return;
    const rows = Object.entries(data ?? {}).map(([key, value]) => {
        const display = Array.isArray(value) || (value && typeof value === "object")
            ? `<pre>${escapeHtml(JSON.stringify(value, null, 2))}</pre>`
            : escapeHtml(toDisplayText(value, key));
        return `<div class="detail-row"><span>${escapeHtml(fieldLabelText(key))}</span><strong>${display}</strong></div>`;
    }).join("");
    container.innerHTML = `<h3>${escapeHtml(title)}</h3>${rows || "<p>暂无详情</p>"}`;
}

function toDisplayText(value, field = "") {
    if (value === null || value === undefined) return "";
    const text = String(value);
    const key = String(field || "").toLowerCase();
    if (key.includes("status") || key.includes("状态") || key.includes("kitting") || key === "fromstatus" || key === "tostatus") {
        return statusText(text);
    }
    if (key.includes("type") || key.includes("类型") || key.includes("operation") || key.includes("action")) {
        return typeText(text);
    }
    if (key.includes("level") || key.includes("级别") || key.includes("priority")) {
        return levelText(text);
    }
    if (key.includes("cycle") || key.includes("周期")) {
        return cycleText(text);
    }
    if (key.includes("remark") || key.includes("desc") || key.includes("description") || key.includes("说明")) {
        return descriptionText(text);
    }
    return text;
}

function fieldLabelText(field) {
    return {
        operationType: "操作",
        fromStatus: "原状态",
        toStatus: "新状态",
        remark: "说明",
        faultDesc: "说明",
        description: "说明",
        resultDesc: "说明"
    }[field] || field;
}

function statusText(status) {
    return {
        CREATED: "已创建",
        PLANNED: "已计划",
        PENDING_PLAN: "待计划",
        RELEASED: "已发布",
        DISPATCHED: "已派发",
        RECEIVED: "已接收",
        APPROVED: "已审核",
        REPORTED: "已报修",
        CONVERTED: "已转维修",
        REJECTED: "已驳回",
        CLOSED: "已关闭",
        PENDING: "待处理",
        SUBMITTED: "待审核",
        COMPLETED: "已完成",
        ARRIVED: "已到达",
        NORMAL: "正常",
        QUALIFIED: "合格",
        UNQUALIFIED: "不合格",
        QUALITY_RISK: "质量风险",
        REWORKED: "已返工",
        SCRAPPED: "已报废",
        PASS: "合格",
        FAIL: "不合格",
        REWORK: "需返工",
        REWORK_REQUIRED: "需返工",
        FAILED: "不合格",
        NG: "不合格",
        IDLE: "空闲",
        RUNNING: "运行中",
        FAULT: "故障",
        MAINTENANCE: "维护中",
        DISABLED: "停用",
        ASSIGNED: "已派工",
        IN_PROGRESS: "处理中",
        FINISHED: "待验收",
        ACCEPTED: "已验收",
        SCHEDULED: "已排期",
        OPEN: "待处理",
        SETTLED: "已结算",
        UNSETTLED: "未结算",
        READY: "已齐套",
        NOT_READY: "未齐套",
        SHORTAGE: "缺料",
        ANALYZED: "已分析",
        WORKING: "工作中",
        CHARGING: "充电中",
        LOW_BATTERY: "低电量",
        ENABLED: "启用",
        DISABLE: "停用",
        ENABLE: "启用"
    }[status] || status;
}

function typeText(type) {
    return {
        CREATE: "创建",
        CREATED: "创建",
        UPDATE: "更新",
        DELETE: "删除",
        DISPATCH: "派发",
        RECEIVE: "接收",
        APPROVE: "审核",
        RELEASE: "发布",
        KITTING: "齐套分析",
        REPORT: "报工",
        INSPECT: "质检",
        REWORK: "返工",
        CLOSE: "关闭",
        QUALITY: "质量",
        PRODUCTION: "生产",
        EQUIPMENT: "设备",
        WAREHOUSE: "仓储",
        SYSTEM: "系统",
        REQUISITION: "领料单",
        PICKING_TASK: "拣货任务",
        DELIVERY_TASK: "配送任务",
        WORK_ORDER: "生产工单",
        REPAIR_REPORT: "报修单",
        MAINTENANCE_ORDER: "维修工单",
        RAW: "原材料",
        FINISHED: "成品",
        SEMI_FINISHED: "半成品",
        IN: "入库",
        OUT: "出库",
        TRANSFER: "转移",
        MIXER: "密炼机",
        BUILDING_MACHINE: "成型机",
        CURING_PRESS: "硫化机",
        INSPECTION: "检测设备"
    }[type] || type;
}

function levelText(level) {
    return {
        LOW: "低",
        MEDIUM: "中",
        HIGH: "高",
        URGENT: "紧急",
        CRITICAL: "严重",
        NORMAL: "普通",
        IMPORTANT: "重要",
        1: "低",
        2: "中",
        3: "高"
    }[level] || level;
}

function cycleText(cycle) {
    return {
        DAILY: "每日",
        WEEKLY: "每周",
        MONTHLY: "每月",
        QUARTERLY: "每季度",
        YEARLY: "每年"
    }[cycle] || cycle;
}

function descriptionText(description) {
    return {
        "work order created": "生产工单已创建",
        "work order dispatched": "生产工单已派发",
        "work order received": "生产工单已接收",
        "production task released": "生产任务已发布",
        "kitting analysis completed": "齐套分析已完成",
        "work report submitted": "报工单已提交",
        "work report approved": "报工单已审核",
        "quality inspection created": "质检单已创建",
        "repair report created": "报修单已创建",
        "maintenance order created": "维修工单已创建",
        "maintenance order assigned": "维修工单已派工",
        "maintenance order finished": "维修工单已完成",
        "maintenance order accepted": "维修工单已验收"
    }[description] || description;
}

function showMessage(message, type = "info") {
    const box = document.getElementById("message");
    if (!box) return;
    box.textContent = message;
    box.className = `show ${type}`;
    window.clearTimeout(showMessage.timer);
    showMessage.timer = window.setTimeout(() => {
        box.className = "";
    }, 3200);
}
