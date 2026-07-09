package com.example.messystem.planning.entity;

import java.time.LocalDateTime;

public class MesWorkOrderOperationLog {
    public Long logId;
    public Long workOrderId;
    public String operationType;
    public Long operatorId;
    public String fromStatus;
    public String toStatus;
    public String remark;
    public LocalDateTime operatedAt;
}
