const API_BASE = "./api";

function showMessage(message, type = "ok") {
    const el = document.getElementById("message");
    if (!el) return;
    el.textContent = message || "";
    el.className = `message ${type}`;
}

async function requestJson(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, {
        headers: {
            "Content-Type": "application/json",
            ...(options.headers || {})
        },
        ...options
    });
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
        throw new Error(payload?.message || `HTTP ${response.status}`);
    }
    if (payload && payload.success === false) {
        throw new Error(payload.message || "操作失败");
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

function renderTable(containerId, rows, columns, actions = []) {
    const container = document.getElementById(containerId);
    if (!container) return;
    if (!rows || rows.length === 0) {
        container.innerHTML = '<div class="empty">暂无数据</div>';
        return;
    }

    const actionHead = actions.length ? "<th>操作</th>" : "";
    const head = columns.map(col => `<th>${col.label}</th>`).join("") + actionHead;
    const body = rows.map(row => {
        const cells = columns.map(col => `<td>${formatCell(row[col.key])}</td>`).join("");
        const buttons = actions.length
            ? `<td><div class="row-actions">${actions.map(action => {
                const value = row[action.idKey];
                return `<button type="button" data-action="${action.name}" data-id="${value}">${action.label}</button>`;
            }).join("")}</div></td>`
            : "";
        return `<tr>${cells}${buttons}</tr>`;
    }).join("");

    container.innerHTML = `<div class="table-wrap"><table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table></div>`;
    actions.forEach(action => {
        container.querySelectorAll(`[data-action="${action.name}"]`).forEach(button => {
            button.addEventListener("click", () => action.handler(button.dataset.id));
        });
    });
}

function formatCell(value) {
    if (value === null || value === undefined) return "";
    if (typeof value === "boolean") return value ? "是" : "否";
    return String(value);
}
