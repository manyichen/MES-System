const AUTH_STORAGE_KEY = "mes.auth.session";

function getCurrentSession() {
    try {
        return JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) || "null");
    } catch {
        return null;
    }
}

function setCurrentSession(session) {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
}

function clearCurrentSession() {
    localStorage.removeItem(AUTH_STORAGE_KEY);
}

function hasPermission(permission) {
    if (!permission) return true;
    return new Set(getCurrentSession()?.permissions || []).has(permission);
}

function hasRole(roleCode) {
    return new Set(getCurrentSession()?.roles || []).has(roleCode);
}

function applyPermissionVisibility() {
    document.querySelectorAll("[data-permission]").forEach(element => {
        const allowed = element.dataset.permission.split("|").some(hasPermission);
        element.classList.toggle("permission-hidden", !allowed);
    });
    document.querySelectorAll("[data-deny-role]").forEach(element => {
        const denied = element.dataset.denyRole.split("|").some(hasRole);
        element.classList.toggle("permission-hidden", denied);
    });
    if (hasRole("PROCESS_ENGINEER")) {
        ["planning", "quality", "equipment", "production"].forEach(tab => {
            document.querySelector(`.sidebar button[data-tab="${tab}"]`)?.classList.add("permission-hidden");
        });
    }
    const activeButton = document.querySelector(".sidebar button[data-tab].active");
    if (activeButton?.classList.contains("permission-hidden")) {
        const fallback = document.querySelector(".sidebar button[data-tab]:not(.permission-hidden)");
        if (fallback) {
            document.querySelectorAll(".sidebar button[data-tab]").forEach(item => item.classList.remove("active"));
            document.querySelectorAll(".panel").forEach(item => item.classList.remove("active"));
            fallback.classList.add("active");
            document.getElementById(fallback.dataset.tab)?.classList.add("active");
        }
    }
    document.querySelectorAll(".sidebar .nav-group").forEach(group => {
        let hasVisibleItem = false;
        for (let item = group.nextElementSibling; item && !item.classList.contains("nav-group"); item = item.nextElementSibling) {
            if (item.matches("button[data-tab]") && !item.classList.contains("permission-hidden")) {
                hasVisibleItem = true;
                break;
            }
        }
        group.classList.toggle("permission-hidden", !hasVisibleItem);
    });
}

async function initAuthGate(onAuthenticated) {
    const session = getCurrentSession();
    const loginScreen = document.getElementById("login-screen");
    const loginForm = document.getElementById("login-form");
    const loginError = document.getElementById("login-error");
    const logoutButton = document.getElementById("logout-button");

    function enter(sessionData) {
        document.body.classList.remove("auth-locked");
        loginScreen?.classList.add("hidden");
        renderCurrentUser(sessionData?.user);
        applyPermissionVisibility();
        onAuthenticated?.(sessionData);
    }

    if (session?.token) {
        try {
            const current = await getJson("/auth/me");
            const refreshed = { token: session.token, ...current };
            setCurrentSession(refreshed);
            enter(refreshed);
        } catch (error) {
            clearCurrentSession();
            loginError.textContent = "登录状态已失效，请重新登录";
        }
    }

    loginForm?.addEventListener("submit", async event => {
        event.preventDefault();
        loginError.textContent = "";
        const body = formToObject(loginForm);
        try {
            const loginSession = await postJson("/auth/login", body);
            setCurrentSession(loginSession);
            enter(loginSession);
            showMessage("登录成功", "ok");
        } catch (error) {
            loginError.textContent = error.message || "登录失败";
        }
    });

    logoutButton?.addEventListener("click", async () => {
        try {
            await postJson("/auth/logout");
        } catch {
            // 本地仍需退出，后端会话可能已经过期。
        } finally {
            clearCurrentSession();
            window.location.reload();
        }
    });

    return Boolean(getCurrentSession()?.user);
}

function renderCurrentUser(user) {
    const box = document.getElementById("current-user");
    if (!box || !user) {
        return;
    }
    box.innerHTML = `
        <strong>${escapeHtml(user.realName || user.username)}</strong>
        <span>${escapeHtml((getCurrentSession()?.roles || [user.roleCode]).map(displayText).join(" / "))}</span>
        <span>${renderScopeSummary(getCurrentSession())}</span>
    `;
}

function renderScopeSummary(session) {
    const lineIds = session?.lineIds || [];
    const warehouseIds = session?.warehouseIds || [];
    const parts = [];
    if (lineIds.length) parts.push(`产线 ${lineIds.join(",")}`);
    if (warehouseIds.length) parts.push(`仓库 ${warehouseIds.join(",")}`);
    return escapeHtml(parts.length ? parts.join("；") : "按当前角色权限访问");
}
