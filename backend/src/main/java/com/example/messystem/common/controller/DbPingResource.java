/*
 * 答辩定位：公共基础设施 模块的 DbPingResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common.controller;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.service.HealthService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/** 供运维人员检查应用和数据库健康状态的控制器。 */
@Path("/db")
@Produces(MediaType.APPLICATION_JSON)
public class DbPingResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final HealthService service = new HealthService();

    /** 返回安全的连接元数据以及 MES 核心表可用状态。 */
    /**
     * 接口：GET /api/db/ping。
     * 用例：执行数据库健康检查，返回连接信息和关键表可用状态，供部署探针与系统管理员诊断。
     * 权限：需要 system.health.read；不返回数据库密码等敏感配置。
     */
    @GET
    @Path("/ping")
    public Response ping() {
        try {
            Map<String, Object> data = service.databaseHealth();
            return Response.ok(ApiResponse.ok("database connected", data)).build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.fail("database connection failed: " + ex.getMessage()))
                    .build();
        }
    }
}
