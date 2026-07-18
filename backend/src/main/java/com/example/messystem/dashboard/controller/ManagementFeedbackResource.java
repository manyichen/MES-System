/*
 * 答辩定位：驾驶舱、反馈与产品追溯 模块的 ManagementFeedbackResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.dashboard.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.security.service.DataScopeService;
import com.example.messystem.dashboard.entity.MesManagementFeedback;
import com.example.messystem.dashboard.service.ManagementFeedbackService;
import com.example.messystem.planning.entity.MesWorkOrder;
import com.example.messystem.planning.service.WorkOrderService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

/** 承载 /management-feedback 管理反馈接口契约的 JAX-RS 控制器。 */
@Path("/management-feedback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ManagementFeedbackResource {

    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final ManagementFeedbackService service = new ManagementFeedbackService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final DataScopeService dataScopeService = new DataScopeService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final WorkOrderService workOrderService = new WorkOrderService();

    /**
     * 接口：GET /api/management-feedback/create-options。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/create-options")
    public ApiResponse<List<MesWorkOrder>> createOptions(@Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        rejectProductionOperator(user);
        var scope = dataScopeService.snapshot(user);
        return ApiResponse.ok(workOrderService.listWorkOrders().stream().filter(scope::canView).toList());
    }

    /**
     * 接口：GET /api/management-feedback。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public ApiResponse<List<MesManagementFeedback>> listFeedback(@QueryParam("workOrderId") Long workOrderId,
            @Context ContainerRequestContext context) {
        try {
            if (workOrderId == null) {
                return ApiResponse.fail("workOrderId query parameter is required");
            }
            AuthenticatedUser user = AuthFilter.currentUser(context);
            rejectProductionOperator(user);
            dataScopeService.snapshot(user).requireWorkOrder(workOrderId);
            boolean ownOnly = user.hasRole("QUALITY_INSPECTOR")
                    || user.hasRole("EQUIPMENT_MAINTAINER");
            return ApiResponse.ok(ownOnly
                    ? service.listOwnFeedbackForWorkOrder(workOrderId, user.user.userId)
                    : service.listFeedbackForWorkOrder(workOrderId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：GET /api/management-feedback/{id}。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}")
    public ApiResponse<MesManagementFeedback> getFeedback(@PathParam("id") long id,
            @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            rejectProductionOperator(user);
            boolean ownOnly = user.hasRole("QUALITY_INSPECTOR")
                    || user.hasRole("EQUIPMENT_MAINTAINER");
            var feedback = (ownOnly ? service.getOwnFeedback(id, user.user.userId) : service.getFeedback(id))
                    .orElseThrow(() -> new BadRequestException("Feedback not found"));
            if (feedback.workOrderId() != null) {
                dataScopeService.snapshot(user).requireWorkOrder(feedback.workOrderId());
            }
            return ApiResponse.ok(feedback);
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/management-feedback。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    public ApiResponse<Long> createFeedback(MesManagementFeedback feedback,
            @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            rejectProductionOperator(user);
            if (feedback == null) {
                throw new BadRequestException("反馈内容不能为空");
            }
            if (feedback.workOrderId() != null) {
                dataScopeService.snapshot(user).requireWorkOrder(feedback.workOrderId());
            }
            return ApiResponse.ok(service.createFeedback(feedback, user.user.userId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/management-feedback/{id}/close。
     * 用例：关闭业务事项；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/close")
    public ApiResponse<Boolean> closeFeedback(@PathParam("id") long id,
            @Context ContainerRequestContext context) {
        try {
            var feedback = service.getFeedback(id)
                    .orElseThrow(() -> new BadRequestException("Feedback not found"));
            if (feedback.workOrderId() != null) {
                dataScopeService.snapshot(AuthFilter.currentUser(context)).requireWorkOrder(feedback.workOrderId());
            }
            return ApiResponse.ok(service.closeFeedback(id));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 内部实现步骤：驳回业务事项。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static void rejectProductionOperator(AuthenticatedUser user) {
        if (user.hasRole("PRODUCTION_OPERATOR")) {
            throw new BadRequestException("生产操作工不能使用管理反馈功能");
        }
    }
}
