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

function initAuthGate(onAuthenticated) {
    const session = getCurrentSession();
    const loginScreen = document.getElementById("login-screen");
    const loginForm = document.getElementById("login-form");
    const loginError = document.getElementById("login-error");
    const logoutButton = document.getElementById("logout-button");

    function enter(sessionData) {
        document.body.classList.remove("auth-locked");
        loginScreen?.classList.add("hidden");
        renderCurrentUser(sessionData?.user);
        onAuthenticated?.(sessionData);
    }

    if (session?.user) {
        enter(session);
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

    logoutButton?.addEventListener("click", () => {
        clearCurrentSession();
        window.location.reload();
    });

    return Boolean(session?.user);
}

function renderCurrentUser(user) {
    const box = document.getElementById("current-user");
    if (!box || !user) {
        return;
    }
    box.innerHTML = `
        <strong>${escapeHtml(user.realName || user.username)}</strong>
        <span>${escapeHtml(user.roleCode || "")}</span>
    `;
}
