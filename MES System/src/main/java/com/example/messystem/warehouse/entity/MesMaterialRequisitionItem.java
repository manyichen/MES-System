package com.example.messystem.warehouse.entity;

import java.math.BigDecimal;

public class MesMaterialRequisitionItem {
    public Long requisitionItemId;
    public Long requisitionId;
    public Long materialId;
    public BigDecimal requiredQty;
    public BigDecimal issuedQty;
    public String unit;
    public String batchNo;
    public String itemStatus;
}
