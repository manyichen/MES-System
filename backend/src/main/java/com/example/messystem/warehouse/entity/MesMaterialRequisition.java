package com.example.messystem.warehouse.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MesMaterialRequisition {
    public Long requisitionId;
    public String requisitionNo;
    public Long workOrderId;
    public Long requestedBy;
    public String requestStatus;
    public LocalDateTime requestTime;
    public Long approvedBy;
    public LocalDateTime approvedTime;
    public String remark;
    public List<MesMaterialRequisitionItem> items = new ArrayList<>();
}
