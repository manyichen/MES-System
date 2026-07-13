(function () {
    window.loadFeedback = async function (workOrderId = 1, options = {}) {
        try {
            const rows = await getJson(`/management-feedback?workOrderId=${workOrderId}`);
            renderTable("feedback-table", rows, [
                { key: "feedbackId", label: "ID" },
                { key: "feedbackNo", label: "编号" },
                { key: "workOrderId", label: "工单" },
                { key: "feedbackType", label: "类型" },
                { key: "feedbackContent", label: "异常/原因", render: row => multiline(row.feedbackContent) },
                { key: "decisionAction", label: "改进措施", render: row => multiline(row.decisionAction) },
                { key: "feedbackStatus", label: "状态", render: row => feedbackStatusText(row.feedbackStatus) }
            ], [
                { name: "close-feedback", label: "关闭", idKey: "feedbackId", permission: "feedback.close", visible: row => row.feedbackStatus === "OPEN", handler: closeFeedback }
            ]);
            if (options.notify) showMessage("管理反馈已刷新", "ok");
        } catch (error) {
            showMessage(toChineseError(error), "error");
        }
    };

    const originalBindDashboardEvents = window.bindDashboardEvents;

    window.bindDashboardEvents = function () {
        if (typeof originalBindDashboardEvents === "function") {
            originalBindDashboardEvents();
        }
        prepareProcessFeedbackForm();
        bindProcessFeedbackSubmit();
    };

    function prepareProcessFeedbackForm() {
        const form = document.getElementById("feedback-form");
        if (!form || !hasRole("PROCESS_ENGINEER") || form.dataset.processMode === "1") return;
        form.dataset.processMode = "1";
        const title = form.querySelector("h3");
        if (title) title.textContent = "工艺原因分析";

        const type = form.elements.feedbackType;
        if (type) {
            type.innerHTML = `
                <option value="PROCESS_ANALYSIS">工艺原因分析</option>
                <option value="QUALITY">质量问题</option>
                <option value="EQUIPMENT">设备问题</option>
                <option value="MATERIAL">物料问题</option>`;
        }

        const content = form.elements.feedbackContent;
        if (content) {
            const label = content.closest("label");
            if (label) label.style.display = "none";
            content.required = false;
        }

        const submit = form.querySelector("button[type='submit']");
        if (!submit) return;
        submit.insertAdjacentHTML("beforebegin", `
            <label>异常现象 <textarea name="processIssue" rows="3" required placeholder="例：X光检测发现帘线角度波动偏高"></textarea></label>
            <label>工艺原因分析 <textarea name="rootCause" rows="3" required placeholder="例：压延张力参数波动，成型定位复核不足"></textarea></label>
            <label>改进措施 <textarea name="decisionAction" rows="3" required placeholder="例：调整压延张力上下限，对同批次追加复检"></textarea></label>
            <label>验证要求 <input name="verificationPlan" placeholder="例：连续3批RFV与X光项目达标后关闭"></label>`);
    }

    function bindProcessFeedbackSubmit() {
        const form = document.getElementById("feedback-form");
        if (!form || form.dataset.processSubmitBound === "1") return;
        form.dataset.processSubmitBound = "1";
        form.addEventListener("submit", async event => {
            if (!hasRole("PROCESS_ENGINEER")) return;
            event.preventDefault();
            event.stopImmediatePropagation();
            try {
                const payload = buildProcessFeedbackPayload(form);
                await postJson("/management-feedback", payload);
                showMessage("工艺原因分析已提交", "ok");
                form.reset();
                await loadFeedback(payload.workOrderId || 1);
                if (typeof loadDashboard === "function") await loadDashboard();
            } catch (error) {
                showMessage(toChineseError(error), "error");
            }
        }, true);
    }

    function buildProcessFeedbackPayload(form) {
        const values = formToObject(form);
        const issue = values.processIssue || "";
        const rootCause = values.rootCause || "";
        const action = values.decisionAction || "";
        const verification = values.verificationPlan || "";
        return {
            feedbackNo: values.feedbackNo,
            orderId: Number(values.orderId || 0),
            taskId: Number(values.taskId || 0),
            workOrderId: Number(values.workOrderId || 0),
            feedbackType: "PROCESS_ANALYSIS",
            feedbackContent: [`异常现象：${issue}`, `原因分析：${rootCause}`].join("\n"),
            decisionAction: [`改进措施：${action}`, verification ? `验证要求：${verification}` : ""].filter(Boolean).join("\n"),
            feedbackStatus: "OPEN",
            createdAt: nowIsoLocal(),
            closedAt: null
        };
    }

    function multiline(value) {
        if (!value) return "";
        return `<span class="multiline-cell">${escapeHtml(value)}</span>`;
    }
})();
