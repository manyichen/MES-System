/*
 * 答辩定位：质检、质量追溯与返工 模块的 QualityTraceResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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

    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final QualityInspectionService service = new QualityInspectionService();

    /**
     * 接口：GET /api/quality-traces。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public ApiResponse<List<MesQualityTrace>> listAll() {
        try {
            return ApiResponse.ok(service.listAllTraces());
        } catch (SQLException e) {
            throw databaseFailure("读取质量追溯列表失败", e);
        }
    }

    /**
     * 接口：GET /api/quality-traces/work-orders/{workOrderId}。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/work-orders/{workOrderId}")
    public ApiResponse<List<MesQualityTrace>> listByWorkOrder(@PathParam("workOrderId") long workOrderId) {
        try {
            return ApiResponse.ok(service.listTracesByWorkOrder(workOrderId));
        } catch (SQLException e) {
            throw databaseFailure("读取工单质量追溯记录失败", e);
        }
    }

    /**
     * 接口：GET /api/quality-traces/by-work-order/{workOrderId}。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/by-work-order/{workOrderId}")
    public ApiResponse<List<MesQualityTrace>> listByWorkOrderAlias(@PathParam("workOrderId") long workOrderId) {
        return listByWorkOrder(workOrderId);
    }

    /**
     * 接口：GET /api/quality-traces/by-inspection/{inspectionId}。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/by-inspection/{inspectionId}")
    public ApiResponse<List<MesQualityTrace>> listByInspection(@PathParam("inspectionId") long inspectionId) {
        try {
            return ApiResponse.ok(service.listTracesByInspection(inspectionId));
        } catch (SQLException e) {
            throw databaseFailure("读取质检单追溯记录失败", e);
        }
    }

    /**
     * 接口：GET /api/quality-traces/{id}。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
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

    /**
     * 内部实现步骤：执行 databaseFailure 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static IllegalStateException databaseFailure(String message, SQLException exception) {
        return new IllegalStateException(message + "：" + exception.getMessage(), exception);
    }
}
