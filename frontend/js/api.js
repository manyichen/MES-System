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
    const response = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers
    });
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
        throw new Error(payload?.message || `HTTP ${response.status}`);
    }
    if (payload && payload.success === false) {
        throw new Error(payload.message || "请求失败");
    }
    return payload?.data ?? payload;
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
    if (typeof value === "boolean") return value ? "是" : "否";
    return escapeHtml(value);
}

function renderDetail(containerId, data, title = "详情") {
    const container = document.getElementById(containerId);
    if (!container) {
        return;
    }
    const rows = Object.entries(data ?? {}).map(([key, value]) => {
        const display = Array.isArray(value) || (value && typeof value === "object")
            ? `<pre>${escapeHtml(JSON.stringify(value, null, 2))}</pre>`
            : escapeHtml(value);
        return `<div class="detail-row"><span>${escapeHtml(key)}</span><strong>${display}</strong></div>`;
    }).join("");
    container.innerHTML = `<h3>${escapeHtml(title)}</h3>${rows || "<p>暂无详情</p>"}`;
}

function showMessage(message, type = "info") {
    const box = document.getElementById("message");
    if (!box) return;
    box.textContent = message;
    box.className = `show ${type}`;
    window.clearTimeout(showMessage.timer);
    showMessage.timer = window.setTimeout(() => {
        box.className = "";
    }, 2600);
}
