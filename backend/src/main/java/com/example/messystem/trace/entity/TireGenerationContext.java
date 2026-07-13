package com.example.messystem.trace.entity;

import java.time.LocalDateTime;

public record TireGenerationContext(
        long workOrderId,
        String workOrderNo,
        long inspectionId,
        String inspectionNo,
        Long workReportId,
        long productId,
        String productCode,
        String productName,
        String productModel,
        String productionLine,
        long warehouseId,
        String warehouseName,
        Long locationId,
        String locationName,
        String batchNo,
        LocalDateTime qualifiedAt,
        int qualifiedQuantity,
        int generatedQuantity
) {
}
