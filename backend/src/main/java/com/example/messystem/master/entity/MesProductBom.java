package com.example.messystem.master.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MesProductBom {
    public Long bomId;
    public Long productId;
    public String productCode;
    public String productName;
    public Long materialId;
    public String materialCode;
    public String materialName;
    public BigDecimal qtyPerUnit;
    public String unit;
    public Integer enabled;
    public LocalDateTime createdAt;
}
