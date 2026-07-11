package com.example.messystem.auth;

import com.example.messystem.master.entity.MesUser;

public class LoginSession {
    public String token;
    public MesUser user;
    public java.util.Set<String> roles;
    public java.util.Set<String> permissions;
    public java.util.Set<Long> lineIds;
    public java.util.Set<Long> warehouseIds;
    public java.time.LocalDateTime expiresAt;

    public LoginSession() {
    }

    public LoginSession(String token, AuthenticatedUser currentUser) {
        this.token = token;
        this.user = currentUser.user;
        this.roles = currentUser.roles;
        this.permissions = currentUser.permissions;
        this.lineIds = currentUser.lineIds;
        this.warehouseIds = currentUser.warehouseIds;
        this.expiresAt = currentUser.expiresAt;
    }
}
