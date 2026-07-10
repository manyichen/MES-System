package com.example.messystem.master.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MesProductBom {
    public Long bomId;
    public Long productId;
    public Long materialId;
    public String materialCode;
    public String materialName;
    public BigDecimal qtyPerUnit;
    public String unit;
    public Integer enabled;
    public LocalDateTime createdAt;
}
