package com.example.messystem.warehouse.entity;

import java.time.LocalDateTime;

public class MesPickingTask {
    public Long pickingTaskId;
    public String pickingTaskNo;
    public Long requisitionId;
    public Long warehouseId;
    public String taskStatus;
    public Long assignedTo;
    public LocalDateTime startTime;
    public LocalDateTime finishTime;
}
