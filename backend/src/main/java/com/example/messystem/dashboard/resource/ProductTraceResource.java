package com.example.messystem.dashboard.resource;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.dashboard.entity.MesProductTrace;
import com.example.messystem.dashboard.service.ProductTraceService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;

@Path("/product-traces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductTraceResource {

    private final ProductTraceService service = new ProductTraceService();

    @GET
    public ApiResponse<List<MesProductTrace>> listTraces() {
        try {
            return ApiResponse.ok(service.listProductTraces());
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/{id}")
    public ApiResponse<MesProductTrace> getTrace(@PathParam("id") String id) {
        try {
            return service.getProductTrace(id)
                    .map(ApiResponse::ok)
                    .orElseGet(() -> ApiResponse.fail("Product trace not found"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/work-orders/{workOrderId}")
    public ApiResponse<List<MesProductTrace>> listByWorkOrder(@PathParam("workOrderId") long workOrderId) {
        try {
            return ApiResponse.ok(service.listTracesByWorkOrder(workOrderId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    public ApiResponse<Long> createTrace(MesProductTrace trace) {
        try {
            if (trace == null) {
                throw new BadRequestException("产品追溯信息不能为空");
            }
            return ApiResponse.ok(service.createProductTrace(trace));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
