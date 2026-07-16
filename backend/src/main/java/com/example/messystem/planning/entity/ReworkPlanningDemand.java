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
