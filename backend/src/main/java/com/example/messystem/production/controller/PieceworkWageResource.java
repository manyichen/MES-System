/*
 * 答辩定位：生产报工与计件工资 模块的 PieceworkWageResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.production.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.production.service.ProductionService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** 承载 /piecework-wages 计件工资接口契约的 JAX-RS 控制器。 */
@Path("/piecework-wages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PieceworkWageResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final ProductionService service = new ProductionService();

    /**
     * 接口：GET /api/piecework-wages。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public Response list(@Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        if (user.hasPermission("production.wage.read_all")) return ResourceSupport.ok(service.listWages());
        if (user.hasPermission("production.wage.read_self")) {
            return ResourceSupport.ok(service.listWagesByOperator(user.user.userId));
        }
        if (user.hasRole("WORKSHOP_MANAGER")) {
            return ResourceSupport.ok(service.wageSummaryForWorkshop(user.user.userId));
        }
        return ResourceSupport.ok(service.wageSummary());
    }

    /**
     * 接口：GET /api/piecework-wages/by-report/{workReportId}。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/by-report/{workReportId}")
    public Response listByReport(@PathParam("workReportId") long workReportId,
            @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (user.hasPermission("production.wage.read_all")) {
                return ResourceSupport.ok(service.listWagesByReport(workReportId));
            }
            if (user.hasPermission("production.wage.read_self")) {
                return ResourceSupport.ok(service.listWagesByReportAndOperator(workReportId, user.user.userId));
            }
            throw new BadRequestException("汇总查看权限不能下钻到个人计件工资明细");
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：GET /api/piecework-wages/{id}。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            var wage = service.getWage(id);
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (!user.hasPermission("production.wage.read_all")
                    && (!user.hasPermission("production.wage.read_self") || !user.user.userId.equals(wage.operatorId))) {
                throw new BadRequestException("无权查看该员工的计件工资明细");
            }
            return ResourceSupport.ok(wage);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
