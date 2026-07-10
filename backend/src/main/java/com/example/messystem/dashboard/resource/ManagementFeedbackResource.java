package com.example.messystem.dashboard.resource;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.dashboard.entity.MesManagementFeedback;
import com.example.messystem.dashboard.service.ManagementFeedbackService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;

@Path("/management-feedback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ManagementFeedbackResource {

    private final ManagementFeedbackService service = new ManagementFeedbackService();

    @GET
    public ApiResponse<List<MesManagementFeedback>> listFeedback(@QueryParam("workOrderId") Long workOrderId) {
        try {
            if (workOrderId == null) {
                return ApiResponse.fail("workOrderId query parameter is required");
            }
            return ApiResponse.ok(service.listFeedbackForWorkOrder(workOrderId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/{id}")
    public ApiResponse<MesManagementFeedback> getFeedback(@PathParam("id") long id) {
        try {
            return service.getFeedback(id)
                    .map(ApiResponse::ok)
                    .orElseGet(() -> ApiResponse.fail("Feedback not found"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    public ApiResponse<Long> createFeedback(MesManagementFeedback feedback) {
        try {
            if (feedback == null) {
                throw new BadRequestException("Feedback body is required");
            }
            return ApiResponse.ok(service.createFeedback(feedback));
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
