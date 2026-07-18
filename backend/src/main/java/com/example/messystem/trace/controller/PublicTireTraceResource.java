/*
 * 答辩定位：轮胎标签与公开追溯 模块的 PublicTireTraceResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.trace.controller;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.trace.service.TireTraceService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/** 承载 /public/tire-traces 公开轮胎追溯接口契约的 JAX-RS 控制器。 */
@Path("/public/tire-traces")
@Produces(MediaType.APPLICATION_JSON)
public class PublicTireTraceResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final TireTraceService service = new TireTraceService();

    /**
     * 接口：GET /api/public/tire-traces/{token}。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{token}")
    public ApiResponse<Map<String, Object>> get(@PathParam("token") String token) {
        return ApiResponse.ok(service.publicView(token));
    }

    /**
     * 接口：GET /api/public/tire-traces/{token}/document。
     * 用例：执行 document 对应的业务步骤；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{token}/document")
    @Produces("application/pdf")
    public Response document(@PathParam("token") String token) {
        return TireLabelResource.file(service.publicDocument(token), "application/pdf", "tire-trace.pdf", false);
    }
}
