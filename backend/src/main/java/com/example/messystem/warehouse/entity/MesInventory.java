package com.example.messystem.warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MesInventory {
    public Long inventoryId;
    public Long materialId;
    public Long warehouseId;
    public Long locationId;
    public String batchNo;
    public BigDecimal availableQty;
    public BigDecimal reservedQty;
    public BigDecimal frozenQty;
    public String qualityStatus;
    public LocalDateTime lastCheckTime;
}
