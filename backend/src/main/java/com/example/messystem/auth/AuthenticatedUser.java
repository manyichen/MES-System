/*
 * 答辩定位：登录认证与会话 模块的 AuthenticatedUser。
 * 分层职责：公共支撑代码：提供多个业务模块共享的响应、异常、编码或工具能力。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.auth;

import com.example.messystem.master.entity.MesUser;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 登录认证与会话 的 AuthenticatedUser，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class AuthenticatedUser {
    /** SUPER_ADMIN_ROLE 业务字段；具体取值由创建/更新用例校验后写入。 */
    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    /** user 业务字段；具体取值由创建/更新用例校验后写入。 */
    public MesUser user;
    /** 当前用户拥有的角色编码集合。 */
    public Set<String> roles = new LinkedHashSet<>();
    /** 由角色展开得到的权限点编码集合。 */
    public Set<String> permissions = new LinkedHashSet<>();
    /** 当前用户被授权访问的生产线主键集合。 */
    public Set<Long> lineIds = new LinkedHashSet<>();
    /** 当前用户被授权访问的仓库主键集合。 */
    public Set<Long> warehouseIds = new LinkedHashSet<>();
    /** 会话或业务对象的失效时间。 */
    public LocalDateTime expiresAt;

    /**
     * 公共能力：校验访问令牌并还原当前用户。
     * 由 AuthenticatedUser 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public AuthenticatedUser() {
    }

    /**
     * 公共能力：校验访问令牌并还原当前用户。
     * 由 AuthenticatedUser 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public AuthenticatedUser(MesUser user, Set<String> roles, Set<String> permissions,
            Set<Long> lineIds, Set<Long> warehouseIds, LocalDateTime expiresAt) {
        this.user = user;
        this.roles = roles;
        this.permissions = permissions;
        this.lineIds = lineIds;
        this.warehouseIds = warehouseIds;
        this.expiresAt = expiresAt;
    }

    /**
     * 公共能力：执行 hasRole 对应的业务步骤。
     * 由 AuthenticatedUser 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public boolean hasRole(String roleCode) {
        return roles.contains(roleCode);
    }

    /**
     * 公共能力：执行 isSuperAdmin 对应的业务步骤。
     * 由 AuthenticatedUser 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
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

    /**
     * 公共能力：执行 hasPermission 对应的业务步骤。
     * 由 AuthenticatedUser 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public boolean hasPermission(String permissionCode) {
        return isSuperAdmin() || permissions.contains(permissionCode);
    }
}
