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
    const response = await fetch(`${API_BASE}${path}`, {
        headers: { "Content-Type": "application/json" },
        ...options
    });
    const payload = await response.json();
    if (!response.ok || payload.success === false) {
        throw new Error(payload.message || "请求失败");
    }
    return payload.data;
}

function getJson(path) {
    return requestJson(path);
}

function postJson(path, body = {}) {
    return requestJson(path, { method: "POST", body: JSON.stringify(body) });
}

function renderTable(containerId, rows, columns) {
    const container = document.getElementById(containerId);
    if (!rows.length) {
        container.innerHTML = "<p>暂无数据</p>";
        return;
    }
    const head = columns.map(col => `<th>${escapeHtml(col.title)}</th>`).join("");
    const body = rows.map(row => {
        const cells = columns.map(col => {
            const value = col.render ? col.render(row) : escapeHtml(row[col.key]);
            return `<td>${value}</td>`;
        }).join("");
        return `<tr>${cells}</tr>`;
    }).join("");
    container.innerHTML = `<table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>`;
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
    box.textContent = message;
    box.className = `show ${type}`;
    window.clearTimeout(showMessage.timer);
    showMessage.timer = window.setTimeout(() => {
        box.className = "";
    }, 2600);
}

document.querySelectorAll(".sidebar button").forEach(button => {
    button.addEventListener("click", () => {
        document.querySelectorAll(".sidebar button").forEach(item => item.classList.remove("active"));
        document.querySelectorAll(".panel").forEach(item => item.classList.remove("active"));
        button.classList.add("active");
        document.getElementById(button.dataset.tab).classList.add("active");
    });
});
