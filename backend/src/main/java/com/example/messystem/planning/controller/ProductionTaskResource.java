/*
 * 答辩定位：订单、计划、齐套与工单 模块的 ProductionTaskResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.service.KittingService;
import com.example.messystem.planning.service.ProductionTaskService;
import com.example.messystem.security.service.DataScopeService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

/** 承载 /production-tasks 生产任务接口契约的 JAX-RS 控制器。 */
@Path("/production-tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductionTaskResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final ProductionTaskService service = new ProductionTaskService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final KittingService kittingService = new KittingService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final DataScopeService dataScopeService = new DataScopeService();

    /**
     * 接口：GET /api/production-tasks。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public Response list(@Context ContainerRequestContext context) {
        var scope = dataScopeService.snapshot(AuthFilter.currentUser(context));
        return ResourceSupport.ok(service.listTasks().stream().filter(scope::canView).toList());
    }

    /**
     * 接口：POST /api/production-tasks。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    public Response create(MesProductionTask task, @Context ContainerRequestContext context) {
        try {
            task.plannerId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.created("生产任务已创建", service.createTask(task));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/production-tasks/{id}/kitting。
     * 用例：执行 analyzeKitting 对应的业务步骤；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/kitting")
    public Response analyzeKitting(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            dataScopeService.snapshot(AuthFilter.currentUser(context)).requireTask(id);
            return ResourceSupport.action("齐套分析已完成", kittingService.analyze(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/production-tasks/{id}/shortage-alerts。
     * 用例：执行 publishShortageAlerts 对应的业务步骤；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/shortage-alerts")
    public Response publishShortageAlerts(@PathParam("id") long id) {
        try {
            return ResourceSupport.action("缺料预警已发布给仓储人员", kittingService.publishShortageAlerts(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：GET /api/production-tasks/kitting-analyses。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/kitting-analyses")
    public Response listAnalyses() {
        return ResourceSupport.ok(kittingService.listAnalyses());
    }

    /**
     * 接口：GET /api/production-tasks/shortage-alerts。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/shortage-alerts")
    public Response listAlerts() {
        return ResourceSupport.ok(kittingService.listAlerts());
    }
}
