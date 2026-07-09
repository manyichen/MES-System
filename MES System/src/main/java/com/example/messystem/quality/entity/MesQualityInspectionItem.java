package com.example.messystem.quality.entity;

public record MesQualityInspectionItem(
        Long inspectionItemId,
        Long inspectionId,
        String itemCode,
        String itemName,
        String standardValue,
        String actualValue,
        String itemResult,
        String remark
) {
}
