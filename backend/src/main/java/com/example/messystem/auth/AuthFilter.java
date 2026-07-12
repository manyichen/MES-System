package com.example.messystem.auth;

import com.example.messystem.common.ApiResponse;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Set;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {
    public static final String CURRENT_USER_PROPERTY = AuthFilter.class.getName() + ".currentUser";
    private final AuthService authService = new AuthService();

    @Override
    public void filter(ContainerRequestContext context) {
        String path = normalize(context.getUriInfo().getPath());
        String method = context.getMethod().toUpperCase();
        if ("OPTIONS".equals(method) || "auth/login".equals(path)) {
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
        boolean granted = required.stream().anyMatch(currentUser.permissions::contains);
        if (!granted) {
            abort(context, Response.Status.FORBIDDEN, "权限不足，需要权限：" + String.join(" 或 ", required));
        }
    }

    public static AuthenticatedUser currentUser(ContainerRequestContext context) {
        return (AuthenticatedUser) context.getProperty(CURRENT_USER_PROPERTY);
    }

    public static String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private static String normalize(String path) {
        return path == null ? "" : path.replaceFirst("^/+", "").replaceFirst("/+$", "");
    }

    private static void abort(ContainerRequestContext context, Response.Status status, String message) {
        context.abortWith(Response.status(status).entity(ApiResponse.fail(message)).build());
    }
}
