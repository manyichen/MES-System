package com.example.messystem.planning.entity;

import java.time.LocalDateTime;

public class ReworkPlanningRequest {
    public Integer planQty;
    public LocalDateTime plannedStartTime;
    public LocalDateTime plannedEndTime;
    public Long targetLineId;
}
