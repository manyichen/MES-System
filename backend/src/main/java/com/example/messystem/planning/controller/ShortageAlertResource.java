/*
 * 答辩定位：订单、计划、齐套与工单 模块的 ShortageAlertResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.planning.service.KittingService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

/** 承载 /shortage-alerts 缺料预警接口契约的 JAX-RS 控制器。 */
@Path("/shortage-alerts")
@Produces(MediaType.APPLICATION_JSON)
public class ShortageAlertResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final KittingService service = new KittingService();

    /**
     * 接口：GET /api/shortage-alerts。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public Response list() { return ResourceSupport.ok(service.listAlerts()); }

    /**
     * 接口：POST /api/shortage-alerts/{id}/accept。
     * 用例：受理业务事项；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/accept")
    public Response accept(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            Long userId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.action("缺料预警已接收，请完成实际备料后通知 PMC 复核", service.acceptShortageAlert(id, userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
