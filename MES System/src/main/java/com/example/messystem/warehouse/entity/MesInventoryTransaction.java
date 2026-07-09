package com.example.messystem.warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MesInventoryTransaction {
    public Long transactionId;
    public String transactionNo;
    public Long materialId;
    public Long inventoryId;
    public String transactionType;
    public BigDecimal qty;
    public String sourceDocType;
    public Long sourceDocId;
    public Long operatorId;
    public LocalDateTime createdAt;
}
