/*
 * 答辩定位：登录认证与会话 模块的 LoginSession。
 * 分层职责：公共支撑代码：提供多个业务模块共享的响应、异常、编码或工具能力。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.auth;

import com.example.messystem.master.entity.MesUser;

/**
 * 登录认证与会话 的 LoginSession，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class LoginSession {
    /** 登录后返回给客户端的原始访问令牌。 */
    public String token;
    /** user 业务字段；具体取值由创建/更新用例校验后写入。 */
    public MesUser user;
    /** 当前用户拥有的角色编码集合。 */
    public java.util.Set<String> roles;
    /** 由角色展开得到的权限点编码集合。 */
    public java.util.Set<String> permissions;
    /** 当前用户被授权访问的生产线主键集合。 */
    public java.util.Set<Long> lineIds;
    /** 当前用户被授权访问的仓库主键集合。 */
    public java.util.Set<Long> warehouseIds;
    /** 会话或业务对象的失效时间。 */
    public java.time.LocalDateTime expiresAt;

    /**
     * 公共能力：校验账号密码并创建会话。
     * 由 LoginSession 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public LoginSession() {
    }

    /**
     * 公共能力：校验账号密码并创建会话。
     * 由 LoginSession 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
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
