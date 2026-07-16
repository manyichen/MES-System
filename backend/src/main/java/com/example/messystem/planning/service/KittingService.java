package com.example.messystem.planning.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.master.entity.MesProductBom;
import com.example.messystem.planning.dao.PlanningDao;
import com.example.messystem.planning.entity.MesKittingAnalysis;
import com.example.messystem.planning.entity.MesKittingShortageItem;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.entity.MesShortageAlert;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class KittingService {
    private final ProductionTaskService taskService = new ProductionTaskService();
    private final PlanningDao dao = new PlanningDao();

    public MesKittingAnalysis analyze(long taskId) {
        MesProductionTask task = taskService.getTask(taskId);
        if (task.productId == null) {
            throw new BadRequestException("task productId is required");
        }
        List<MesProductBom> bomItems = database(() -> dao.listBomForProduct(task.productId));
        if (bomItems.isEmpty()) {
            throw new BadRequestException("product bom is required before kitting analysis");
        }
        MesKittingAnalysis analysis = new MesKittingAnalysis();
        analysis.analysisNo = IdGenerator.nextCode("KIT");
        analysis.taskId = task.taskId;
        analysis.productId = task.productId;
        analysis.planQty = task.planQty;
        analysis.analysisTime = LocalDateTime.now();

        List<MesKittingShortageItem> shortages = new ArrayList<>();
        for (MesProductBom bom : bomItems) {
            BigDecimal requiredQty = nvl(bom.qtyPerUnit).multiply(BigDecimal.valueOf(task.planQty == null ? 0 : task.planQty));
            BigDecimal availableQty = database(() -> dao.availableQty(bom.materialId));
            if (availableQty.compareTo(requiredQty) < 0) {
                MesKittingShortageItem item = new MesKittingShortageItem();
                item.taskId = task.taskId;
                item.materialId = bom.materialId;
                item.materialCode = bom.materialCode;
                item.materialName = bom.materialName;
                item.requiredQty = requiredQty;
                item.availableQty = availableQty;
                item.shortageQty = requiredQty.subtract(availableQty);
                item.itemStatus = "OPEN";
                shortages.add(item);
            }
        }
        analysis.shortageItems = shortages;
        analysis.kittingStatus = shortages.isEmpty() ? "READY" : "SHORTAGE";
        analysis.analysisResult = shortages.isEmpty() ? "物料齐套" : "物料短缺";
        MesKittingAnalysis created = database(() -> dao.insertAnalysis(analysis, shortages));
        database(() -> {
            dao.updateTaskKitting(task.taskId, shortages.isEmpty() ? "READY" : "SHORTAGE", created.kittingStatus);
            dao.resolveRecoveredShortageAlerts(task.taskId);
            return null;
        });
        return created;
    }

    public List<MesKittingAnalysis> listAnalyses() {
        return database(dao::listAnalyses);
    }

    public List<MesShortageAlert> listAlerts() {
        return database(dao::listAlerts);
    }

    public List<MesShortageAlert> publishShortageAlerts(long taskId) {
        MesProductionTask task = taskService.getTask(taskId);
        if (!"SHORTAGE".equals(task.kittingStatus)) {
            throw new BadRequestException("当前任务不存在待发布的物料缺口");
        }
        return database(() -> dao.publishShortageAlerts(taskId));
    }

    public MesShortageAlert acceptShortageAlert(long alertId, long userId) {
        return database(() -> dao.acceptShortageAlert(alertId, userId));
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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
