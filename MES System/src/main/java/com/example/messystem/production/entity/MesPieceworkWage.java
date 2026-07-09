package com.example.messystem.production.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MesPieceworkWage {
    public Long wageId;
    public Long reportId;
    public Long operatorId;
    public BigDecimal pieceRate;
    public Integer qualifiedQty;
    public BigDecimal wageAmount;
    public String settlementStatus;
    public LocalDateTime createdAt;
}
