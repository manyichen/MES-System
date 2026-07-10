package com.example.messystem.dashboard.entity;

import java.time.LocalDateTime;

public record MesProductTrace(
        Long traceId,
        String traceCode,
        Long orderId,
        Long taskId,
        Long workOrderId,
        Long productId,
        String batchNo,
        String traceStatus,
        LocalDateTime createdAt
) {
}
