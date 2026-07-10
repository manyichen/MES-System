package com.example.messystem.planning.entity;

import java.time.LocalDateTime;

public class MesProductionTask {
    public Long taskId;
    public String taskNo;
    public Long orderId;
    public Long productId;
    public Long plannerId;
    public Integer planQty;
    public LocalDateTime plannedStartTime;
    public LocalDateTime plannedEndTime;
    public Long targetLineId;
    public String taskStatus;
    public String kittingStatus;
    public LocalDateTime releaseTime;
    public LocalDateTime closeTime;
    public String remark;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
