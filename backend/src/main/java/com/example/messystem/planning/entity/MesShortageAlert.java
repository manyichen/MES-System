package com.example.messystem.planning.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MesShortageAlert {
    public Long alertId;
    public String alertNo;
    public Long taskId;
    public Long analysisId;
    public Long materialId;
    public String materialCode;
    public String materialName;
    public BigDecimal requiredQty;
    public BigDecimal availableQty;
    public BigDecimal shortageQty;
    public String alertLevel;
    public String alertStatus;
    public Long acceptedBy;
    public LocalDateTime acceptedAt;
    public LocalDateTime resolvedAt;
    public LocalDateTime createdAt;
}
