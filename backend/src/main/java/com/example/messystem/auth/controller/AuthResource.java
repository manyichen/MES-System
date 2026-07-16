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
    private final AuthService service = new AuthService();

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

    @GET
    @Path("/me")
    public Response me(@Context ContainerRequestContext context) {
        return ResourceSupport.ok(AuthFilter.currentUser(context));
    }

    @POST
    @Path("/logout")
    public Response logout(@Context ContainerRequestContext context,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        try {
            AuthenticatedUser currentUser = AuthFilter.currentUser(context);
            service.logout(AuthFilter.bearerToken(authorization), currentUser);
            return ResourceSupport.action("已退出登录", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
