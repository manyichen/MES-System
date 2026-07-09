function bindNavigation() {
    document.querySelectorAll(".sidebar button").forEach(button => {
        button.addEventListener("click", () => {
            document.querySelectorAll(".sidebar button").forEach(item => item.classList.remove("active"));
            document.querySelectorAll(".panel").forEach(item => item.classList.remove("active"));
            button.classList.add("active");
            document.getElementById(button.dataset.tab)?.classList.add("active");
        });
    });
}

window.addEventListener("DOMContentLoaded", () => {
    bindNavigation();
    if (typeof bindDashboardEvents === "function") bindDashboardEvents();
    if (typeof bindQualityEvents === "function") bindQualityEvents();
    if (typeof bindEquipmentEvents === "function") bindEquipmentEvents();
    if (typeof loadDashboard === "function") loadDashboard();
    if (typeof loadQuality === "function") loadQuality();
    if (typeof loadEquipment === "function") loadEquipment();
    if (typeof loadTraces === "function") loadTraces();
    if (typeof loadFeedback === "function") loadFeedback(1);
});
