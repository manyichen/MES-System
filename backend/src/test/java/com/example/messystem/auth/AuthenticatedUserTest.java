/*
 * 答辩定位：登录认证与会话 模块的 AuthenticatedUserTest。
 * 分层职责：自动化回归测试：固定关键业务规则、接口契约和架构边界，防止重构时出现静默回归。
 * 典型调用链：Maven Surefire -> JUnit 5 -> 被测类；测试替身用于隔离远程数据库或文件系统。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;

/**
 * 登录认证与会话 的 AuthenticatedUserTest，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
class AuthenticatedUserTest {
    /**
     * 回归场景：验证 superAdminCanUseEveryPermissionAndRoleGatedActionWithoutBecomingThatRole 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
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

    /**
     * 回归场景：验证 normalRoleKeepsExplicitPermissionAndRoleBoundaries 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
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
