const API_BASE = "./api";

const DISPLAY_TEXT = {
    SYSTEM_ADMIN: "系统管理员", HR_MANAGER: "人事经理",
    GENERAL_MANAGER: "总经理", PMC_PLANNER: "PMC计划员", WORKSHOP_MANAGER: "车间管理员",
    PRODUCTION_OPERATOR: "生产操作工", WORKSHOP_OPERATOR: "生产操作工", WAREHOUSE_ADMIN: "仓库管理员",
    WAREHOUSE_KEEPER: "仓储人员", QUALITY_MANAGER: "质量主管", QUALITY_INSPECTOR: "质检员",
    PROCESS_ENGINEER: "工艺工程师", EQUIPMENT_ADMIN: "设备管理员", EQUIPMENT_MAINTAINER: "设备维护员",
    CREATED: "待处理", DRAFT: "草稿", PENDING: "待处理", PENDING_PLAN: "待排产", PLANNED: "已排产",
    RELEASED: "已发布", READY: "齐套", NOT_READY: "未齐套", SHORTAGE: "缺料", ANALYZED: "已分析", PROCESSING: "处理中", IN_PROGRESS: "处理中", RESOLVED: "已解决",
    DISPATCHED: "已派发", RECEIVED: "已接收", RUNNING: "进行中", FINISHED: "已完成", COMPLETED: "已完成",
    CLOSED: "已关闭", CANCELLED: "已取消", SUBMITTED: "已提交", REVIEWED: "已复核", APPLIED: "已执行",
    APPROVED: "已通过", REJECTED: "已驳回", ASSIGNED: "已分配", ACCEPTED: "已验收", ARRIVED: "已到达",
    PICKING: "拣货中", DELIVERING: "配送中", CONVERTED: "已转维修", REPORTED: "已上报", VERIFIED: "已核验",
    WORKING: "作业中", CHARGING: "充电中", FAILED: "失败", PAUSED: "已暂停", WAITING: "等待中",
    UNSETTLED: "未结算", SETTLED: "已结算", PAID: "已支付", UNPAID: "未支付",
    OPEN: "待处理", PASS: "合格", FAIL: "不合格", REWORK: "返工", NORMAL: "正常", QUALITY_RISK: "质量风险", IN_STOCK: "已入库", VOID: "已作废",
    REWORKED: "已返工", SCRAPPED: "已报废", IDLE: "空闲", FAULT: "故障", MAINTENANCE: "维护中", DISABLED: "已停用",
    ENABLED: "已启用", HIGH: "高", MEDIUM: "中", LOW: "低", URGENT: "紧急", CRITICAL: "严重",
    MIXER: "密炼设备", MIXING: "炼胶", BUILDING: "成型", BUILDING_MACHINE: "成型机", CURING: "硫化", CURING_PRESS: "硫化机", INSPECTION: "检测设备", QUALITY: "质量",
    PRODUCTION: "生产", EQUIPMENT: "设备", MATERIAL: "物料", SYSTEM: "系统", EMPLOYEE: "员工账号",
    RAW: "原材料", SEMI_FINISHED: "半成品", FG: "成品", TRANSFER: "转移", RAW_MATERIAL: "原材料仓", WIP: "在制品仓", FINISHED_GOODS: "成品仓", SPARE_PARTS: "备件仓",
    IN: "入库", OUT: "出库", INBOUND: "入库", OUTBOUND: "出库", RESERVE: "预留", RELEASE: "释放预留",
    ADJUST_IN: "盘盈调整", ADJUST_OUT: "盘亏调整", PURCHASE_IN: "采购入库", FINISHED_GOODS_IN: "成品入库",
    EXTERNAL_PURCHASE: "外部采购", QUALITY_INSPECTION: "质检单", REQUISITION: "领料单", PICKING_TASK: "拣货任务",
    DELIVERY_TASK: "配送任务", WORK_REPORT: "报工单", CUSTOMER_ORDER: "客户订单", WORK_ORDER: "生产工单",
    ALL: "全部数据", DEPT: "本部门", SELF: "本人数据", LINE: "指定产线", WAREHOUSE: "指定仓库",
    READ: "查看", CREATE: "新建", UPDATE: "修改", DELETE: "删除", MANAGE: "管理", REVIEW: "审核",
    DISPATCH: "派发", RECEIVE: "接收", APPROVE: "审核通过", REJECT: "驳回", ASSIGN: "分配", ACCEPT: "验收",
    TRUE: "是", FALSE: "否"
};

const FIELD_TEXT = {
    id: "ID", userId: "用户ID", username: "登录账号", realName: "姓名", roleCode: "角色", department: "部门",
    phone: "联系电话", email: "电子邮箱", employeeNo: "员工编号", positionName: "岗位", enabled: "是否启用",
    orderId: "订单ID", orderNo: "订单编号", customerName: "客户名称", orderQty: "订单数量", orderStatus: "订单状态",
    taskId: "任务ID", taskNo: "任务编号", taskStatus: "任务状态", planQty: "计划数量", targetLineId: "目标产线",
    workOrderId: "工单ID", workOrderNo: "工单编号", workOrderStatus: "工单状态", plannedQty: "计划数量", actualQty: "实际数量",
    productId: "产品ID", productCode: "产品编码", productName: "产品名称", productModel: "产品型号",
    lineId: "产线ID", lineCode: "产线编码", lineName: "产线名称", processId: "工序ID", processName: "工序名称",
    materialId: "物料ID", materialCode: "物料编码", materialName: "物料名称", unit: "单位",
    warehouseId: "仓库ID", warehouseCode: "仓库编码", warehouseName: "仓库名称", warehouseType: "仓库类型",
    locationId: "库位ID", locationCode: "库位编码", locationName: "库位名称", inventoryId: "库存ID",
    batchNo: "批次号", availableQty: "可用数量", reservedQty: "预留数量", qualityStatus: "质量状态",
    transactionId: "流水ID", transactionNo: "流水编号", transactionType: "流水类型", qty: "数量", sourceDocType: "来源单据类型",
    requisitionId: "领料单ID", requisitionNo: "领料单编号", requestStatus: "领料状态", requiredQty: "需求数量",
    pickingTaskId: "拣货任务ID", pickingTaskNo: "拣货任务编号", deliveryTaskId: "配送任务ID", deliveryTaskNo: "配送任务编号",
    robotId: "机器人ID", robotCode: "机器人编码", robotName: "机器人名称", robotStatus: "机器人状态", batteryLevel: "剩余电量",
    reportId: "报工单ID", reportNo: "报工单编号", reportQty: "报工数量", qualifiedQty: "合格数量", defectQty: "不合格数量",
    reportStatus: "报工状态", workHours: "工时", operatorId: "操作工ID", wageId: "计件记录ID", wageAmount: "计件金额",
    inspectionId: "质检单ID", inspectionNo: "质检单编号", inspectionStatus: "质检状态", judgementResult: "判定结果",
    sampleQty: "抽检数量", inspectorId: "质检员ID", itemResult: "项目结果", reworkOrderId: "返工单ID", reworkReason: "返工原因",
    equipmentId: "设备ID", equipmentCode: "设备编码", equipmentName: "设备名称", equipmentType: "设备类型", equipmentStatus: "设备状态",
    repairReportId: "报修单ID", repairReportNo: "报修单编号", faultLevel: "故障等级", faultDesc: "故障描述", repairStatus: "报修状态",
    maintenanceOrderId: "维修工单ID", maintenanceOrderNo: "维修工单编号", maintenanceStatus: "维修状态", resultDesc: "维修结果",
    traceId: "追溯记录ID", traceCode: "追溯码", traceStatus: "追溯状态", feedbackId: "反馈ID", feedbackNo: "反馈编号",
    feedbackType: "反馈类型", feedbackContent: "反馈内容", feedbackStatus: "反馈状态", applyStatus: "申请状态",
    roleName: "角色名称", permissionCode: "权限编码", permissionName: "权限名称", description: "说明", dataScope: "数据范围",
    plannerId: "计划员ID", assignedOperatorId: "操作工ID", actorId: "操作人ID", reporterId: "上报人ID", maintainerId: "维护员ID",
    materialType: "物料类型", specification: "规格", frozenQty: "冻结数量", totalQty: "总数量", shortageQty: "缺料数量",
    processCode: "工序编码", processSeq: "工序顺序", requiredEquipmentType: "所需设备类型", priorityLevel: "优先级",
    deliveryDate: "交付日期", plannedStartTime: "计划开始时间", plannedEndTime: "计划结束时间", releaseTime: "发布时间",
    kittingStatus: "齐套状态", operationType: "操作类型", operationReason: "操作原因", operationTime: "操作时间",
    itemId: "项目ID", itemCode: "项目编码", itemName: "项目名称", standardValue: "标准值", actualValue: "实际值",
    maintenancePlanId: "维护计划ID", planName: "计划名称", cycleDays: "周期天数", nextMaintenanceDate: "下次维护日期",
    tireId: "轮胎实例ID", serialNo: "轮胎序列号", tireStatus: "轮胎状态", accessToken: "访问令牌", targetUrl: "扫码地址",
    qualifiedAt: "质检合格时间", inboundAt: "入库时间", printCount: "打印次数",
    recordCount: "记录数", operatorCount: "员工数", settlementStatus: "结算状态", pieceRate: "计件单价",
    createdAt: "创建时间", updatedAt: "更新时间", lastLoginAt: "最近登录时间", dispatchTime: "派发时间", receiveTime: "接收时间",
    completedTime: "完成时间", finishTime: "完成时间", remark: "备注", message: "说明"
};

function displayText(value) {
    if (value === null || value === undefined || value === "") return "";
    const key = String(value).trim();
    return DISPLAY_TEXT[key.toUpperCase()] || key;
}

function fieldText(key) {
    return FIELD_TEXT[key] || "其他信息";
}

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
    if (message.includes("only approved passed quality inspections can receive finished goods")) return "只有已审核通过且判定合格的质检单才能成品入库";
    if (message.includes("work report not found")) return "报工ID不存在，请检查报工列表";

    if (message.includes("warehouse location is required before completing picking")) return "完成拣货前必须有仓库库位，请先维护库位";
    if (message.includes("only CREATED picking tasks can be completed")) return "只有待拣货状态的任务才能完成拣货";
    if (message.includes("only PENDING delivery tasks can arrive")) return "只有待配送状态的任务才能标记到达";
    if (message.includes("only ARRIVED delivery tasks can be confirmed")) return "只有已到达的配送任务才能确认收料";
    if (message.includes("only CREATED requisitions can be received")) return "只有待接收的领料任务才能接收";
    if (message.includes("only RECEIVED requisitions can be approved")) return "请先由仓库管理员接收领料任务，再进行审批";
    if (message.includes("only CREATED requisitions can be approved")) return "只有待审核的领料任务才能审核";
    if (message.includes("only warehouse admins can receive or approve requisitions")) return "只有仓库管理员可以接收和审批领料任务";
    if (message.includes("only production operators can confirm requisition receipt")) return "只有申请领料的生产操作工可以确认收料";
    if (message.includes("delivery task is not requested by current operator")) return "该配送任务不属于当前操作工的领料申请";
    if (message.includes("only APPROVED requisitions can be received")) return "只有已审批发料的领料任务才能确认收料";
    if (message.includes("warehouse is required before approving requisition")) return "审核领料前必须选择目标仓库";
    if (message.includes("inventory is not enough")) return "目标仓库库存不足，请换有库存的仓库或清空批次号";
    if (message.includes("requisition items are required")) return "领料明细不能为空";
    if (message.includes("requiredQty must be positive")) return "领料数量必须大于 0";
    if (message.includes("purchase qty must be positive")) return "采购数量必须大于 0";
    if (message.includes("warehouse location does not belong to target warehouse")) return "采购库位不属于目标仓库";
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
    if (message.includes("AI 排产建议未开启")) return "AI 排产建议未开启，请确认 .env 中 AI_PLANNING_ENABLED=true 并重启后端";
    if (message.includes("未配置百炼 API Key")) return "未配置百炼 API Key，请确认 .env 中 DASHSCOPE_API_KEY 已填写并重启后端";
    if (message.includes("百炼 API 调用失败")) return message;
    if (message.includes("AI 排产建议生成失败")) return message;
    if (message.includes("没有可用于 AI 排产建议的生产任务")) return "当前没有可分析的生产任务，请先创建生产任务";
    if (message.includes("AI 排产建议一次最多分析 20 个生产任务")) return "一次最多选择 20 个生产任务";
    const exactMessages = {
        "work order not found": "生产工单不存在", "production task not found": "生产任务不存在",
        "order not found": "客户订单不存在", "product not found": "产品不存在", "material not found": "物料不存在",
        "warehouse not found": "仓库不存在", "warehouse location not found": "库位不存在", "inventory not found": "库存记录不存在",
        "requisition not found": "领料单不存在", "picking task not found": "拣货任务不存在", "delivery task not found": "配送任务不存在",
        "robot not found": "机器人不存在", "work report not found": "报工单不存在", "piecework wage not found": "计件工资记录不存在",
        "user not found": "用户不存在", "password is required": "密码不能为空"
    };
    const exact = exactMessages[message.trim().toLowerCase()];
    if (exact) return exact;
    if (/^[\x00-\x7F]+$/.test(message) && /[A-Za-z]/.test(message)) return "操作未完成，请检查填写内容或当前业务状态";
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
    const sourceRows = Array.isArray(rows) ? rows : [];
    const visibleActions = actions.filter(action => !action.permission || hasPermission(action.permission));
    const state = container._tableState || { query: "", sortKey: "", direction: "asc", page: 1, pageSize: 10, dense: false };
    container._tableState = state;
    const searchable = columns.filter(column => column.key);
    let visibleRows = sourceRows.filter(row => !state.query || searchable.some(column =>
        String(row[column.key] ?? "").toLowerCase().includes(state.query.toLowerCase())));
    if (state.sortKey) {
        visibleRows = [...visibleRows].sort((left, right) => compareTableValues(left[state.sortKey], right[state.sortKey]) * (state.direction === "desc" ? -1 : 1));
    }
    const pages = Math.max(1, Math.ceil(visibleRows.length / state.pageSize));
    state.page = Math.min(state.page, pages);
    const start = (state.page - 1) * state.pageSize;
    const pageRows = visibleRows.slice(start, start + state.pageSize);
    const head = columns.map(col => {
        const label = escapeHtml(col.title || col.label || "");
        if (!col.key) return `<th>${label}</th>`;
        const active = state.sortKey === col.key;
        return `<th><button type="button" class="table-sort${active ? " active" : ""}" data-sort-key="${escapeHtml(col.key)}">${label}<i>${active ? (state.direction === "asc" ? "↑" : "↓") : "↕"}</i></button></th>`;
    }).join("") + (visibleActions.length ? "<th class=\"action-column\">操作</th>" : "");
    const body = pageRows.map(row => {
        const cells = columns.map(col => {
            const value = col.render ? col.render(row) : formatSmartCell(row[col.key], col.key, col.title || col.label);
            return `<td data-label="${escapeHtml(col.title || col.label || "")}">${value}</td>`;
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

    const empty = `<div class="data-empty"><span>□</span><strong>${state.query ? "未找到匹配记录" : "暂无数据"}</strong><small>${state.query ? "请调整搜索条件" : "当前列表没有可显示的记录"}</small></div>`;
    container.classList.toggle("dense-table", state.dense);
    container.innerHTML = `<div class="data-view">
        <div class="data-toolbar">
            <div class="data-summary"><strong>${visibleRows.length}</strong><span>条记录</span>${sourceRows.length !== visibleRows.length ? `<small>共 ${sourceRows.length} 条</small>` : ""}</div>
            <div class="data-tools"><label class="table-search"><span>⌕</span><input type="search" value="${escapeHtml(state.query)}" placeholder="搜索当前列表"></label><button type="button" class="table-density secondary" title="切换表格密度">${state.dense ? "宽松" : "紧凑"}</button></div>
        </div>
        <div class="table-viewport">${pageRows.length ? `<table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>` : empty}</div>
        ${visibleRows.length ? `<div class="data-footer"><span>第 ${start + 1}-${Math.min(start + state.pageSize, visibleRows.length)} 条，共 ${visibleRows.length} 条</span><div class="pagination"><button type="button" data-page="${state.page - 1}" ${state.page <= 1 ? "disabled" : ""}>上一页</button><span>${state.page} / ${pages}</span><button type="button" data-page="${state.page + 1}" ${state.page >= pages ? "disabled" : ""}>下一页</button></div></div>` : ""}
    </div>`;
    container.querySelector(".table-search input")?.addEventListener("input", event => { state.query = event.target.value; state.page = 1; renderTable(containerId, sourceRows, columns, actions); });
    container.querySelector(".table-density")?.addEventListener("click", () => { state.dense = !state.dense; renderTable(containerId, sourceRows, columns, actions); });
    container.querySelectorAll("[data-sort-key]").forEach(button => button.addEventListener("click", () => {
        if (state.sortKey === button.dataset.sortKey) state.direction = state.direction === "asc" ? "desc" : "asc";
        else { state.sortKey = button.dataset.sortKey; state.direction = "asc"; }
        state.page = 1; renderTable(containerId, sourceRows, columns, actions);
    }));
    container.querySelectorAll("[data-page]").forEach(button => button.addEventListener("click", () => { state.page = Number(button.dataset.page); renderTable(containerId, sourceRows, columns, actions); }));
    visibleActions.forEach(action => {
        container.querySelectorAll(`[data-action="${action.name}"]`).forEach(button => {
            button.addEventListener("click", () => action.handler(button.dataset.id));
        });
    });
    if (typeof updateModuleWorkspace === "function") updateModuleWorkspace(container);
}

function compareTableValues(left, right) {
    if (left == null) return 1;
    if (right == null) return -1;
    if (typeof left === "number" && typeof right === "number") return left - right;
    return String(left).localeCompare(String(right), "zh-CN", { numeric: true, sensitivity: "base" });
}

function formatSmartCell(value, key = "", label = "") {
    if (value === null || value === undefined || value === "") return `<span class="cell-empty">—</span>`;
    if (typeof value === "boolean" || (/enabled|启用/.test(`${key} ${label}`) && (value === 1 || value === 0))) {
        const yes = value === true || value === 1;
        return `<span class="boolean-chip ${yes ? "yes" : "no"}">${yes ? "是" : "否"}</span>`;
    }
    const field = `${key} ${label}`.toLowerCase();
    if (/status|result|level|状态|结果|判定|级别/.test(field)) {
        const status = String(value);
        return `<span class="status status-${escapeHtml(status.toLowerCase().replaceAll("_", "-"))}">${escapeHtml(displayText(status))}</span>`;
    }
    if (/no$|code$|编号|编码|单号|批次|追溯码/.test(field)) return `<span class="code-cell">${escapeHtml(value)}</span>`;
    if (/time|date|created|updated|时间|日期/.test(field)) return `<span class="date-cell">${escapeHtml(formatTableDate(value))}</span>`;
    const text = displayText(value);
    return text.length > 38 ? `<span class="text-cell" title="${escapeHtml(text)}">${escapeHtml(text.slice(0, 38))}…</span>` : escapeHtml(text);
}

function formatTableDate(value) {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hour12: false });
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
            ? renderStructuredDetail(value)
            : formatSmartCell(value, key, fieldText(key));
        return `<div class="detail-row"><span>${escapeHtml(fieldText(key))}</span><strong>${display}</strong></div>`;
    }).join("");
    container.classList.add("detail-drawer", "open");
    container.innerHTML = `<div class="detail-drawer-head"><div><span>记录详情</span><h3>${escapeHtml(title)}</h3></div><button type="button" class="detail-close" aria-label="关闭">×</button></div><div class="detail-drawer-body">${rows || "<p>暂无详情</p>"}</div>`;
    container.querySelector(".detail-close")?.addEventListener("click", typeof closeModuleDrawers === "function" ? closeModuleDrawers : () => container.classList.remove("open"));
    if (typeof ensureWorkspaceBackdrop === "function") ensureWorkspaceBackdrop().classList.add("open");
    document.body.classList.add("overlay-open");
}

function renderStructuredDetail(value) {
    if (Array.isArray(value)) {
        if (!value.length) return `<span class="cell-empty">暂无数据</span>`;
        return `<div class="detail-list">${value.map((item, index) => {
            if (!item || typeof item !== "object") return `<span>${escapeHtml(displayText(item))}</span>`;
            return `<section><h4>第 ${index + 1} 项</h4>${Object.entries(item).map(([key, itemValue]) => `<div><span>${escapeHtml(fieldText(key))}</span><strong>${formatSmartCell(itemValue, key, fieldText(key))}</strong></div>`).join("")}</section>`;
        }).join("")}</div>`;
    }
    return `<div class="detail-object">${Object.entries(value).map(([key, itemValue]) => `<div><span>${escapeHtml(fieldText(key))}</span><strong>${formatSmartCell(itemValue, key, fieldText(key))}</strong></div>`).join("")}</div>`;
}

async function getBlob(path) {
    const session = typeof getCurrentSession === "function" ? getCurrentSession() : null;
    const response = await fetch(`${API_BASE}${path}`, {
        headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {}
    });
    if (!response.ok) {
        const text = await response.text();
        let message = text;
        try { message = JSON.parse(text)?.message || text; } catch { }
        throw new Error(toChineseError(message || `HTTP ${response.status}`));
    }
    return response.blob();
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
