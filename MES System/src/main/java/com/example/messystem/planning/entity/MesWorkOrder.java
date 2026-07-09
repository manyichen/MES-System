package com.example.messystem.planning.entity;

import java.time.LocalDateTime;

public class MesWorkOrder {
    public Long workOrderId;
    public String workOrderNo;
    public Long taskId;
    public Long productId;
    public Long lineId;
    public Long processId;
    public Integer plannedQty;
    public Integer actualQty;
    public Integer priorityLevel;
    public String workOrderStatus;
    public String batchNo;
    public LocalDateTime dispatchTime;
    public LocalDateTime receiveTime;
    public LocalDateTime completedTime;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
