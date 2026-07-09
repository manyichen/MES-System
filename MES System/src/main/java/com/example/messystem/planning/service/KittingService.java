package com.example.messystem.planning.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.master.entity.MesProductBom;
import com.example.messystem.planning.entity.MesKittingAnalysis;
import com.example.messystem.planning.entity.MesKittingShortageItem;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.MesShortageAlert;
import com.example.messystem.warehouse.entity.MesInventory;
import com.example.messystem.warehouse.service.WarehouseService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class KittingService {
    private final ProductionTaskService taskService = new ProductionTaskService();
    private static final WarehouseService warehouseService = new WarehouseService();

    public MesKittingAnalysis analyze(long taskId) {
        MesProductionTask task = taskService.getTask(taskId);
        if (task.productId == null) {
            throw new BadRequestException("task productId is required");
        }
        List<MesProductBom> bomItems = PlanningStore.productBoms.values().stream()
                .filter(item -> item.productId != null && item.productId.equals(task.productId))
                .toList();
        if (bomItems.isEmpty()) {
            throw new BadRequestException("product bom is required before kitting analysis");
        }
        MesKittingAnalysis analysis = new MesKittingAnalysis();
        analysis.analysisId = PlanningStore.nextId();
        analysis.analysisNo = IdGenerator.nextCode("KIT");
        analysis.taskId = task.taskId;
        analysis.productId = task.productId;
        analysis.planQty = task.planQty;
        analysis.analysisTime = LocalDateTime.now();

        List<MesKittingShortageItem> shortages = new ArrayList<>();
        for (MesProductBom bom : bomItems) {
            BigDecimal requiredQty = nvl(bom.qtyPerUnit).multiply(BigDecimal.valueOf(task.planQty == null ? 0 : task.planQty));
            BigDecimal availableQty = availableQty(bom.materialId);
            if (availableQty.compareTo(requiredQty) < 0) {
                MesKittingShortageItem item = new MesKittingShortageItem();
                item.shortageItemId = PlanningStore.nextId();
                item.analysisId = analysis.analysisId;
                item.taskId = task.taskId;
                item.materialId = bom.materialId;
                item.materialCode = bom.materialCode;
                item.materialName = bom.materialName;
                item.requiredQty = requiredQty;
                item.availableQty = availableQty;
                item.shortageQty = requiredQty.subtract(availableQty);
                item.itemStatus = "OPEN";
                PlanningStore.shortageItems.put(item.shortageItemId, item);
                shortages.add(item);
                createAlert(task.taskId, item.materialId, item.shortageQty);
            }
        }
        analysis.shortageItems = shortages;
        analysis.kittingStatus = shortages.isEmpty() ? "READY" : "SHORTAGE";
        analysis.analysisResult = shortages.isEmpty() ? "materials ready" : "materials shortage";
        PlanningStore.analyses.put(analysis.analysisId, analysis);
        task.kittingStatus = analysis.kittingStatus;
        task.taskStatus = shortages.isEmpty() ? "READY" : "SHORTAGE";
        task.updatedAt = LocalDateTime.now();
        return analysis;
    }

    public List<MesKittingAnalysis> listAnalyses() {
        return new ArrayList<>(PlanningStore.analyses.values());
    }

    public List<MesShortageAlert> listAlerts() {
        return new ArrayList<>(PlanningStore.shortageAlerts.values());
    }

    private static BigDecimal availableQty(Long materialId) {
        return warehouseService.listInventory().stream()
                .filter(item -> materialId != null && materialId.equals(item.materialId))
                .map(item -> nvl(item.availableQty))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void createAlert(Long taskId, Long materialId, BigDecimal shortageQty) {
        MesShortageAlert alert = new MesShortageAlert();
        alert.alertId = PlanningStore.nextId();
        alert.alertNo = IdGenerator.nextCode("ALERT");
        alert.taskId = taskId;
        alert.materialId = materialId;
        alert.shortageQty = shortageQty;
        alert.alertLevel = shortageQty.compareTo(new BigDecimal("100")) > 0 ? "HIGH" : "NORMAL";
        alert.alertStatus = "OPEN";
        alert.createdAt = LocalDateTime.now();
        PlanningStore.shortageAlerts.put(alert.alertId, alert);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
