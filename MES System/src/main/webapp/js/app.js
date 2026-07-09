function bindTabs() {
    document.querySelectorAll(".tab").forEach(tab => {
        tab.addEventListener("click", () => {
            document.querySelectorAll(".tab").forEach(item => item.classList.remove("is-active"));
            document.querySelectorAll(".view").forEach(view => view.classList.remove("is-active"));
            tab.classList.add("is-active");
            document.getElementById(tab.dataset.target)?.classList.add("is-active");
        });
    });
}

window.addEventListener("DOMContentLoaded", () => {
    bindTabs();
    bindDashboardEvents();
    bindQualityEvents();
    bindEquipmentEvents();
    loadDashboard();
    loadQuality();
    loadEquipment();
    loadTraces();
    loadFeedback(1);
});
