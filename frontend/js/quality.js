async function loadQuality() {
    try {
        const inspections = await getJson("/quality-inspections");
        renderTable("quality-table", inspections, [
            { key: "inspectionId", label: "ID" },
            { key: "inspectionNo", label: "质检单" },
            { key: "workOrderId", label: "工单" },
            { key: "workReportId", label: "报工" },
            { key: "sampleQty", label: "样本数" },
            { key: "inspectionStatus", label: "状态" },
            { key: "judgementResult", label: "判定" }
        ], [
            { name: "assign-inspection", label: "分配", idKey: "inspectionId", permission: "quality.inspection.assign", handler: assignInspection },
            { name: "submit-inspection", label: "提交审核", idKey: "inspectionId", permission: "quality.inspect", handler: submitInspection },
            { name: "judge-pass", label: "审核通过", idKey: "inspectionId", permission: "quality.review", handler: id => judgeInspection(id, "PASS") },
            { name: "judge-rework", label: "退回返工", idKey: "inspectionId", permission: "quality.review", handler: id => judgeInspection(id, "REWORK") }
        ]);

        const reworks = await getJson("/rework-orders").catch(() => []);
        renderTable("rework-table", reworks, [
            { key: "reworkOrderId", label: "ID" },
            { key: "reworkOrderNo", label: "返工单" },
            { key: "inspectionId", label: "质检单" },
            { key: "reworkReason", label: "原因" },
            { key: "reworkStatus", label: "状态" }
        ], [
            { name: "dispatch-rework", label: "派发", idKey: "reworkOrderId", permission: "quality.rework.manage", handler: dispatchRework },
            { name: "finish-rework", label: "完成", idKey: "reworkOrderId", permission: "quality.rework.manage", handler: finishRework }
        ]);
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function assignInspection(id) {
    const inspectorId = window.prompt("请输入质检员的用户 ID");
    if (!inspectorId) return;
    await postJson(`/quality-inspections/${id}/assign?inspectorId=${encodeURIComponent(inspectorId)}`);
    showMessage("质检任务已分配");
    await loadQuality();
    await loadDashboard();
}

async function submitInspection(id) {
    await postJson(`/quality-inspections/${id}/submit`);
    showMessage("检验结果已提交质量主管审核");
    await loadQuality();
    await loadDashboard();
}

async function judgeInspection(id, result) {
    await postJson(`/quality-inspections/${id}/judge`, {
        status: result,
        result
    });
    showMessage(`质检结果：${displayText(result)}`);
    loadQuality();
    loadDashboard();
}

async function dispatchRework(id) {
    await postJson(`/rework-orders/${id}/dispatch`);
    showMessage("返工单已派发");
    loadQuality();
}

async function finishRework(id) {
    await postJson(`/rework-orders/${id}/finish`);
    showMessage("返工单已完成");
    loadQuality();
}

function bindQualityEvents() {
    document.getElementById("refresh-quality")?.addEventListener("click", loadQuality);

    document.getElementById("inspection-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const payload = {
            ...formToObject(event.target),
            inspectionStatus: "CREATED",
            inspectionTime: nowIsoLocal(),
            judgementResult: null
        };
        await postJson("/quality-inspections", payload);
        showMessage("质检单已创建");
        event.target.reset();
        loadQuality();
    });

    document.getElementById("inspection-item-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const payload = formToObject(event.target);
        const inspectionId = payload.inspectionId;
        await postJson(`/quality-inspections/${inspectionId}/items`, payload);
        showMessage("质检项目已添加");
        event.target.reset();
    });
}
