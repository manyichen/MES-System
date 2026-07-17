package com.example.messystem.warehouse.entity;

import java.time.LocalDateTime;

public class MesMaterial {
    public Long materialId;
    public String materialCode;
    public String materialName;
    public String materialType;
    public String specification;
    public String unit;
    public Integer shelfLifeDays;
    public Integer enabled;
    public String defaultWarehouseType;
    public Long defaultWarehouseId;
    public String defaultWarehouseCode;
    public String defaultWarehouseName;
    public LocalDateTime createdAt;
}
