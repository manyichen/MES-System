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

    private final ManagementFeedbackService service = new ManagementFeedbackService();
    private final DataScopeService dataScopeService = new DataScopeService();
    private final WorkOrderService workOrderService = new WorkOrderService();

    @GET
    @Path("/create-options")
    public ApiResponse<List<MesWorkOrder>> createOptions(@Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        rejectProductionOperator(user);
        var scope = dataScopeService.snapshot(user);
        return ApiResponse.ok(workOrderService.listWorkOrders().stream().filter(scope::canView).toList());
    }

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

    private static void rejectProductionOperator(AuthenticatedUser user) {
        if (user.hasRole("PRODUCTION_OPERATOR")) {
            throw new BadRequestException("生产操作工不能使用管理反馈功能");
        }
    }
}
