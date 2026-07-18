/*
 * 答辩定位：登录认证与会话 模块的 ProfileResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.auth.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.service.ProfileService;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.master.entity.MesUser;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** 承载 /profile 个人资料接口契约的 JAX-RS 控制器。 */
@Path("/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final ProfileService service = new ProfileService();

    /**
     * 接口：GET /api/profile。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public Response get(@Context ContainerRequestContext context) {
        try {
            return ResourceSupport.ok(service.get(AuthFilter.currentUser(context).user.userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：PUT /api/profile。
     * 用例：更新业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @PUT
    public Response update(MesUser profile, @Context ContainerRequestContext context) {
        try {
            return ResourceSupport.action("个人资料已更新",
                    service.update(AuthFilter.currentUser(context).user.userId, profile));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
