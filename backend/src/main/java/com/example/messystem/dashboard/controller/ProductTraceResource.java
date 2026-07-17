package com.example.messystem.dashboard.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.dashboard.entity.MesProductTrace;
import com.example.messystem.dashboard.service.ProductTraceService;
import com.example.messystem.security.service.DataScopeService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** 承载 /product-traces 产品追溯接口契约的 JAX-RS 控制器。 */
@Path("/product-traces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductTraceResource {

    private final ProductTraceService service = new ProductTraceService();
    private final DataScopeService dataScopeService = new DataScopeService();

    @GET
    public ApiResponse<List<MesProductTrace>> listTraces() {
        try {
            return ApiResponse.ok(service.listProductTraces());
        } catch (SQLException e) {
            throw databaseFailure("读取产品追溯列表失败", e);
        }
    }

    @GET
    @Path("/{id}")
    public ApiResponse<Map<String, Object>> getTrace(@PathParam("id") String id,
            @Context ContainerRequestContext context) {
        try {
            Map<String, Object> chain = service.getProductTraceChain(id);
            MesProductTrace trace = (MesProductTrace) chain.get("trace");
            dataScopeService.snapshot(AuthFilter.currentUser(context)).requireWorkOrder(trace.workOrderId());
            return ApiResponse.ok(chain);
        } catch (SQLException e) {
            throw databaseFailure("读取产品追溯详情失败", e);
        }
    }

    @GET
    @Path("/work-orders/{workOrderId}")
    public ApiResponse<List<MesProductTrace>> listByWorkOrder(@PathParam("workOrderId") long workOrderId) {
        try {
            return ApiResponse.ok(service.listTracesByWorkOrder(workOrderId));
        } catch (SQLException e) {
            throw databaseFailure("读取工单产品追溯记录失败", e);
        }
    }

    @POST
    public ApiResponse<Long> createTrace(MesProductTrace trace, @Context ContainerRequestContext context) {
        try {
            if (trace == null) {
                throw new BadRequestException("产品追溯信息不能为空");
            }
            if (trace.workOrderId() != null) {
                dataScopeService.snapshot(AuthFilter.currentUser(context)).requireWorkOrder(trace.workOrderId());
            }
            return ApiResponse.ok(service.createProductTrace(trace));
        } catch (SQLException e) {
            throw databaseFailure("创建产品追溯记录失败", e);
        }
    }

    private static IllegalStateException databaseFailure(String message, SQLException exception) {
        return new IllegalStateException(message + "：" + exception.getMessage(), exception);
    }
}
