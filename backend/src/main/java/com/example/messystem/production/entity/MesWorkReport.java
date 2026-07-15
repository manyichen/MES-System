package com.example.messystem.production.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MesWorkReport {
    public Long reportId;
    public String reportNo;
    public Long workOrderId;
    public String batchNo;
    public Long operatorId;
    public Integer reportQty;
    public Integer qualifiedQty;
    public Integer defectQty;
    public BigDecimal workHours;
    public LocalDateTime reportTime;
    public String reportStatus;
    public String remark;
    public String rejectReason;
}
