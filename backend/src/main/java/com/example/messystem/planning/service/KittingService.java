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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KittingService {
    private final ProductionTaskService taskService = new ProductionTaskService();
    private final PlanningDao dao = new PlanningDao();

    public MesKittingAnalysis analyze(long taskId) {
        MesProductionTask task = taskService.getTask(taskId);
        if (!Boolean.TRUE.equals(task.kittingAnalyzable)) {
            throw new BadRequestException(task.kittingBlockedReason == null
                    ? "当前生产任务不满足齐套分析条件，请检查产品、计划数量和产品BOM"
                    : task.kittingBlockedReason);
        }
        List<MesProductBom> bomItems = database(() -> dao.listBomForProduct(task.productId));
        if (bomItems.isEmpty()) {
            throw new BadRequestException("产品未配置启用的BOM物料，请先维护产品BOM");
        }
        MesKittingAnalysis analysis = new MesKittingAnalysis();
        analysis.analysisNo = IdGenerator.nextCode("KIT");
        analysis.taskId = task.taskId;
        analysis.productId = task.productId;
        analysis.planQty = task.planQty;
        analysis.analysisTime = LocalDateTime.now();

        Map<Long, MesKittingShortageItem> requirements = new LinkedHashMap<>();
        for (MesProductBom bom : bomItems) {
            BigDecimal requiredQty = nvl(bom.qtyPerUnit).multiply(BigDecimal.valueOf(task.planQty == null ? 0 : task.planQty));
            MesKittingShortageItem item = requirements.computeIfAbsent(bom.materialId, ignored -> {
                MesKittingShortageItem requirement = new MesKittingShortageItem();
                requirement.taskId = task.taskId;
                requirement.materialId = bom.materialId;
                requirement.materialCode = bom.materialCode;
                requirement.materialName = bom.materialName;
                requirement.requiredQty = BigDecimal.ZERO;
                return requirement;
            });
            item.requiredQty = item.requiredQty.add(requiredQty);
        }

        List<MesKittingShortageItem> shortages = new ArrayList<>();
        for (MesKittingShortageItem item : requirements.values()) {
            BigDecimal availableQty = database(() -> dao.availableQty(item.materialId));
            if (availableQty.compareTo(item.requiredQty) < 0) {
                item.availableQty = availableQty;
                item.shortageQty = item.requiredQty.subtract(availableQty);
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
        if (database(() -> dao.hasPublishedShortageAlert(taskId))) {
            throw new BadRequestException("该生产任务已经发布过缺料预警，不能重复发布");
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
