package com.example.messystem.quality.entity;

import java.time.LocalDateTime;

public record MesQualityTrace(
        Long traceId,
        String traceNo,
        Long orderId,
        Long taskId,
        Long workOrderId,
        String batchNo,
        Long inspectionId,
        Long reworkOrderId,
        String traceStatus,
        LocalDateTime createdAt
) {
}
