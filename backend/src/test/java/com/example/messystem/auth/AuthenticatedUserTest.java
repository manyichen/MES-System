package com.example.messystem.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;

class AuthenticatedUserTest {
    @Test
    void superAdminCanUseEveryPermissionAndRoleGatedActionWithoutBecomingThatRole() {
        AuthenticatedUser user = new AuthenticatedUser();
        user.roles = new LinkedHashSet<>();
        user.roles.add(AuthenticatedUser.SUPER_ADMIN_ROLE);

        assertTrue(user.isSuperAdmin());
        assertTrue(user.hasPermission("any.future.permission"));
        assertTrue(user.canActAs("GENERAL_MANAGER"));
        assertTrue(user.canActAs("PRODUCTION_OPERATOR"));
        assertFalse(user.hasRole("PRODUCTION_OPERATOR"));
    }

    @Test
    void normalRoleKeepsExplicitPermissionAndRoleBoundaries() {
        AuthenticatedUser user = new AuthenticatedUser();
        user.roles = new LinkedHashSet<>();
        user.roles.add("HR_MANAGER");
        user.permissions = new LinkedHashSet<>();
        user.permissions.add("permission.apply");

        assertTrue(user.canActAs("HR_MANAGER"));
        assertTrue(user.hasPermission("permission.apply"));
        assertFalse(user.canActAs("WAREHOUSE_ADMIN"));
        assertFalse(user.hasPermission("warehouse.read"));
    }
}
