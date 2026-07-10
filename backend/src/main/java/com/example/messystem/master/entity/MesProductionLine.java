package com.example.messystem.master.entity;

import java.time.LocalDateTime;

public class MesProductionLine {
    public Long lineId;
    public String lineCode;
    public String lineName;
    public String lineType;
    public Integer capacityPerDay;
    public String lineStatus;
    public Integer enabled;
    public LocalDateTime createdAt;
}
