package com.example.messystem.master.entity;

import java.time.LocalDateTime;

public class MesSyncLog {
    public Long syncLogId;
    public String syncType;
    public String sourceSystem;
    public String targetTable;
    public String syncStatus;
    public String message;
    public LocalDateTime createdAt;
}
