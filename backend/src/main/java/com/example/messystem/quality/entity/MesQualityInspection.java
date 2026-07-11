package com.example.messystem.quality.entity;

import java.time.LocalDateTime;

public record MesQualityInspection(
        Long inspectionId,
        String inspectionNo,
        Long workOrderId,
        Long workReportId,
        Integer sampleQty,
        String inspectionStatus,
        Long inspectorId,
        Long assignedTo,
        LocalDateTime inspectionTime,
        String judgementResult,
        Long submittedBy,
        LocalDateTime submittedAt,
        Long reviewedBy,
        LocalDateTime reviewedAt
) {
}
