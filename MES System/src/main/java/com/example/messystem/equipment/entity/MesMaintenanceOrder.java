package com.example.messystem.equipment.entity;

import java.time.LocalDateTime;

public record MesMaintenanceOrder(
        Long maintenanceOrderId,
        String maintenanceOrderNo,
        Long repairReportId,
        Long equipmentId,
        Long maintainerId,
        String maintenanceStatus,
        LocalDateTime dispatchTime,
        LocalDateTime finishTime,
        String resultDesc
) {
}
