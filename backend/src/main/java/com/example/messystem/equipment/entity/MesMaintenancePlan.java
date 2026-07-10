package com.example.messystem.equipment.entity;

import java.time.LocalDateTime;

public record MesMaintenancePlan(
        Long maintenancePlanId,
        Long equipmentId,
        String planCycle,
        LocalDateTime nextPlanTime,
        String planStatus,
        LocalDateTime createdAt
) {
}
