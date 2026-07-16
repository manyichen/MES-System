package com.example.messystem.auth;

import com.example.messystem.master.entity.MesUser;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public class AuthenticatedUser {
    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    public MesUser user;
    public Set<String> roles = new LinkedHashSet<>();
    public Set<String> permissions = new LinkedHashSet<>();
    public Set<Long> lineIds = new LinkedHashSet<>();
    public Set<Long> warehouseIds = new LinkedHashSet<>();
    public LocalDateTime expiresAt;

    public AuthenticatedUser() {
    }

    public AuthenticatedUser(MesUser user, Set<String> roles, Set<String> permissions,
            Set<Long> lineIds, Set<Long> warehouseIds, LocalDateTime expiresAt) {
        this.user = user;
        this.roles = roles;
        this.permissions = permissions;
        this.lineIds = lineIds;
        this.warehouseIds = warehouseIds;
        this.expiresAt = expiresAt;
    }

    public boolean hasRole(String roleCode) {
        return roles.contains(roleCode);
    }

    public boolean isSuperAdmin() {
        return hasRole(SUPER_ADMIN_ROLE);
    }

    /**
     * Allows a super administrator through role-specific action gates without treating the
     * account as a restricted front-line role in list and data-scope branches.
     */
    public boolean canActAs(String roleCode) {
        return isSuperAdmin() || hasRole(roleCode);
    }

    public boolean hasPermission(String permissionCode) {
        return isSuperAdmin() || permissions.contains(permissionCode);
    }
}
