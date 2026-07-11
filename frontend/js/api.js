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
        response = await fetch(`${API_BASE}${path}`, {
            ...options,
            headers
        });
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
        throw new Error(toChineseError(payload.message || "\u8bf7\u6c42\u5931\u8d25"));
    }
    return payload?.data ?? payload;
}

function toChineseError(error) {
    const message = String(error?.message || error || "");
    if (!message || message === "Failed to fetch") return "\u65e0\u6cd5\u8fde\u63a5\u540e\u7aef\u670d\u52a1\uff0c\u8bf7\u786e\u8ba4\u670d\u52a1\u5df2\u542f\u52a8\u5e76\u5237\u65b0\u9875\u9762";
    if (message.includes("operatorId is required")) return "\u8bf7\u9009\u62e9\u63a5\u5355\u64cd\u4f5c\u5de5";
    if (message.includes("only CREATED work orders can be changed to DISPATCHED")) return "\u53ea\u6709\u5f85\u6d3e\u53d1\u5de5\u5355\u624d\u80fd\u6d3e\u53d1";
    if (message.includes("only DISPATCHED work orders can be changed to RECEIVED")) return "\u53ea\u6709\u5df2\u6d3e\u53d1\u7ed9\u5f53\u524d\u64cd\u4f5c\u5de5\u7684\u5de5\u5355\u624d\u80fd\u63a5\u6536";
    if (message.includes("work order status does not allow work report")) return "\u8be5\u5de5\u5355\u72b6\u6001\u4e0d\u5141\u8bb8\u62a5\u5de5\uff0c\u8bf7\u5148\u6d3e\u53d1\u5e76\u63a5\u6536\u5de5\u5355";
    if (message.includes("work order is not assigned to current operator")) return "\u8be5\u5de5\u5355\u672a\u6d3e\u53d1\u7ed9\u5f53\u524d\u64cd\u4f5c\u5de5\uff0c\u4e0d\u80fd\u62a5\u5de5";
    if (message.includes("warehouse location is required before completing picking")) return "\u5b8c\u6210\u62e3\u8d27\u524d\u5fc5\u987b\u6709\u4ed3\u5e93\u5e93\u4f4d\uff0c\u8bf7\u5148\u7ef4\u62a4\u5e93\u4f4d\u6216\u4f7f\u7528\u5df2\u6709\u5e93\u4f4d\u7684\u4ed3\u5e93";
    if (message.includes("only CREATED picking tasks can be completed")) return "\u53ea\u6709\u5f85\u62e3\u8d27\u72b6\u6001\u7684\u4efb\u52a1\u624d\u80fd\u5b8c\u6210\u62e3\u8d27";
    if (message.includes("only PENDING delivery tasks can arrive")) return "\u53ea\u6709\u5f85\u914d\u9001\u72b6\u6001\u7684\u4efb\u52a1\u624d\u80fd\u6807\u8bb0\u5230\u8fbe";
    if (message.includes("only ARRIVED delivery tasks can be confirmed")) return "\u53ea\u6709\u5df2\u5230\u8fbe\u7684\u914d\u9001\u4efb\u52a1\u624d\u80fd\u786e\u8ba4\u6536\u6599";
    if (message.includes("only CREATED requisitions can be approved")) return "\u53ea\u6709\u5f85\u5ba1\u6838\u7684\u9886\u6599\u4efb\u52a1\u624d\u80fd\u5ba1\u6838";
    if (message.includes("warehouse is required before approving requisition")) return "\u5ba1\u6838\u9886\u6599\u524d\u5fc5\u987b\u9009\u62e9\u76ee\u6807\u4ed3\u5e93";
    if (message.includes("inventory is not enough")) return "\u5e93\u5b58\u4e0d\u8db3\uff0c\u8bf7\u68c0\u67e5\u7269\u6599\u548c\u6279\u6b21\u5e93\u5b58";
    if (message.includes("requisition items are required")) return "\u9886\u6599\u660e\u7ec6\u4e0d\u80fd\u4e3a\u7a7a";
    if (message.includes("work order status does not allow requisition")) return "\u5f53\u524d\u5de5\u5355\u72b6\u6001\u4e0d\u5141\u8bb8\u521b\u5efa\u9886\u6599\u4efb\u52a1";
    if (message.includes("materials have already been received for this delivery task")) return "\u8be5\u914d\u9001\u4efb\u52a1\u5df2\u7ecf\u6536\u6599\uff0c\u4e0d\u80fd\u91cd\u590d\u786e\u8ba4";
    if (message.includes("Forbidden") || message.includes("HTTP 403")) return "\u5f53\u524d\u8d26\u53f7\u6ca1\u6709\u6743\u9650\u6267\u884c\u6b64\u64cd\u4f5c";
    if (message.includes("Unauthorized") || message.includes("HTTP 401")) return "\u767b\u5f55\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55";
    if (message.includes("HTTP 404")) return "\u8bf7\u6c42\u7684\u6570\u636e\u4e0d\u5b58\u5728";
    if (message.includes("database operation failed")) return "\u6570\u636e\u5e93\u64cd\u4f5c\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u6570\u636e\u662f\u5426\u5b8c\u6574";
    return message;
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
        container.innerHTML = "<p>\u6682\u65e0\u6570\u636e</p>";
        return;
    }
    const visibleActions = actions.filter(action => !action.permission || hasPermission(action.permission));
    const head = columns.map(col => `<th>${escapeHtml(col.title || col.label || "")}</th>`).join("")
        + (visibleActions.length ? "<th>\u64cd\u4f5c</th>" : "");
    const body = rows.map(row => {
        const cells = columns.map(col => {
            const value = col.render ? col.render(row) : formatCell(row[col.key]);
            return `<td>${value}</td>`;
        }).join("");
        const actionCells = visibleActions.length
            ? `<td><div class="row-actions">${visibleActions.map(action => {
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

function formatCell(value) {
    if (value === null || value === undefined) return "";
    if (typeof value === "boolean") return value ? "\u662f" : "\u5426";
    return escapeHtml(value);
}

function renderDetail(containerId, data, title = "\u8be6\u60c5") {
    const container = document.getElementById(containerId);
    if (!container) return;
    const rows = Object.entries(data ?? {}).map(([key, value]) => {
        const display = Array.isArray(value) || (value && typeof value === "object")
            ? `<pre>${escapeHtml(JSON.stringify(value, null, 2))}</pre>`
            : escapeHtml(value);
        return `<div class="detail-row"><span>${escapeHtml(key)}</span><strong>${display}</strong></div>`;
    }).join("");
    container.innerHTML = `<h3>${escapeHtml(title)}</h3>${rows || "<p>\u6682\u65e0\u8be6\u60c5</p>"}`;
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
