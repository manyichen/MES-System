/*
 * 答辩定位：订单、计划、齐套与工单 模块的 ReworkPlanningDemand。
 * 分层职责：领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。
 * 典型调用链：PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.entity;

import java.time.LocalDateTime;

/** 质量返工需求在计划模块中的只读投影。 */
public record ReworkPlanningDemand(
        long reworkOrderId,
        String reworkOrderNo,
        long sourceWorkOrderId,
        String reworkReason,
        String reworkStatus,
        Long assignedLineId,
        int sourcePlannedQty,
        long orderId,
        String orderNo,
        long productId,
        Long plannedTaskId,
        String plannedTaskNo,
        LocalDateTime createdAt) {
}
