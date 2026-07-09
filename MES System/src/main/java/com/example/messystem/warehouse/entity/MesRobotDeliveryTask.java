package com.example.messystem.warehouse.entity;

import java.time.LocalDateTime;

public class MesRobotDeliveryTask {
    public Long deliveryTaskId;
    public String deliveryTaskNo;
    public Long pickingTaskId;
    public Long robotId;
    public Long fromLocationId;
    public Long toLineId;
    public String deliveryStatus;
    public LocalDateTime loadTime;
    public LocalDateTime handoverTime;
}
