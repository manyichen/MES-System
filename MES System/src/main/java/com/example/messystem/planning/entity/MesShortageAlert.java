package com.example.messystem.planning.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MesShortageAlert {
    public Long alertId;
    public String alertNo;
    public Long taskId;
    public Long materialId;
    public BigDecimal shortageQty;
    public String alertLevel;
    public String alertStatus;
    public LocalDateTime createdAt;
}
