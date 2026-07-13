async function loadQuality(options = {}) {
    try {
        const inspections = await getJson("/quality-inspections");
        renderTable("quality-table", inspections, [
            { key: "inspectionId", label: "ID" },
            { key: "inspectionNo", label: "质检单" },
            { key: "workOrderId", label: "工单" },
            { key: "workReportId", label: "报工" },
            { key: "sampleQty", label: "样本数" },
            { key: "inspectionStatus", label: "状态", render: row => qualityStatusText(row.inspectionStatus) },
            { key: "judgementResult", label: "判定", render: row => qualityResultText(row.judgementResult) }
        ], [
            {
                name: "view-items",
                label: "项目",
                idKey: "inspectionId",
                permission: "quality.read",
                handler: loadInspectionItems
            },
            {
                name: "assign-inspection",
                label: "分配",
                idKey: "inspectionId",
                permission: "quality.inspection.assign",
                visible: row => row.inspectionStatus === "CREATED",
                handler: assignInspection
            },
            {
                name: "submit-inspection",
                label: "提交审核",
                idKey: "inspectionId",
                permission: "quality.inspect",
                visible: row => ["CREATED", "IN_PROGRESS"].includes(row.inspectionStatus),
                handler: submitInspection
            },
            {
                name: "judge-pass",
                label: "判定合格",
                idKey: "inspectionId",
                permission: "quality.review",
                visible: row => row.inspectionStatus === "SUBMITTED",
                handler: id => judgeInspection(id, "PASS")
            },
            {
                name: "judge-fail",
                label: "判定不合格",
                idKey: "inspectionId",
                permission: "quality.review",
                visible: row => row.inspectionStatus === "SUBMITTED",
                handler: id => judgeInspection(id, "FAIL")
            },
            {
                name: "judge-rework",
                label: "返工",
                idKey: "inspectionId",
                permission: "quality.review",
                visible: row => row.inspectionStatus === "SUBMITTED",
                handler: id => judgeInspection(id, "REWORK")
            }
        ]);

        const reworks = await getJson("/rework-orders").catch(() => []);
        renderTable("rework-table", reworks, [
            { key: "reworkOrderId", label: "ID" },
            { key: "reworkOrderNo", label: "返工单" },
            { key: "sourceWorkOrderId", label: "来源工单" },
            { key: "inspectionId", label: "质检单" },
            { key: "reworkReason", label: "原因" },
            { key: "reworkStatus", label: "状态", render: row => reworkStatusText(row.reworkStatus) }
        ], [
            {
                name: "dispatch-rework",
                label: "派发",
                idKey: "reworkOrderId",
                permission: "quality.rework.manage",
                visible: row => row.reworkStatus === "CREATED",
                handler: dispatchRework
            },
            {
                name: "finish-rework",
                label: "完成",
                idKey: "reworkOrderId",
                permission: "quality.rework.manage",
                visible: row => ["DISPATCHED", "IN_PROGRESS"].includes(row.reworkStatus),
                handler: finishRework
            }
        ]);
        if (options.notify) showMessage("质量管理数据已刷新", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function loadInspectionItems(id) {
    try {
        const rows = await getJson(`/quality-inspections/${id}/items`);
        document.getElementById("quality-item-title").textContent = `质检项目：质检单 ${id}`;
        renderTable("quality-item-table", rows, [
            { key: "inspectionItemId", label: "ID" },
            { key: "itemCode", label: "项目编码" },
            { key: "itemName", label: "项目名称" },
            { key: "standardValue", label: "标准值" },
            { key: "actualValue", label: "实际值" },
            { key: "itemResult", label: "结果", render: row => qualityResultText(row.itemResult) },
            { key: "remark", label: "备注" }
        ]);
        showMessage("质检项目已加载", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function assignInspection(id) {
    try {
        const inspectorId = window.prompt("请输入质检员用户 ID");
        if (!inspectorId) {
            showMessage("已取消分配质检任务", "info");
            return;
        }
        await postJson(`/quality-inspections/${id}/assign?inspectorId=${encodeURIComponent(inspectorId)}`);
        showMessage("质检任务已分配", "ok");
        await loadQuality();
        await loadDashboard();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function submitInspection(id) {
    try {
        await postJson(`/quality-inspections/${id}/submit`);
        showMessage("检验结果已提交质量主管审核", "ok");
        await loadQuality();
        await loadDashboard();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function judgeInspection(id, result) {
    try {
        await postJson(`/quality-inspections/${id}/judge`, { status: result, result });
        showMessage(`质检已完成判定：${qualityResultText(result)}`, "ok");
        await loadQuality();
        await loadDashboard();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function dispatchRework(id) {
    try {
        await postJson(`/rework-orders/${id}/dispatch`);
        showMessage("返工单已派发", "ok");
        await loadQuality();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

async function finishRework(id) {
    try {
        await postJson(`/rework-orders/${id}/finish`);
        showMessage("返工单已完成", "ok");
        await loadQuality();
        await loadDashboard();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function bindQualityEvents() {
    document.getElementById("refresh-quality")?.addEventListener("click", () => loadQuality({ notify: true }));

    document.getElementById("inspection-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const status = document.getElementById("inspection-form-status");
        if (status) status.textContent = "正在创建质检单...";
        try {
            const payload = {
                ...formToObject(event.target),
                inspectionStatus: "CREATED",
                inspectionTime: nowIsoLocal(),
                judgementResult: null
            };
            if (payload.inspectionNo === "QI-DEMO-001") {
                payload.inspectionNo = `QI-${Date.now()}`;
            }
            const id = await postJson("/quality-inspections", payload);
            if (status) status.textContent = `质检单创建成功，ID：${id}`;
            showMessage(`质检单创建成功，ID：${id}`, "ok");
            event.target.reset();
            await loadQuality();
        } catch (error) {
            const message = toChineseError(error);
            if (status) status.textContent = `创建失败：${message}`;
            showMessage(message, "error");
        }
    });

    document.getElementById("inspection-item-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        try {
            const payload = formToObject(event.target);
            const inspectionId = payload.inspectionId;
            const itemId = await postJson(`/quality-inspections/${inspectionId}/items`, payload);
            showMessage(`质检项目已添加，ID：${itemId}`, "ok");
            event.target.reset();
            event.target.inspectionId.value = inspectionId;
            await loadInspectionItems(inspectionId);
        } catch (error) {
            showMessage(toChineseError(error), "error");
        }
    });

    document.getElementById("rework-form")?.addEventListener("submit", async event => {
        event.preventDefault();
        const status = document.getElementById("rework-form-status");
        if (status) status.textContent = "正在创建返工单...";
        try {
            const payload = formToObject(event.target);
            payload.sourceWorkOrderId = Number(payload.sourceWorkOrderId);
            payload.inspectionId = payload.inspectionId ? Number(payload.inspectionId) : null;
            payload.assignedLineId = payload.assignedLineId ? Number(payload.assignedLineId) : null;
            payload.reworkStatus = "CREATED";
            payload.createdAt = nowIsoLocal();
            payload.closedAt = null;
            if (payload.reworkOrderNo === "RW-DEMO-001") {
                payload.reworkOrderNo = `RW-${Date.now()}`;
            }
            const id = await postJson("/rework-orders", payload);
            if (status) status.textContent = `返工单创建成功，ID：${id}`;
            showMessage(`返工单创建成功，ID：${id}`, "ok");
            event.target.reset();
            await loadQuality();
            await loadDashboard();
        } catch (error) {
            const message = toChineseError(error);
            if (status) status.textContent = `创建失败：${message}`;
            showMessage(message, "error");
        }
    });
}

function qualityStatusText(status) {
    return {
        CREATED: "待检验",
        IN_PROGRESS: "检验中",
        SUBMITTED: "待审核",
        APPROVED: "已合格",
        REJECTED: "不合格",
        REWORK_REQUIRED: "需返工"
    }[status] || escapeHtml(status || "");
}

function qualityResultText(result) {
    return {
        PASS: "合格",
        FAIL: "不合格",
        FAILED: "不合格",
        NG: "不合格",
        REWORK: "返工"
    }[result] || escapeHtml(result || "");
}

function reworkStatusText(status) {
    return {
        CREATED: "待派发",
        DISPATCHED: "已派发",
        IN_PROGRESS: "返工中",
        FINISHED: "已完成",
        CLOSED: "已关闭"
    }[status] || escapeHtml(status || "");
}
