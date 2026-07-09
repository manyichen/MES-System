package com.example.messystem.planning.entity;

import java.math.BigDecimal;

public class MesKittingShortageItem {
    public Long shortageItemId;
    public Long analysisId;
    public Long taskId;
    public Long materialId;
    public String materialCode;
    public String materialName;
    public BigDecimal requiredQty;
    public BigDecimal availableQty;
    public BigDecimal shortageQty;
    public String itemStatus;
}
