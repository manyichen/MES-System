package com.example.messystem.dashboard.resource;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.security.DataScopeService;
import com.example.messystem.dashboard.entity.MesManagementFeedback;
import com.example.messystem.dashboard.service.ManagementFeedbackService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

@Path("/management-feedback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ManagementFeedbackResource {

    private final ManagementFeedbackService service = new ManagementFeedbackService();
    private final DataScopeService dataScopeService = new DataScopeService();

    @GET
    public ApiResponse<List<MesManagementFeedback>> listFeedback(@QueryParam("workOrderId") Long workOrderId,
            @Context ContainerRequestContext context) {
        try {
            if (workOrderId == null) {
                return ApiResponse.fail("workOrderId query parameter is required");
            }
            AuthenticatedUser user = AuthFilter.currentUser(context);
            boolean ownOnly = user.hasRole("PRODUCTION_OPERATOR") || user.hasRole("QUALITY_INSPECTOR")
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
            boolean ownOnly = user.hasRole("PRODUCTION_OPERATOR") || user.hasRole("QUALITY_INSPECTOR")
                    || user.hasRole("EQUIPMENT_MAINTAINER");
            return (ownOnly ? service.getOwnFeedback(id, user.user.userId) : service.getFeedback(id))
                    .map(ApiResponse::ok)
                    .orElseGet(() -> ApiResponse.fail("Feedback not found"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    public ApiResponse<Long> createFeedback(MesManagementFeedback feedback,
            @Context ContainerRequestContext context) {
        try {
            if (feedback == null) {
                throw new BadRequestException("Feedback body is required");
            }
            if (feedback.workOrderId() != null) {
                dataScopeService.snapshot(AuthFilter.currentUser(context)).requireWorkOrder(feedback.workOrderId());
            }
            return ApiResponse.ok(service.createFeedback(feedback,
                    AuthFilter.currentUser(context).user.userId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/close")
    public ApiResponse<Boolean> closeFeedback(@PathParam("id") long id) {
        try {
            return ApiResponse.ok(service.closeFeedback(id));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
