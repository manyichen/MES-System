/*
 * 答辩定位：登录认证与会话 模块的 AuthorizationPolicyTest。
 * 分层职责：自动化回归测试：固定关键业务规则、接口契约和架构边界，防止重构时出现静默回归。
 * 典型调用链：Maven Surefire -> JUnit 5 -> 被测类；测试替身用于隔离远程数据库或文件系统。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 固化当前启用及明确停用的接口权限契约。 */
class AuthorizationPolicyTest {
    /**
     * 回归场景：验证 activeVueContractsRemainProtected 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @Test
    void activeVueContractsRemainProtected() {
        assertFalse(AuthorizationPolicy.requiredPermissions("GET", "dashboard/my-summary").isEmpty());
        assertFalse(AuthorizationPolicy.requiredPermissions("PUT", "equipment/12/status").isEmpty());
        assertFalse(AuthorizationPolicy.requiredPermissions("GET", "warehouses/locations").isEmpty());
        assertTrue(AuthorizationPolicy.requiredPermissions("POST", "inventory/external-purchase")
                .contains("warehouse.inventory.adjust"));
        assertTrue(AuthorizationPolicy.requiredPermissions("PUT", "products/12")
                .contains("master.manage"));
        assertTrue(AuthorizationPolicy.requiredPermissions("DELETE", "product-boms/12")
                .contains("master.manage"));
        assertTrue(AuthorizationPolicy.requiredPermissions("PUT", "production-lines/12")
                .contains("master.manage"));
        assertTrue(AuthorizationPolicy.requiredPermissions("GET", "planning/reworks")
                .contains("planning.rework.read"));
        assertTrue(AuthorizationPolicy.requiredPermissions("POST", "planning/reworks/12/tasks")
                .contains("planning.rework.plan"));
        assertTrue(AuthorizationPolicy.requiredPermissions("POST", "access/account-applications/12/review")
                .contains("permission.review"));
        assertTrue(AuthorizationPolicy.requiredPermissions("POST", "access/system-maintenance/users/12/restore")
                .contains("system.health.read"));
    }

    /**
     * 回归场景：验证 retiredDuplicateContractsRemainUnavailable 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @Test
    void retiredDuplicateContractsRemainUnavailable() {
        assertTrue(AuthorizationPolicy.requiredPermissions("GET", "warehouse-locations").isEmpty());
        assertTrue(AuthorizationPolicy.requiredPermissions("POST", "equipment/12/status").isEmpty());
        assertTrue(AuthorizationPolicy.requiredPermissions("POST", "production-tasks/12/release").isEmpty());
        assertTrue(AuthorizationPolicy.requiredPermissions("POST", "work-orders/12/reject").isEmpty());
        assertTrue(AuthorizationPolicy.requiredPermissions("GET", "dashboard/summary").isEmpty());
    }
}
