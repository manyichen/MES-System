package com.example.messystem.equipment.entity;

import java.time.LocalDateTime;

public record MesEquipmentRepairReport(
        Long repairReportId,
        String repairReportNo,
        Long equipmentId,
        Long workOrderId,
        String faultLevel,
        String faultDesc,
        Long reporterId,
        LocalDateTime reportTime,
        String repairStatus
) {
}
