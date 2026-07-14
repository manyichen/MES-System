let appInitialized = false;

const MODULE_PRESENTATION = {
    planning: { icon: "▤", eyebrow: "生产计划", title: "计划与工单", description: "管理客户订单、生产任务、齐套分析和工单执行。" },
    warehouse: { icon: "▦", eyebrow: "仓储作业", title: "仓储物流", description: "查看库存资源并处理领料、拣货和配送业务。" },
    production: { icon: "◉", eyebrow: "生产执行", title: "生产报工", description: "提交生产实绩，跟踪报工审核和计件结果。" },
    quality: { icon: "◇", eyebrow: "质量控制", title: "质量管理", description: "执行质量检验、结果判定和返工闭环。" },
    equipment: { icon: "⚙", eyebrow: "设备保障", title: "设备维护", description: "掌握设备状态，处理报修、维修和验收任务。" },
    process: { icon: "⌘", eyebrow: "工艺工程", title: "工艺管理", description: "查看原料和产品主数据，维护轮胎制造方法和工艺路线。" },
    trace: { icon: "⌁", eyebrow: "全程追溯", title: "产品追溯", description: "按追溯码、批次和工单查询产品生产履历。" },
    feedback: { icon: "✦", eyebrow: "经营管理", title: "管理反馈", description: "记录并跟踪生产经营过程中的管理意见。" },
    systemOps: { icon: "⚙", eyebrow: "系统运行", title: "系统运维", description: "巡检会话、锁定账号、系统健康和数据同步状态。" },
    audit: { icon: "◎", eyebrow: "安全审计", title: "审计日志", description: "查看登录、授权和关键操作留下的审计记录。" },
    system: { icon: "♙", eyebrow: "系统管理", title: "用户与权限", description: "管理用户角色、数据范围和权限变更申请。" }
};

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
    updateModuleWorkspace(panel);
    if (anchorId) window.setTimeout(() => document.getElementById(anchorId)?.scrollIntoView({ behavior: "smooth", block: "center" }), 80);
}

function setupModuleWorkspaces() {
    Object.entries(MODULE_PRESENTATION).forEach(([panelId, config]) => {
        const panel = document.getElementById(panelId);
        const grid = panel?.querySelector(":scope > .grid");
        const header = panel?.querySelector(":scope > header");
        if (!panel || !grid || !header || panel.dataset.workspaceReady) return;
        panel.dataset.workspaceReady = "true";

        const forms = [...grid.querySelectorAll(":scope > form.tool")];
        const utilityForms = forms.filter(form => /(?:search|filter)/i.test(form.id));
        const actionForms = forms.filter(form => !utilityForms.includes(form));
        const detailPanels = [...grid.querySelectorAll(":scope > .detail-panel")];
        const views = [...grid.children].filter(item => !actionForms.includes(item) && !detailPanels.includes(item));

        const banner = document.createElement("section");
        banner.className = "module-overview";
        banner.innerHTML = `<div class="module-overview-icon">${config.icon}</div><div class="module-overview-copy"><span>${config.eyebrow}</span><h3>${config.title}</h3><p>${config.description}</p></div><div class="module-overview-stats"><div><strong data-module-records>0</strong><span>当前记录</span></div><div><strong>${views.length}</strong><span>数据视图</span></div><div><strong>${actionForms.filter(form => !form.classList.contains("permission-hidden")).length}</strong><span>可用操作</span></div></div>`;
        header.after(banner);

        const tabs = document.createElement("nav");
        tabs.className = "module-view-tabs";
        views.forEach((view, index) => {
            const title = view.querySelector("h3,h4")?.textContent?.replace(/（.*$/, "").trim() || `数据视图 ${index + 1}`;
            view.dataset.workspaceView = String(index);
            view.classList.toggle("workspace-view-active", index === 0);
            const button = document.createElement("button");
            button.type = "button";
            button.className = index === 0 ? "active" : "";
            button.dataset.workspaceTarget = String(index);
            button.innerHTML = `<span>${escapeHtml(title)}</span><small data-view-count="${index}"></small>`;
            button.addEventListener("click", () => selectModuleView(panel, String(index)));
            tabs.appendChild(button);
        });
        banner.after(tabs);

        if (actionForms.length) {
            const actions = header.querySelector(".actions") || header.appendChild(Object.assign(document.createElement("div"), { className: "actions" }));
            const trigger = document.createElement("button");
            trigger.type = "button";
            trigger.className = "module-action-trigger";
            trigger.innerHTML = `<span>＋</span>业务操作`;
            actions.prepend(trigger);
            const drawer = document.createElement("aside");
            drawer.className = "module-drawer";
            drawer.innerHTML = `<div class="module-drawer-head"><div><span>${config.eyebrow}</span><h3>${config.title} · 业务操作</h3></div><button type="button" class="drawer-close" aria-label="关闭">×</button></div><div class="module-action-tabs"></div><div class="module-drawer-body"></div>`;
            document.body.appendChild(drawer);
            actionForms.forEach((form, index) => {
                const title = form.querySelector("h3")?.textContent?.trim() || `操作 ${index + 1}`;
                form.dataset.actionView = String(index);
                form.classList.toggle("action-view-active", index === 0);
                drawer.querySelector(".module-drawer-body").appendChild(form);
                const tab = document.createElement("button");
                tab.type = "button";
                tab.className = index === 0 ? "active" : "";
                tab.textContent = title;
                tab.addEventListener("click", () => selectActionView(drawer, String(index)));
                drawer.querySelector(".module-action-tabs").appendChild(tab);
            });
            trigger.addEventListener("click", () => openModuleDrawer(drawer));
            drawer.querySelector(".drawer-close").addEventListener("click", closeModuleDrawers);
        }

        detailPanels.forEach(detail => detail.classList.add("detail-drawer"));
        updateModuleWorkspace(panel);
    });
}

function selectModuleView(panel, target) {
    panel.querySelectorAll("[data-workspace-view]").forEach(view => view.classList.toggle("workspace-view-active", view.dataset.workspaceView === target));
    panel.querySelectorAll("[data-workspace-target]").forEach(button => button.classList.toggle("active", button.dataset.workspaceTarget === target));
}

function selectActionView(drawer, target) {
    drawer.querySelectorAll("[data-action-view]").forEach(view => view.classList.toggle("action-view-active", view.dataset.actionView === target));
    [...drawer.querySelectorAll(".module-action-tabs button")].forEach((button, index) => button.classList.toggle("active", String(index) === target));
}

function syncDrawerActions(drawer) {
    const views = [...drawer.querySelectorAll("[data-action-view]")];
    const buttons = [...drawer.querySelectorAll(".module-action-tabs button")];
    let firstAllowed = null;
    views.forEach((view, index) => {
        const allowed = !view.classList.contains("permission-hidden");
        buttons[index]?.classList.toggle("permission-hidden", !allowed);
        if (allowed && firstAllowed === null) firstAllowed = view.dataset.actionView;
    });
    const active = views.find(view => view.classList.contains("action-view-active") && !view.classList.contains("permission-hidden"));
    if (!active && firstAllowed !== null) {
        selectActionView(drawer, firstAllowed);
    }
}

function openModuleDrawer(drawer) {
    closeModuleDrawers();
    syncDrawerActions(drawer);
    ensureWorkspaceBackdrop().classList.add("open");
    drawer.classList.add("open");
    document.body.classList.add("overlay-open");
}

function closeModuleDrawers() {
    document.querySelectorAll(".module-drawer.open,.detail-drawer.open").forEach(item => item.classList.remove("open"));
    document.getElementById("workspace-backdrop")?.classList.remove("open");
    document.body.classList.remove("overlay-open");
}

function ensureWorkspaceBackdrop() {
    let backdrop = document.getElementById("workspace-backdrop");
    if (!backdrop) {
        backdrop = document.createElement("div");
        backdrop.id = "workspace-backdrop";
        backdrop.className = "workspace-backdrop";
        backdrop.addEventListener("click", closeModuleDrawers);
        document.body.appendChild(backdrop);
    }
    return backdrop;
}

function updateModuleWorkspace(panelOrChild) {
    const panel = panelOrChild?.classList?.contains("panel") ? panelOrChild : panelOrChild?.closest?.(".panel");
    if (!panel?.dataset.workspaceReady) return;
    let total = 0;
    panel.querySelectorAll(".data-summary strong").forEach(value => total += Number(value.textContent) || 0);
    const record = panel.querySelector("[data-module-records]");
    if (record) record.textContent = total;
    panel.querySelectorAll("[data-workspace-view]").forEach(view => {
        const count = [...view.querySelectorAll(".data-summary strong")].reduce((sum, value) => sum + (Number(value.textContent) || 0), 0);
        const badge = panel.querySelector(`[data-view-count="${view.dataset.workspaceView}"]`);
        if (badge) badge.textContent = count ? String(count) : "";
    });
}

function initializeApp() {
    if (appInitialized) {
        return;
    }
    appInitialized = true;
    setupModuleWorkspaces();
    bindNavigation();
    if (typeof bindDashboardEvents === "function") bindDashboardEvents();
    if (typeof bindQualityEvents === "function") bindQualityEvents();
    if (typeof bindEquipmentEvents === "function") bindEquipmentEvents();
    if (typeof bindProcessEvents === "function") bindProcessEvents();
    if (typeof bindProfileEvents === "function") bindProfileEvents();
    if (typeof loadDashboard === "function") loadDashboard();
    if (typeof loadProfile === "function") loadProfile();
    if ((hasPermission("planning.read") || hasPermission("planning.work_order.read")) && typeof refreshPlanning === "function") refreshPlanning();
    if (hasPermission("warehouse.read") && typeof refreshWarehouse === "function") refreshWarehouse();
    if (hasPermission("production.read") && typeof refreshProduction === "function") refreshProduction();
    if (hasPermission("quality.read") && typeof loadQuality === "function") loadQuality();
    if (hasPermission("equipment.read") && typeof loadEquipment === "function") loadEquipment();
    if (hasPermission("process.read") && typeof loadProcess === "function") loadProcess();
    if (hasPermission("trace.read") && typeof loadTraces === "function") loadTraces();
    if (hasPermission("feedback.read") && typeof loadFeedback === "function") loadFeedback(1);
    if (typeof loadAccessManagement === "function") loadAccessManagement();
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
