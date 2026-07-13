package com.example.messystem.trace.entity;

import java.time.LocalDateTime;

public record TireTraceItem(
        Long tireId,
        String serialNo,
        String traceCode,
        Long workOrderId,
        String workOrderNo,
        Long inspectionId,
        String inspectionNo,
        Long workReportId,
        Long productId,
        String productCode,
        String productName,
        String productModel,
        String productionLine,
        Long warehouseId,
        String warehouseName,
        Long locationId,
        String locationName,
        String batchNo,
        String tireStatus,
        LocalDateTime qualifiedAt,
        LocalDateTime inboundAt,
        String accessToken,
        String targetUrl,
        Integer printCount
) {
}
