const API_BASE = "./api";

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

    const head = columns.map(col => `<th>${col.title || col.label}</th>`).join("")
        + (actions.length ? "<th>操作</th>" : "");
    const body = rows.map(row => {
        const cells = columns.map(col => {
            const value = col.render ? col.render(row) : row[col.key];
            return `<td>${formatCell(value)}</td>`;
        }).join("");
        const actionCells = actions.length
            ? `<td><div class="row-actions">${actions.map(action => {
                const id = row[action.idKey];
                return `<button type="button" data-action="${action.name}" data-id="${id}">${action.label}</button>`;
            }).join("")}</div></td>`
            : "";
        return `<tr>${cells}${actionCells}</tr>`;
    }).join("");

    container.innerHTML = `<table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>`;
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
