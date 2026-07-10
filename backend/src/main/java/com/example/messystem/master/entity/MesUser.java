package com.example.messystem.master.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class MesUser {
    public Long userId;
    public String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String password;
    public String realName;
    public String roleCode;
    public String department;
    public String phone;
    public Integer enabled;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime lastLoginAt;
}
