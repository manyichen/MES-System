package com.example.messystem.warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MesInventory {
    public Long inventoryId;
    public Long materialId;
    public String materialCode;
    public String materialName;
    public String materialType;
    public String specification;
    public String unit;
    public Long warehouseId;
    public String warehouseCode;
    public String warehouseName;
    public Long locationId;
    public String locationCode;
    public String locationName;
    public String batchNo;
    public BigDecimal availableQty;
    public BigDecimal warehouseMaterialAvailableQty;
    public BigDecimal reservedQty;
    public BigDecimal frozenQty;
    public String qualityStatus;
    public LocalDateTime lastCheckTime;
}
