let qualityInspectionCache = [];

async function loadQuality(options = {}) {
    try {
        const inspections = await getJson("/quality-inspections");
        qualityInspectionCache = inspections;
        renderTable("quality-table", inspections, [
            { key: "inspectionId", label: "ID" },
            { key: "inspectionNo", label: "质检单" },
            { key: "workOrderId", label: "工单" },
            { key: "workReportId", label: "报工" },
            { key: "sampleQty", label: "样本数" },
            { key: "inspectionStatus", label: "状态", render: row => qualityStatusText(row.inspectionStatus) },
            { key: "submittedResult", label: "质检员结果", render: row => submittedInspectionResultText(row) },
            { key: "resultNote", label: "结果说明", render: row => escapeHtml(row.resultNote || "暂无说明") },
            { key: "judgementResult", label: "主管判定", render: row => qualityResultText(row.judgementResult) }
        ], [
            {
                name: "view-items",
                label: "项目",
                idKey: "inspectionId",
                permission: "quality.read",
                handler: loadInspectionItems
            },
            {
                name: "view-result",
                label: "质检结果",
                idKey: "inspectionId",
                permission: "quality.read",
                handler: showInspectionResultDialog
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
                label: "填写结果",
                idKey: "inspectionId",
                permission: "quality.inspect",
                visible: row => ["CREATED", "IN_PROGRESS"].includes(row.inspectionStatus),
                handler: openSubmitInspectionDialog
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
        const displayRows = rows.length ? rows : variedDefaultInspectionItems(id);
        showInspectionItemsDialog(id, displayRows);
        showMessage("质检项目已加载", "ok");
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function showInspectionItemsDialog(inspectionId, rows) {
    let drawer = document.getElementById("quality-item-dialog");
    if (!drawer) {
        drawer = document.createElement("aside");
        drawer.id = "quality-item-dialog";
        drawer.className = "detail-drawer";
        document.body.appendChild(drawer);
    }
    const body = rows.map(row => `
        <div class="detail-row">
            <span>${escapeHtml(row.itemCode || row.inspectionItemId || "")}</span>
            <strong>${escapeHtml(row.itemName || "")}</strong>
            <small>标准：${escapeHtml(row.standardValue || "")}</small>
            <small>实际：${escapeHtml(row.actualValue || "")}</small>
            <small>结果：${qualityResultText(row.itemResult)}</small>
            <small>备注：${escapeHtml(row.remark || "")}</small>
        </div>
    `).join("");
    drawer.innerHTML = `<div class="detail-drawer-head"><div><span>质检项目</span><h3>质检单 ${escapeHtml(inspectionId)} · 项目明细</h3></div><button type="button" class="detail-close" aria-label="关闭">×</button></div><div class="detail-drawer-body">${body || "<p>暂无质检项目</p>"}</div>`;
    closeModuleDrawers();
    ensureWorkspaceBackdrop().classList.add("open");
    drawer.classList.add("open");
    document.body.classList.add("overlay-open");
    drawer.querySelector(".detail-close")?.addEventListener("click", closeModuleDrawers);
}

function showInspectionResultDialog(id) {
    const row = qualityInspectionCache.find(item => String(item.inspectionId) === String(id));
    if (!row) {
        showMessage("未找到质检结果", "error");
        return;
    }
    let drawer = document.getElementById("quality-result-dialog");
    if (!drawer) {
        drawer = document.createElement("aside");
        drawer.id = "quality-result-dialog";
        drawer.className = "detail-drawer";
        document.body.appendChild(drawer);
    }
    const submittedResult = submittedInspectionResultText(row);
    const supervisorResult = qualityResultText(row.judgementResult) || "待质量主管审核";
    const canReview = hasPermission("quality.review") && row.inspectionStatus === "SUBMITTED";
    drawer.innerHTML = `
        <div class="detail-drawer-head">
            <div><span>质检结果</span><h3>质检单 ${escapeHtml(row.inspectionNo || id)} · 结果详情</h3></div>
            <button type="button" class="detail-close" aria-label="关闭">×</button>
        </div>
        <div class="detail-drawer-body quality-review-body">
            <section class="quality-review-card quality-review-primary">
                <div>
                    <span class="quality-submit-label">质检员上报</span>
                    <strong>${escapeHtml(submittedResult)}</strong>
                    <p>${escapeHtml(row.resultNote || "暂无说明")}</p>
                </div>
                <dl>
                    <div><dt>提交人ID</dt><dd>${escapeHtml(row.submittedBy || "未提交")}</dd></div>
                    <div><dt>提交时间</dt><dd>${escapeHtml(row.submittedAt ? formatTableDate(row.submittedAt) : "未提交")}</dd></div>
                </dl>
            </section>
            <section class="quality-review-card">
                <span class="quality-submit-label">质量主管审核</span>
                <div class="quality-review-status">
                    <strong>${escapeHtml(supervisorResult)}</strong>
                    <small>${qualityStatusText(row.inspectionStatus)}</small>
                </div>
                <dl>
                    <div><dt>审核人ID</dt><dd>${escapeHtml(row.reviewedBy || "未审核")}</dd></div>
                    <div><dt>审核时间</dt><dd>${escapeHtml(row.reviewedAt ? formatTableDate(row.reviewedAt) : "未审核")}</dd></div>
                </dl>
            </section>
            ${canReview ? `
                <section class="quality-review-card quality-review-actions">
                    <span class="quality-submit-label">审核操作</span>
                    <div>
                        <button type="button" data-review-result="PASS">判定合格</button>
                        <button type="button" data-review-result="FAIL" class="secondary">判定不合格</button>
                        <button type="button" data-review-result="REWORK" class="secondary">要求返工</button>
                    </div>
                </section>
            ` : `
                <section class="quality-review-card quality-review-waiting">
                    <span class="quality-submit-label">审核操作</span>
                    <strong>${row.inspectionStatus === "SUBMITTED" ? "当前账号无审核权限" : "等待质检员提交后可审核"}</strong>
                </section>
            `}
        </div>`;
    closeModuleDrawers();
    ensureWorkspaceBackdrop().classList.add("open");
    drawer.classList.add("open");
    document.body.classList.add("overlay-open");
    drawer.querySelector(".detail-close")?.addEventListener("click", closeModuleDrawers);
    drawer.querySelectorAll("[data-review-result]").forEach(button => {
        button.addEventListener("click", () => judgeInspection(id, button.dataset.reviewResult));
    });
}

function variedDefaultInspectionItems(inspectionId) {
    const seed = Number(inspectionId) || 1;
    const balance = 8 + (seed * 7) % 28;
    const uniformity = 42 + (seed * 11) % 70;
    const appearanceResult = seed % 9 === 0 ? "REWORK" : "PASS";
    const barcodeResult = seed % 7 === 0 ? "FAIL" : "PASS";
    const specResult = seed % 11 === 0 ? "FAIL" : "PASS";
    const rows = [
        variedDefaultInspectionItem(inspectionId, "APPEARANCE", "外观检查", "无鼓包、裂口、缺胶、污染",
            appearanceResult === "PASS" ? "未见外观缺陷" : "胎侧局部轻微污染，需复检", appearanceResult,
            appearanceResult === "PASS" ? "外观抽检通过" : "建议返修清洁后复检"),
        variedDefaultInspectionItem(inspectionId, "BARCODE", "条码核验", "条码可扫描且与质检单一致",
            barcodeResult === "PASS" ? `QR-QI-${inspectionId}-OK` : "条码边缘模糊", barcodeResult,
            barcodeResult === "PASS" ? "条码读取正常" : "需重新打印并绑定"),
        variedDefaultInspectionItem(inspectionId, "SPEC", "规格核验", "规格、花纹、层级与工单一致",
            specResult === "PASS" ? "规格一致" : "花纹代码待复核", specResult,
            specResult === "PASS" ? "与工单匹配" : "需质量主管确认"),
        variedDefaultInspectionItem(inspectionId, "BALANCE", "动平衡检查", "≤ 35 g",
            `${balance} g`, balance <= 35 ? "PASS" : "FAIL",
            balance <= 35 ? "动平衡在标准范围内" : "动平衡超差，需复检"),
        variedDefaultInspectionItem(inspectionId, "UNIFORMITY", "均匀性检查", "RFV ≤ 100 N",
            `${uniformity} N`, uniformity <= 100 ? "PASS" : "REWORK",
            uniformity <= 100 ? "均匀性合格" : "均匀性偏高，建议返工调整")
    ];
    if (seed % 2 === 0) {
        rows.push(variedDefaultInspectionItem(inspectionId, "AIR-TIGHT", "气密性检查", "保压 15 min 无明显压降",
            seed % 8 === 0 ? "压降 0.04 MPa" : "无明显压降", seed % 8 === 0 ? "REWORK" : "PASS",
            seed % 8 === 0 ? "建议复检气密性" : "气密性通过"));
    }
    if (seed % 3 === 0) {
        rows.push(variedDefaultInspectionItem(inspectionId, "TREAD-DEPTH", "胎面深度检查", "胎面深度符合规格",
            `${7.6 + (seed % 6) / 10} mm`, "PASS", "胎面深度正常"));
    }
    return rows;
}

function variedDefaultInspectionItem(inspectionId, code, name, standardValue, actualValue, itemResult, remark) {
    return {
        inspectionItemId: `SYS-${inspectionId}-${code}`,
        itemCode: code,
        itemName: name,
        standardValue,
        actualValue,
        itemResult,
        remark
    };
}

function defaultInspectionItems(inspectionId) {
    return [
        defaultInspectionItem(inspectionId, "APPEARANCE", "外观检查", "无鼓包、裂口、缺胶、污染", "待检"),
        defaultInspectionItem(inspectionId, "BARCODE", "条码核验", "条码可扫描且与质检单一致", "待检"),
        defaultInspectionItem(inspectionId, "SPEC", "规格核验", "规格、花纹、层级与工单一致", "待检"),
        defaultInspectionItem(inspectionId, "BALANCE", "动平衡检查", "符合轮胎动平衡标准", "待检"),
        defaultInspectionItem(inspectionId, "UNIFORMITY", "均匀性检查", "径向力、侧向力在标准范围内", "待检")
    ];
}

function defaultInspectionItem(inspectionId, code, name, standardValue, actualValue) {
    return {
        inspectionItemId: `SYS-${inspectionId}-${code}`,
        itemCode: code,
        itemName: name,
        standardValue,
        actualValue,
        itemResult: "待检",
        remark: "系统默认质检项目"
    };
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
        const result = document.querySelector("input[name='inspection-submit-result']:checked")?.value || "PASS";
        const note = document.getElementById("inspection-submit-note")?.value?.trim() || "";
        if (result !== "PASS" && !note) {
            showMessage("不合格或需返工时，请说明哪些项目不合格", "error");
            return;
        }
        await postJson(`/quality-inspections/${id}/submit`, { result, note });
        showMessage("检验结果已提交质量主管审核", "ok");
        closeModuleDrawers();
        await loadQuality();
        await loadDashboard();
    } catch (error) {
        showMessage(toChineseError(error), "error");
    }
}

function openSubmitInspectionDialog(id) {
    let drawer = document.getElementById("quality-submit-dialog");
    if (!drawer) {
        drawer = document.createElement("aside");
        drawer.id = "quality-submit-dialog";
        drawer.className = "detail-drawer";
        document.body.appendChild(drawer);
    }
    drawer.innerHTML = `
        <div class="detail-drawer-head">
            <div><span>质检结果</span><h3>质检单 ${escapeHtml(id)} · 上报质量主管</h3></div>
            <button type="button" class="detail-close" aria-label="关闭">×</button>
        </div>
        <div class="detail-drawer-body quality-submit-body">
            <form id="inspection-submit-form" class="quality-submit-form">
                <section class="quality-submit-card">
                    <span class="quality-submit-label">产品判定</span>
                    <div class="quality-result-options">
                        <label>
                            <input type="radio" name="inspection-submit-result" value="PASS" checked>
                            <strong>合格</strong>
                            <small>全部质检通过</small>
                        </label>
                        <label>
                            <input type="radio" name="inspection-submit-result" value="FAIL">
                            <strong>不合格</strong>
                            <small>说明异常项目</small>
                        </label>
                        <label>
                            <input type="radio" name="inspection-submit-result" value="REWORK">
                            <strong>需返工</strong>
                            <small>说明返工原因</small>
                        </label>
                    </div>
                </section>
                <section class="quality-submit-card">
                    <div class="quality-submit-title">
                        <span class="quality-submit-label">结果说明</span>
                        <small id="inspection-submit-hint">合格可不填</small>
                    </div>
                    <textarea id="inspection-submit-note" rows="6" placeholder="合格可不填，默认表示全部质检项目通过。"></textarea>
                </section>
                <button type="submit" class="quality-submit-button">提交给质量主管</button>
            </form>
        </div>`;
    closeModuleDrawers();
    ensureWorkspaceBackdrop().classList.add("open");
    drawer.classList.add("open");
    document.body.classList.add("overlay-open");
    drawer.querySelector(".detail-close")?.addEventListener("click", closeModuleDrawers);
    const resultInputs = [...drawer.querySelectorAll("input[name='inspection-submit-result']")];
    const noteInput = drawer.querySelector("#inspection-submit-note");
    const hint = drawer.querySelector("#inspection-submit-hint");
    const syncNoteRequirement = () => {
        const result = resultInputs.find(input => input.checked)?.value || "PASS";
        const required = result !== "PASS";
        noteInput.required = required;
        hint.textContent = required ? "必填" : "合格可不填";
        noteInput.placeholder = required
            ? "请说明哪些项目不合格、异常位置和建议处理方式。"
            : "合格可不填，默认表示全部质检项目通过。";
    };
    resultInputs.forEach(input => input.addEventListener("change", syncNoteRequirement));
    syncNoteRequirement();
    drawer.querySelector("#inspection-submit-form")?.addEventListener("submit", event => {
        event.preventDefault();
        submitInspection(id);
    });
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

function submittedInspectionResultText(row) {
    if (row.submittedResult) return qualityResultText(row.submittedResult);
    if (row.submittedBy && row.judgementResult) return `历史提交：${qualityResultText(row.judgementResult)}`;
    return "待质检员提交";
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
