package com.example.messystem.quality.controller;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.quality.entity.MesQualityTrace;
import com.example.messystem.quality.service.QualityInspectionService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;

/** 承载 /quality-traces 质量追溯接口契约的 JAX-RS 控制器。 */
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
            throw databaseFailure("读取质量追溯列表失败", e);
        }
    }

    @GET
    @Path("/work-orders/{workOrderId}")
    public ApiResponse<List<MesQualityTrace>> listByWorkOrder(@PathParam("workOrderId") long workOrderId) {
        try {
            return ApiResponse.ok(service.listTracesByWorkOrder(workOrderId));
        } catch (SQLException e) {
            throw databaseFailure("读取工单质量追溯记录失败", e);
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
            throw databaseFailure("读取质检单追溯记录失败", e);
        }
    }

    @GET
    @Path("/{id}")
    public ApiResponse<MesQualityTrace> get(@PathParam("id") long id) {
        try {
            return service.getTraceById(id)
                    .map(ApiResponse::ok)
                    .orElseThrow(() -> new NotFoundException("质量追溯记录不存在"));
        } catch (SQLException e) {
            throw databaseFailure("读取质量追溯详情失败", e);
        }
    }

    private static IllegalStateException databaseFailure(String message, SQLException exception) {
        return new IllegalStateException(message + "：" + exception.getMessage(), exception);
    }
}
