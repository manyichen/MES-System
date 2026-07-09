async function refreshProduction() {
    const [reports, wages] = await Promise.all([
        getJson("/work-reports"),
        getJson("/piecework-wages")
    ]);
    renderTable("reportTable", reports, [
        { title: "ID", key: "reportId" },
        { title: "编号", key: "reportNo" },
        { title: "工单", key: "workOrderId" },
        { title: "合格", key: "qualifiedQty" },
        { title: "状态", key: "reportStatus" },
        { title: "操作", render: row => `<button onclick="approveReport(${row.reportId})">审核</button>` }
    ]);
    renderTable("wageTable", wages, [
        { title: "ID", key: "wageId" },
        { title: "报工", key: "reportId" },
        { title: "单价", key: "pieceRate" },
        { title: "金额", key: "wageAmount" },
        { title: "状态", key: "settlementStatus" }
    ]);
}

async function approveReport(id) {
    try {
        await postJson(`/work-reports/${id}/approve`);
        showMessage("报工已审核，计件工资已生成");
        await refreshProduction();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

document.getElementById("refreshProduction").addEventListener("click", () => refreshProduction().catch(error => showMessage(error.message, "error")));
document.getElementById("reportForm").addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.target);
    try {
        await postJson("/work-reports", {
            workOrderId: Number(form.get("workOrderId")),
            operatorId: Number(form.get("operatorId")),
            reportQty: Number(form.get("reportQty")),
            qualifiedQty: Number(form.get("qualifiedQty")),
            defectQty: Number(form.get("defectQty")),
            workHours: Number(form.get("workHours"))
        });
        showMessage("报工已提交");
        await refreshProduction();
    } catch (error) {
        showMessage(error.message, "error");
    }
});

refreshProduction().catch(() => {});
