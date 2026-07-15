package com.example.messystem.warehouse.entity;

import java.math.BigDecimal;

public class ExternalPurchaseRequest {
    public Long materialId;
    public Long warehouseId;
    public Long locationId;
    public String batchNo;
    public BigDecimal qty;
    public String reason;
}
