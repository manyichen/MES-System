package com.example.messystem.dashboard.resource;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.auth.AuthFilter;
import com.example.messystem.dashboard.entity.MesDashboardMetric;
import com.example.messystem.dashboard.entity.RoleDashboard;
import com.example.messystem.dashboard.service.DashboardService;
import com.example.messystem.dashboard.service.RoleDashboardService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import java.sql.SQLException;
import java.util.List;

@Path("/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DashboardResource {

    private final DashboardService service = new DashboardService();
    private final RoleDashboardService roleDashboardService = new RoleDashboardService();

    @GET
    @Path("/my-summary")
    public ApiResponse<RoleDashboard> mySummary(@Context ContainerRequestContext context) {
        return ApiResponse.ok(roleDashboardService.build(AuthFilter.currentUser(context)));
    }

    @GET
    @Path("/metrics")
    public ApiResponse<List<MesDashboardMetric>> listMetrics() {
        try {
            return ApiResponse.ok(service.listMetrics());
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/metrics/{id}")
    public ApiResponse<MesDashboardMetric> getMetric(@PathParam("id") long id) {
        try {
            return service.getMetric(id)
                    .map(ApiResponse::ok)
                    .orElseGet(() -> ApiResponse.fail("Metric not found"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/metrics")
    public ApiResponse<Long> createMetric(MesDashboardMetric metric) {
        try {
            if (metric == null) {
                throw new BadRequestException("Metric body is required");
            }
            return ApiResponse.ok(service.createMetric(metric));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/metrics/default")
    public ApiResponse<Long> createDefaultMetric(DefaultMetricRequest request) {
        try {
            if (request == null || request.metricKey() == null || request.metricName() == null) {
                throw new BadRequestException("Metric key and name are required");
            }
            return ApiResponse.ok(service.createDefaultMetric(request.metricKey(), request.metricName(), request.metricValue(), request.metricType()));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    public record DefaultMetricRequest(String metricKey, String metricName, String metricValue, String metricType) {
    }

    @GET
    @Path("/summary")
    public ApiResponse<List<MesDashboardMetric>> summary() {
        return listMetrics();
    }

    @GET
    @Path("/quality")
    public ApiResponse<List<MesDashboardMetric>> quality() {
        return listMetrics();
    }

    @GET
    @Path("/equipment")
    public ApiResponse<List<MesDashboardMetric>> equipment() {
        return listMetrics();
    }

    @GET
    @Path("/production")
    public ApiResponse<List<MesDashboardMetric>> production() {
        return listMetrics();
    }
}
