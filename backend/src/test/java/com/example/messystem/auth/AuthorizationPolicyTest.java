package com.example.messystem.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 固化当前启用及明确停用的接口权限契约。 */
class AuthorizationPolicyTest {
    @Test
    void activeVueContractsRemainProtected() {
        assertFalse(AuthorizationPolicy.requiredPermissions("GET", "dashboard/my-summary").isEmpty());
        assertFalse(AuthorizationPolicy.requiredPermissions("PUT", "equipment/12/status").isEmpty());
        assertFalse(AuthorizationPolicy.requiredPermissions("GET", "warehouses/locations").isEmpty());
        assertTrue(AuthorizationPolicy.requiredPermissions("POST", "inventory/external-purchase")
                .contains("warehouse.inventory.adjust"));
        assertTrue(AuthorizationPolicy.requiredPermissions("GET", "planning/reworks")
                .contains("planning.rework.read"));
        assertTrue(AuthorizationPolicy.requiredPermissions("POST", "planning/reworks/12/tasks")
                .contains("planning.rework.plan"));
        assertTrue(AuthorizationPolicy.requiredPermissions("POST", "access/account-applications/12/review")
                .contains("permission.review"));
        assertTrue(AuthorizationPolicy.requiredPermissions("POST", "access/system-maintenance/users/12/restore")
                .contains("system.health.read"));
    }

    @Test
    void retiredDuplicateContractsRemainUnavailable() {
        assertTrue(AuthorizationPolicy.requiredPermissions("GET", "warehouse-locations").isEmpty());
        assertTrue(AuthorizationPolicy.requiredPermissions("POST", "equipment/12/status").isEmpty());
        assertTrue(AuthorizationPolicy.requiredPermissions("POST", "production-tasks/12/release").isEmpty());
        assertTrue(AuthorizationPolicy.requiredPermissions("GET", "dashboard/summary").isEmpty());
    }
}
