let appInitialized = false;

function bindNavigation() {
    document.querySelectorAll(".sidebar button[data-tab]").forEach(button => {
        button.addEventListener("click", () => {
            switchTab(button.dataset.tab);
        });
    });
}

function switchTab(tab, anchorId = null) {
    const button = document.querySelector(`.sidebar button[data-tab="${tab}"]`);
    const panel = document.getElementById(tab);
    if (!button || !panel || button.classList.contains("permission-hidden")) return;
    document.querySelectorAll(".sidebar button[data-tab]").forEach(item => item.classList.remove("active"));
    document.querySelectorAll(".panel").forEach(item => item.classList.remove("active"));
    button.classList.add("active");
    panel.classList.add("active");
    if (anchorId) window.setTimeout(() => document.getElementById(anchorId)?.scrollIntoView({ behavior: "smooth", block: "center" }), 80);
}

function initializeApp() {
    if (appInitialized) {
        return;
    }
    appInitialized = true;
    bindNavigation();
    if (typeof bindDashboardEvents === "function") bindDashboardEvents();
    if (typeof bindQualityEvents === "function") bindQualityEvents();
    if (typeof bindEquipmentEvents === "function") bindEquipmentEvents();
    if (typeof bindProfileEvents === "function") bindProfileEvents();
    if (typeof loadDashboard === "function") loadDashboard();
    if (typeof loadProfile === "function") loadProfile();
    if ((hasPermission("planning.read") || hasPermission("planning.work_order.read")) && typeof refreshPlanning === "function") refreshPlanning();
    if (hasPermission("warehouse.read") && typeof refreshWarehouse === "function") refreshWarehouse();
    if (hasPermission("production.read") && typeof refreshProduction === "function") refreshProduction();
    if (hasPermission("quality.read") && typeof loadQuality === "function") loadQuality();
    if (hasPermission("equipment.read") && typeof loadEquipment === "function") loadEquipment();
    if (hasPermission("trace.read") && typeof loadTraces === "function") loadTraces();
    if (hasPermission("feedback.read") && typeof loadFeedback === "function") loadFeedback(1);
    if (hasPermission("user.read") && typeof loadAccessManagement === "function") loadAccessManagement();
}

window.addEventListener("DOMContentLoaded", () => {
    const date = document.getElementById("workspace-date");
    if (date) date.textContent = new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", weekday: "short" }).format(new Date());
    if (typeof initAuthGate === "function") {
        void initAuthGate(initializeApp);
        return;
    }
    initializeApp();
});
