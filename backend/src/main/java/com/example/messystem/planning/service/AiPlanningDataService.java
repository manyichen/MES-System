package com.example.messystem.planning.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.master.entity.MesProductBom;
import com.example.messystem.master.entity.MesProcessRoute;
import com.example.messystem.master.entity.MesProductionLine;
import com.example.messystem.planning.dao.PlanningDao;
import com.example.messystem.planning.ai.AiPlanningAdviceRequest;
import com.example.messystem.planning.entity.MesCustomerOrder;
import com.example.messystem.planning.entity.MesKittingAnalysis;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.MesShortageAlert;
import com.example.messystem.planning.entity.MesWorkOrder;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AiPlanningDataService {
    private static final int MAX_TASKS = 20;

    private final PlanningDao dao = new PlanningDao();
    private final WorkOrderService workOrderService = new WorkOrderService();

    public Map<String, Object> buildSnapshot(AiPlanningAdviceRequest request) {
        List<MesProductionTask> allTasks = database(dao::listTasks);
        List<MesProductionTask> selectedTasks = selectTasks(request, allTasks);
        if (selectedTasks.isEmpty()) {
            throw new BadRequestException("没有可用于 AI 排产建议的生产任务");
        }

        List<MesCustomerOrder> orders = database(dao::listOrders);
        Map<Long, MesCustomerOrder> orderById = orders.stream()
                .collect(Collectors.toMap(order -> order.orderId, Function.identity(), (a, b) -> a));
        Set<Long> taskIds = selectedTasks.stream().map(task -> task.taskId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> productIds = selectedTasks.stream()
                .map(task -> task.productId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MesWorkOrder> activeWorkOrders = workOrderService.listWorkOrders().stream()
                .filter(row -> !"COMPLETED".equals(row.workOrderStatus) && !"CANCELLED".equals(row.workOrderStatus))
                .toList();

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("today", LocalDate.now().toString());
        snapshot.put("objective", normalizeObjective(request));
        snapshot.put("horizonDays", normalizeHorizonDays(request));
        snapshot.put("candidateTasks", selectedTasks.stream().map(task -> taskView(task, orderById.get(task.orderId))).toList());
        snapshot.put("productionLines", lineCapacityView(database(dao::listProductionLines), activeWorkOrders));
        snapshot.put("selectedProcessRoute", processRouteView(selectedTasks.get(0).productId));
        snapshot.put("openWorkOrders", activeWorkOrders.stream().map(this::workOrderView).toList());
        snapshot.put("kittingAnalyses", database(dao::listAnalyses).stream()
                .filter(row -> row.taskId != null && taskIds.contains(row.taskId))
                .map(this::analysisView)
                .toList());
        snapshot.put("shortageAlerts", database(dao::listAlerts).stream()
                .filter(row -> row.taskId != null && taskIds.contains(row.taskId))
                .map(this::alertView)
                .toList());
        snapshot.put("materialDemand", materialDemand(selectedTasks, productIds));
        return snapshot;
    }

    public Set<Long> validTaskIds(Map<String, Object> snapshot) {
        return ((List<?>) snapshot.getOrDefault("candidateTasks", List.of())).stream()
                .filter(Map.class::isInstance)
                .map(row -> ((Map<?, ?>) row).get("taskId"))
                .filter(Number.class::isInstance)
                .map(value -> ((Number) value).longValue())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Long> validLineIds(Map<String, Object> snapshot) {
        return ((List<?>) snapshot.getOrDefault("productionLines", List.of())).stream()
                .map(row -> {
                    if (row instanceof Map<?, ?> map) return map.get("lineId");
                    try { return row.getClass().getField("lineId").get(row); } catch (ReflectiveOperationException ex) { return null; }
                })
                .filter(Number.class::isInstance)
                .map(value -> ((Number) value).longValue())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<MesProductionTask> selectTasks(AiPlanningAdviceRequest request, List<MesProductionTask> allTasks) {
        if (request != null && request.taskIds != null && !request.taskIds.isEmpty()) {
            Set<Long> ids = request.taskIds.stream()
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (ids.size() > MAX_TASKS) {
                throw new BadRequestException("AI 排产建议一次最多分析 20 个生产任务");
            }
            List<MesProductionTask> selected = allTasks.stream()
                    .filter(task -> ids.contains(task.taskId))
                    .sorted(Comparator.comparing(task -> new ArrayList<>(ids).indexOf(task.taskId)))
                    .toList();
            if (selected.size() != ids.size()) {
                throw new BadRequestException("部分生产任务不存在，请刷新后重试");
            }
            if (selected.size() != 1) {
                throw new BadRequestException("制定生产工单时一次只能分析一个生产任务");
            }
            MesProductionTask task = selected.get(0);
            if (!"READY".equals(task.kittingStatus) || !"READY".equals(task.taskStatus)) {
                throw new BadRequestException("请先完成并通过齐套分析，再使用 AI 排产辅助");
            }
            return selected;
        }
        throw new BadRequestException("请在制定生产工单时选择一个已齐套的生产任务");
    }

    private int statusRank(String status) {
        if ("SHORTAGE".equals(status)) return 2;
        if ("READY".equals(status)) return 0;
        return 1;
    }

    private Map<String, Object> taskView(MesProductionTask task, MesCustomerOrder order) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("taskId", task.taskId);
        row.put("taskNo", task.taskNo);
        row.put("orderId", task.orderId);
        row.put("productId", task.productId);
        row.put("planQty", task.planQty);
        row.put("targetLineId", task.targetLineId);
        row.put("taskStatus", task.taskStatus);
        row.put("kittingStatus", task.kittingStatus);
        row.put("plannedStartTime", stringValue(task.plannedStartTime));
        row.put("plannedEndTime", stringValue(task.plannedEndTime));
        row.put("orderNo", order == null ? null : order.orderNo);
        row.put("customerName", order == null ? null : order.customerName);
        row.put("deliveryDate", order == null ? null : stringValue(order.deliveryDate));
        row.put("priorityLevel", order == null ? null : order.priorityLevel);
        row.put("orderStatus", order == null ? null : order.orderStatus);
        return row;
    }

    private Map<String, Object> workOrderView(MesWorkOrder workOrder) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("workOrderId", workOrder.workOrderId);
        row.put("workOrderNo", workOrder.workOrderNo);
        row.put("taskId", workOrder.taskId);
        row.put("lineId", workOrder.lineId);
        row.put("processId", workOrder.processId);
        row.put("plannedQty", workOrder.plannedQty);
        row.put("actualQty", workOrder.actualQty);
        row.put("workOrderStatus", workOrder.workOrderStatus);
        return row;
    }

    private List<Map<String, Object>> lineCapacityView(List<MesProductionLine> lines, List<MesWorkOrder> activeWorkOrders) {
        return lines.stream().map(line -> {
            int activeCount = (int) activeWorkOrders.stream().filter(workOrder -> line.lineId.equals(workOrder.lineId)).count();
            int pendingQty = activeWorkOrders.stream().filter(workOrder -> line.lineId.equals(workOrder.lineId))
                    .mapToInt(workOrder -> Math.max(0, (workOrder.plannedQty == null ? 0 : workOrder.plannedQty) - (workOrder.actualQty == null ? 0 : workOrder.actualQty))).sum();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lineId", line.lineId); row.put("lineCode", line.lineCode); row.put("lineName", line.lineName);
            row.put("lineType", line.lineType); row.put("lineStatus", line.lineStatus); row.put("enabled", line.enabled);
            row.put("dailyCapacity", line.capacityPerDay); row.put("activeWorkOrderCount", activeCount); row.put("pendingQty", pendingQty);
            row.put("schedulable", line.enabled != null && line.enabled == 1 && !"FAULT".equals(line.lineStatus) && !"DISABLED".equals(line.lineStatus));
            return row;
        }).toList();
    }

    private List<Map<String, Object>> processRouteView(Long productId) {
        return database(dao::listProcessRoutes).stream().filter(route -> productId != null && productId.equals(route.productId))
                .sorted(Comparator.comparing(route -> route.processSeq == null ? 0 : route.processSeq)).map(route -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("sequence", route.processSeq); row.put("processCode", route.processCode); row.put("processName", route.processName);
                    row.put("requiredEquipmentType", route.workCenter); return row;
                }).toList();
    }

    private Map<String, Object> analysisView(MesKittingAnalysis analysis) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("analysisId", analysis.analysisId);
        row.put("analysisNo", analysis.analysisNo);
        row.put("taskId", analysis.taskId);
        row.put("kittingStatus", analysis.kittingStatus);
        row.put("analysisTime", stringValue(analysis.analysisTime));
        return row;
    }

    private Map<String, Object> alertView(MesShortageAlert alert) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("alertId", alert.alertId);
        row.put("alertNo", alert.alertNo);
        row.put("taskId", alert.taskId);
        row.put("alertLevel", alert.alertLevel);
        row.put("alertStatus", alert.alertStatus);
        row.put("createdAt", stringValue(alert.createdAt));
        return row;
    }

    private List<Map<String, Object>> materialDemand(List<MesProductionTask> tasks, Set<Long> productIds) {
        Map<Long, List<MesProductBom>> bomByProduct = new LinkedHashMap<>();
        for (Long productId : productIds) {
            bomByProduct.put(productId, database(() -> dao.listBomForProduct(productId)));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (MesProductionTask task : tasks) {
            List<MesProductBom> bomItems = bomByProduct.getOrDefault(task.productId, List.of());
            for (MesProductBom bom : bomItems) {
                BigDecimal required = nvl(bom.qtyPerUnit).multiply(BigDecimal.valueOf(task.planQty == null ? 0 : task.planQty));
                BigDecimal available = database(() -> dao.availableQty(bom.materialId));
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("taskId", task.taskId);
                row.put("materialId", bom.materialId);
                row.put("materialCode", bom.materialCode);
                row.put("materialName", bom.materialName);
                row.put("unit", bom.unit);
                row.put("usageQtyPerUnit", bom.qtyPerUnit);
                row.put("requiredQty", required);
                row.put("availableQty", available);
                row.put("shortageQty", available.compareTo(required) < 0 ? required.subtract(available) : BigDecimal.ZERO);
                rows.add(row);
            }
        }
        return rows;
    }

    private static String normalizeObjective(AiPlanningAdviceRequest request) {
        if (request == null || request.objective == null || request.objective.isBlank()) {
            return "优先满足交期，在不突破齐套、库存、产线状态等硬约束的前提下给出任务排序和风险建议";
        }
        return request.objective.trim();
    }

    private static int normalizeHorizonDays(AiPlanningAdviceRequest request) {
        if (request == null || request.horizonDays == null || request.horizonDays <= 0) return 7;
        return Math.min(request.horizonDays, 30);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static <T> T database(SqlCall<T> call) {
        try {
            return call.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T execute() throws SQLException;
    }
}
