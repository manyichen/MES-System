/*
 * 答辩定位：登录认证与会话 模块的 AuthResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.auth.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.service.AuthService;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.auth.LoginRequest;
import com.example.messystem.common.ResourceSupport;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.servlet.http.HttpServletRequest;

/** 承载 /auth 认证接口契约的 JAX-RS 控制器。 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final AuthService service = new AuthService();

    /**
     * 接口：POST /api/auth/login。
     * 用例：校验账号密码并创建会话；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/login")
    public Response login(LoginRequest request, @Context HttpServletRequest servletRequest,
            @HeaderParam("User-Agent") String userAgent) {
        try {
            String ip = servletRequest == null ? null : servletRequest.getRemoteAddr();
            return ResourceSupport.ok(service.login(request, ip, userAgent));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：GET /api/auth/me。
     * 用例：执行 me 对应的业务步骤；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/me")
    public Response me(@Context ContainerRequestContext context) {
        return ResourceSupport.ok(AuthFilter.currentUser(context));
    }

    /**
     * 接口：POST /api/auth/logout。
     * 用例：注销当前会话；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/logout")
    @Consumes(MediaType.WILDCARD)
    public Response logout(@Context ContainerRequestContext context,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        try {
            AuthenticatedUser currentUser = AuthFilter.currentUser(context);
            service.logout(AuthFilter.bearerToken(authorization), currentUser);
            return ResourceSupport.action("已退出登录", true);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
