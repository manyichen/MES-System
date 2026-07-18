/*
 * 答辩定位：订单、计划、齐套与工单 模块的 KittingService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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

/**
 * 订单、计划、齐套与工单 的 KittingService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class KittingService {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final ProductionTaskService taskService = new ProductionTaskService();
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final PlanningDao dao = new PlanningDao();

    /**
     * 业务用例：执行 analyze 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesKittingAnalysis> listAnalyses() {
        return database(dao::listAnalyses);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesShortageAlert> listAlerts() {
        return database(dao::listAlerts);
    }

    /**
     * 业务用例：执行 publishShortageAlerts 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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

    /**
     * 业务用例：受理业务事项。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesShortageAlert acceptShortageAlert(long alertId, long userId) {
        return database(() -> dao.acceptShortageAlert(alertId, userId));
    }

    /**
     * 业务用例：执行 nvl 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 业务用例：执行 database 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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
