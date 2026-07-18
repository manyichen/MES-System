/*
 * 答辩定位：生产报工与计件工资 模块的 WorkReportResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.production.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.production.entity.MesWorkReport;
import com.example.messystem.production.service.ProductionService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/** 承载 /work-reports 生产报工接口契约的 JAX-RS 控制器。 */
@Path("/work-reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkReportResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final ProductionService service = new ProductionService();

    /**
     * 接口：GET /api/work-reports。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public Response list(@Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        return ResourceSupport.ok(user.hasRole("PRODUCTION_OPERATOR")
                ? service.listWorkReportsByOperator(user.user.userId)
                : service.listWorkReports());
    }

    /**
     * 接口：GET /api/work-reports/by-work-order/{workOrderId}。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/by-work-order/{workOrderId}")
    public Response listByWorkOrder(@PathParam("workOrderId") long workOrderId,
            @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            return ResourceSupport.ok(user.hasRole("PRODUCTION_OPERATOR")
                    ? service.listWorkReportsByWorkOrderAndOperator(workOrderId, user.user.userId)
                    : service.listWorkReportsByWorkOrder(workOrderId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：GET /api/work-reports/{id}。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            MesWorkReport report = service.getWorkReport(id);
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (user.hasRole("PRODUCTION_OPERATOR") && !user.user.userId.equals(report.operatorId)) {
                throw new BadRequestException("只能查看本人的报工记录");
            }
            return ResourceSupport.ok(report);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/work-reports。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    public Response create(MesWorkReport report, @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            report.operatorId = user.user.userId;
            return ResourceSupport.created("报工单已提交",
                    service.createWorkReport(report, user.isSuperAdmin()));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：PUT /api/work-reports/{id}。
     * 用例：更新业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") long id, MesWorkReport report, @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            MesWorkReport current = service.getWorkReport(id);
            if ((!user.isSuperAdmin() && !user.user.userId.equals(current.operatorId))
                    || (!"SUBMITTED".equals(current.reportStatus) && !"REJECTED".equals(current.reportStatus))) {
                throw new BadRequestException("只能修改本人待审核或已驳回的报工单");
            }
            report.operatorId = user.user.userId;
            report.reportStatus = "SUBMITTED";
            return ResourceSupport.action("报工单已更新",
                    service.updateWorkReport(id, report, user.isSuperAdmin()));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：DELETE /api/work-reports/{id}。
     * 用例：删除业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        try {
            service.deleteWorkReport(id);
            return ResourceSupport.action("报工单已删除", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/work-reports/{id}/approve。
     * 用例：审核通过业务事项；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/approve")
    public Response approve(@PathParam("id") long id) {
        try {
            return ResourceSupport.action("报工单已审核通过", service.approveWorkReport(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/work-reports/{id}/reject。
     * 用例：驳回业务事项；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/reject")
    public Response reject(@PathParam("id") long id, Map<String, String> request) {
        try {
            String reason = request == null ? "" : request.getOrDefault("reason", "").trim();
            if (reason.isEmpty()) {
                throw new BadRequestException("驳回理由不能为空");
            }
            return ResourceSupport.action("报工单已驳回", service.rejectWorkReport(id, reason));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
