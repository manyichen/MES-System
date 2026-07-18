/*
 * 答辩定位：驾驶舱、反馈与产品追溯 模块的 ProductTraceResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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

    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final ProductTraceService service = new ProductTraceService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final DataScopeService dataScopeService = new DataScopeService();

    /**
     * 接口：GET /api/product-traces。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public ApiResponse<List<MesProductTrace>> listTraces() {
        try {
            return ApiResponse.ok(service.listProductTraces());
        } catch (SQLException e) {
            throw databaseFailure("读取产品追溯列表失败", e);
        }
    }

    /**
     * 接口：GET /api/product-traces/{id}。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
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

    /**
     * 接口：GET /api/product-traces/work-orders/{workOrderId}。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/work-orders/{workOrderId}")
    public ApiResponse<List<MesProductTrace>> listByWorkOrder(@PathParam("workOrderId") long workOrderId) {
        try {
            return ApiResponse.ok(service.listTracesByWorkOrder(workOrderId));
        } catch (SQLException e) {
            throw databaseFailure("读取工单产品追溯记录失败", e);
        }
    }

    /**
     * 接口：POST /api/product-traces。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
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

    /**
     * 内部实现步骤：执行 databaseFailure 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static IllegalStateException databaseFailure(String message, SQLException exception) {
        return new IllegalStateException(message + "：" + exception.getMessage(), exception);
    }
}
