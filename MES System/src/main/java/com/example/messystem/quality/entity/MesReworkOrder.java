package com.example.messystem.quality.entity;

import java.time.LocalDateTime;

public record MesReworkOrder(
        Long reworkOrderId,
        String reworkOrderNo,
        Long sourceWorkOrderId,
        Long inspectionId,
        String reworkReason,
        String reworkStatus,
        Long assignedLineId,
        LocalDateTime createdAt,
        LocalDateTime closedAt
) {
}
