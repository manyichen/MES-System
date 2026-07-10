package com.example.messystem.quality.resource;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.quality.entity.MesQualityTrace;
import com.example.messystem.quality.service.QualityInspectionService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;

@Path("/quality-traces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QualityTraceResource {

    private final QualityInspectionService service = new QualityInspectionService();

    @GET
    public ApiResponse<List<MesQualityTrace>> listAll() {
        try {
            return ApiResponse.ok(service.listAllTraces());
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/work-orders/{workOrderId}")
    public ApiResponse<List<MesQualityTrace>> listByWorkOrder(@PathParam("workOrderId") long workOrderId) {
        try {
            return ApiResponse.ok(service.listTracesByWorkOrder(workOrderId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/by-work-order/{workOrderId}")
    public ApiResponse<List<MesQualityTrace>> listByWorkOrderAlias(@PathParam("workOrderId") long workOrderId) {
        return listByWorkOrder(workOrderId);
    }

    @GET
    @Path("/by-inspection/{inspectionId}")
    public ApiResponse<List<MesQualityTrace>> listByInspection(@PathParam("inspectionId") long inspectionId) {
        try {
            return ApiResponse.ok(service.listTracesByInspection(inspectionId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/{id}")
    public ApiResponse<MesQualityTrace> get(@PathParam("id") long id) {
        try {
            return service.getTraceById(id)
                    .map(ApiResponse::ok)
                    .orElseGet(() -> ApiResponse.fail("Quality trace not found"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
