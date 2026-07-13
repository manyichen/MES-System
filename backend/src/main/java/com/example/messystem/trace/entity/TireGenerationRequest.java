package com.example.messystem.trace.entity;

public record TireGenerationRequest(
        Long workOrderId,
        Long inspectionId,
        Long warehouseId,
        Long locationId,
        Integer quantity,
        String publicBaseUrl
) {
}
