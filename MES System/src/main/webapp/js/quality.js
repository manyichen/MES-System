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
            { name: "judge-pass", label: "判定通过", idKey: "inspectionId", handler: id => judgeInspection(id, "PASS") },
            { name: "judge-rework", label: "返工", idKey: "inspectionId", handler: id => judgeInspection(id, "REWORK") }
        ]);

        const reworks = await getJson("/rework-orders").catch(() => []);
        renderTable("rework-table", reworks, [
            { key: "reworkOrderId", label: "ID" },
            { key: "reworkOrderNo", label: "返工单" },
            { key: "inspectionId", label: "质检单" },
            { key: "reworkReason", label: "原因" },
            { key: "reworkStatus", label: "状态" }
        ], [
            { name: "dispatch-rework", label: "派发", idKey: "reworkOrderId", handler: dispatchRework },
            { name: "finish-rework", label: "完成", idKey: "reworkOrderId", handler: finishRework }
        ]);
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function judgeInspection(id, result) {
    await postJson(`/quality-inspections/${id}/judge`, {
        status: result,
        result
    });
    showMessage(`质检已判定为 ${result}`);
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
