let lastWorkOrderId = null;

async function refreshPlanning() {
    const [orders, tasks, workOrders] = await Promise.all([
        getJson("/orders"),
        getJson("/production-tasks"),
        getJson("/work-orders")
    ]);

    renderTable("orderTable", orders, [
        { title: "ID", key: "orderId" },
        { title: "编号", key: "orderNo" },
        { title: "客户", key: "customerName" },
        { title: "产品", key: "productId" },
        { title: "数量", key: "orderQty" },
        { title: "状态", key: "orderStatus" }
    ]);

    renderTable("taskTable", tasks, [
        { title: "ID", key: "taskId" },
        { title: "编号", key: "taskNo" },
        { title: "订单", key: "orderId" },
        { title: "数量", key: "planQty" },
        { title: "齐套", key: "kittingStatus" },
        { title: "状态", key: "taskStatus" },
        { title: "操作", render: row => `
            <button onclick="analyzeTask(${row.taskId})">齐套</button>
            <button onclick="releaseTask(${row.taskId})">发布</button>` }
    ]);

    renderTable("workOrderTable", workOrders, [
        { title: "ID", key: "workOrderId" },
        { title: "编号", key: "workOrderNo" },
        { title: "任务", key: "taskId" },
        { title: "产线", key: "lineId" },
        { title: "工序", key: "processId" },
        { title: "状态", key: "workOrderStatus" },
        { title: "操作", render: row => `
            <button onclick="dispatchWorkOrder(${row.workOrderId})">派发</button>
            <button onclick="receiveWorkOrder(${row.workOrderId})">接收</button>
            <button onclick="loadWorkOrderLogs(${row.workOrderId})">日志</button>` }
    ]);

    if (workOrders.length) {
        lastWorkOrderId = workOrders[workOrders.length - 1].workOrderId;
        await loadWorkOrderLogs(lastWorkOrderId, false);
    }
}

async function seedPlanning() {
    try {
        const [products, lines, routes] = await Promise.all([
            getJson("/products"),
            getJson("/production-lines"),
            getJson("/process-routes")
        ]);
        if (!products.length) {
            throw new Error("请先维护产品主数据");
        }
        if (!lines.length) {
            throw new Error("请先维护产线主数据");
        }
        if (!routes.length) {
            throw new Error("请先维护工艺路线");
        }

        const product = products[0];
        const line = lines[0];
        const route = routes.find(item => item.productId === product.productId) || routes[0];
        const order = await postJson("/orders", {
            customerName: "双星演示客户",
            productId: product.productId,
            orderQty: 100,
            priorityLevel: 1
        });
        const task = await postJson("/production-tasks", {
            orderId: order.orderId,
            plannerId: 1,
            planQty: 100,
            targetLineId: line.lineId
        });
        await postJson(`/production-tasks/${task.taskId}/kitting`);
        await postJson(`/production-tasks/${task.taskId}/release`);
        const workOrder = await postJson("/work-orders", {
            taskId: task.taskId,
            lineId: line.lineId,
            processId: route.processId,
            plannedQty: 100
        });
        lastWorkOrderId = workOrder.workOrderId;
        showMessage("计划主线演示数据已生成");
        await refreshPlanning();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function analyzeTask(id) {
    try {
        await postJson(`/production-tasks/${id}/kitting`);
        showMessage("齐套分析已完成");
        await refreshPlanning();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function releaseTask(id) {
    try {
        await postJson(`/production-tasks/${id}/release`);
        showMessage("生产任务已发布");
        await refreshPlanning();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function dispatchWorkOrder(id) {
    try {
        await postJson(`/work-orders/${id}/dispatch?operatorId=1`);
        showMessage("生产工单已派发");
        await refreshPlanning();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function receiveWorkOrder(id) {
    try {
        await postJson(`/work-orders/${id}/receive?operatorId=2`);
        showMessage("生产工单已接收");
        await refreshPlanning();
    } catch (error) {
        showMessage(error.message, "error");
    }
}

async function loadWorkOrderLogs(id, notify = true) {
    try {
        const logs = await getJson(`/work-orders/${id}/logs`);
        renderTable("workOrderLogTable", logs, [
            { title: "ID", key: "logId" },
            { title: "工单", key: "workOrderId" },
            { title: "操作", key: "operationType" },
            { title: "原状态", key: "fromStatus" },
            { title: "新状态", key: "toStatus" },
            { title: "说明", key: "remark" }
        ]);
        if (notify) {
            showMessage("工单日志已加载");
        }
    } catch (error) {
        showMessage(error.message, "error");
    }
}

document.getElementById("seedPlanning").addEventListener("click", seedPlanning);
document.getElementById("refreshPlanning").addEventListener("click", () => refreshPlanning().catch(error => showMessage(error.message, "error")));
document.getElementById("orderForm").addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.target);
    try {
        await postJson("/orders", {
            customerName: form.get("customerName"),
            productId: Number(form.get("productId")),
            orderQty: Number(form.get("orderQty"))
        });
        showMessage("订单已创建");
        await refreshPlanning();
    } catch (error) {
        showMessage(error.message, "error");
    }
});
document.getElementById("taskForm").addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.target);
    try {
        await postJson("/production-tasks", {
            orderId: Number(form.get("orderId")),
            planQty: Number(form.get("planQty")) || null
        });
        showMessage("生产任务已创建");
        await refreshPlanning();
    } catch (error) {
        showMessage(error.message, "error");
    }
});
document.getElementById("workOrderForm").addEventListener("submit", async event => {
    event.preventDefault();
    const form = new FormData(event.target);
    try {
        await postJson("/work-orders", {
            taskId: Number(form.get("taskId")),
            lineId: Number(form.get("lineId")) || null,
            processId: Number(form.get("processId")) || null
        });
        showMessage("生产工单已创建");
        await refreshPlanning();
    } catch (error) {
        showMessage(error.message, "error");
    }
});

refreshPlanning().catch(error => showMessage(error.message, "error"));
