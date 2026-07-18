/*
 * 答辩定位：登录认证与会话 模块的 AuthFilter。
 * 分层职责：安全边界：在业务方法执行前完成身份、权限或数据范围判断，避免只依赖前端隐藏按钮。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.auth;

import com.example.messystem.auth.service.AuthService;

import com.example.messystem.common.ApiResponse;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Set;

/**
 * 全局 JAX-RS 认证授权过滤器，在 Resource 方法之前执行。
 * <p>顺序：识别公开接口 -> 解析 Bearer token -> AuthService 查询有效会话 ->
 * AuthorizationPolicy 匹配权限 -> 把 AuthenticatedUser 放入请求上下文。</p>
 * 未配置权限的接口采用默认拒绝策略，防止新增 Resource 后忘记加权限而意外公开。
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {
    /** Resource 从 ContainerRequestContext 取得已认证用户时使用的属性键。 */
    public static final String CURRENT_USER_PROPERTY = AuthFilter.class.getName() + ".currentUser";
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final AuthService authService = new AuthService();

    /**
     * 过滤一次 HTTP 请求。OPTIONS、登录和公开追溯放行；其余接口必须同时通过身份与权限校验。
     * 身份失败返回 401，权限不足或未配置返回 403，响应始终是统一 JSON 而非容器 HTML 错误页。
     */
    @Override
    public void filter(ContainerRequestContext context) {
        String path = normalize(context.getUriInfo().getPath());
        String method = context.getMethod().toUpperCase();
        if ("OPTIONS".equals(method) || "auth/login".equals(path) || path.startsWith("public/tire-traces/")) {
            return;
        }

        String token = bearerToken(context.getHeaderString(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            abort(context, Response.Status.UNAUTHORIZED, "请先登录，或登录状态已失效");
            return;
        }

        AuthenticatedUser currentUser = authService.authenticate(token);
        if (currentUser == null) {
            abort(context, Response.Status.UNAUTHORIZED, "登录状态已过期，请重新登录");
            return;
        }
        context.setProperty(CURRENT_USER_PROPERTY, currentUser);

        if (path.equals("auth/me") || path.equals("auth/logout") || path.equals("profile")) {
            return;
        }

        Set<String> required = AuthorizationPolicy.requiredPermissions(method, path);
        if (required.isEmpty()) {
            abort(context, Response.Status.FORBIDDEN, "该接口尚未配置权限，系统已默认拒绝访问");
            return;
        }
        boolean granted = required.stream().anyMatch(currentUser::hasPermission);
        if (!granted) {
            abort(context, Response.Status.FORBIDDEN, "权限不足，需要权限：" + String.join(" 或 ", required));
        }
    }

    /**
     * 公共能力：执行 currentUser 对应的业务步骤。
     * 由 AuthFilter 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static AuthenticatedUser currentUser(ContainerRequestContext context) {
        return (AuthenticatedUser) context.getProperty(CURRENT_USER_PROPERTY);
    }

    /** 从 Authorization 头提取 Bearer 后的原始令牌；前缀大小写不敏感，空令牌视为未登录。 */
    public static String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    /**
     * 内部实现步骤：规范化输入并补齐默认值。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static String normalize(String path) {
        return path == null ? "" : path.replaceFirst("^/+", "").replaceFirst("/+$", "");
    }

    /**
     * 内部实现步骤：执行 abort 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static void abort(ContainerRequestContext context, Response.Status status, String message) {
        context.abortWith(Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(ApiResponse.fail(message))
                .build());
    }
}
