let processCache = { materials: [], products: [], routes: [] };

async function loadProcess(options = {}) {
    try {
        const [materials, products, routes] = await Promise.all([
            getJson("/materials"),
            getJson("/products"),
            getJson("/process-routes")
        ]);
        processCache = { materials, products, routes };
        renderProcessTables();
        if (options.notify) showMessage("工艺数据已刷新", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function renderProcessTables() {
    renderTable("process-material-table", processCache.materials, [
        { key: "materialId", label: "ID" },
        { key: "materialCode", label: "原料编码" },
        { key: "materialName", label: "原料名称" },
        { key: "materialType", label: "类型" },
        { key: "specification", label: "规格" },
        { key: "unit", label: "单位" }
    ]);

    renderTable("process-product-table", processCache.products, [
        { key: "productId", label: "ID" },
        { key: "productCode", label: "产品编码" },
        { key: "productName", label: "轮胎名称" },
        { key: "productModel", label: "规格型号" }
    ]);

    renderTable("process-route-table", processCache.routes, [
        { key: "processId", label: "ID" },
        { key: "productId", label: "产品ID" },
        { key: "processCode", label: "方法编码" },
        { key: "processName", label: "制造方法" },
        { key: "processSeq", label: "顺序" },
        { key: "workCenter", label: "设备/工作中心" },
        { key: "enabled", label: "状态", render: row => Number(row.enabled) === 1 ? "启用" : "停用" }
    ], [
        { name: "edit-process-route", label: "编辑", idKey: "processId", permission: "process.manage", handler: editProcessRoute },
        { name: "delete-process-route", label: "删除", idKey: "processId", permission: "process.manage", handler: deleteProcessRoute }
    ]);
}

function bindProcessEvents() {
    document.getElementById("refresh-process")?.addEventListener("click", () => loadProcess({ notify: true }));
    document.getElementById("process-route-reset")?.addEventListener("click", resetProcessRouteForm);
    document.getElementById("process-route-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const status = document.getElementById("process-route-status");
        if (status) status.textContent = "正在保存制造方法...";
        try {
            const form = event.target;
            const payload = formToObject(form);
            payload.productId = payload.productId ? Number(payload.productId) : null;
            payload.processSeq = Number(payload.processSeq || 1);
            payload.enabled = Number(payload.enabled ?? 1);
            const id = payload.processId;
            delete payload.processId;
            if (id) {
                await putJson(`/process-routes/${id}`, payload);
            } else {
                await postJson("/process-routes", payload);
            }
            if (status) status.textContent = "制造方法已保存";
            showMessage("制造方法已保存", "ok");
            resetProcessRouteForm();
            await loadProcess();
        } catch (error) {
            const message = toChineseError(error);
            if (status) status.textContent = `保存失败：${message}`;
            showMessage(message, "error");
        }
    });
}

function editProcessRoute(id) {
    const route = processCache.routes.find(item => String(item.processId) === String(id));
    const form = document.getElementById("process-route-form");
    if (!route || !form) return;
    form.processId.value = route.processId || "";
    form.productId.value = route.productId || "";
    form.processCode.value = route.processCode || "";
    form.processName.value = route.processName || "";
    form.processSeq.value = route.processSeq || 1;
    form.workCenter.value = route.workCenter || "";
    form.enabled.value = route.enabled == null ? "1" : String(route.enabled);
    const drawer = form.closest(".module-drawer");
    if (drawer && typeof selectActionView === "function") {
        selectActionView(drawer, form.dataset.actionView);
    }
    if (drawer && typeof openModuleDrawer === "function") {
        openModuleDrawer(drawer);
    } else {
        form.scrollIntoView({ behavior: "smooth", block: "start" });
    }
}

async function deleteProcessRoute(id) {
    if (!window.confirm("确定删除这条制造方法吗？")) return;
    try {
        await requestJson(`/process-routes/${id}`, { method: "DELETE" });
        showMessage("制造方法已删除", "ok");
        await loadProcess();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function resetProcessRouteForm() {
    const form = document.getElementById("process-route-form");
    if (!form) return;
    form.reset();
    form.processId.value = "";
    form.productId.value = "1";
    form.processCode.value = `PROC-${Date.now()}`;
    form.processName.value = "密炼混炼";
    form.processSeq.value = "1";
    form.workCenter.value = "密炼机 GK400";
    form.enabled.value = "1";
    const status = document.getElementById("process-route-status");
    if (status) status.textContent = "";
}
