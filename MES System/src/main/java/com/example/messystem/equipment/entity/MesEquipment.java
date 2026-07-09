package com.example.messystem.equipment.entity;

import java.time.LocalDateTime;

public record MesEquipment(
        Long equipmentId,
        String equipmentCode,
        String equipmentName,
        String equipmentType,
        Long lineId,
        String equipmentStatus,
        LocalDateTime lastMaintenanceTime,
        Boolean enabled
) {
}
