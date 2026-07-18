/*
 * 答辩定位：驾驶舱、反馈与产品追溯 模块的 DashboardResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.dashboard.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ApiResponse;
import com.example.messystem.dashboard.entity.ExecutiveDashboard;
import com.example.messystem.dashboard.entity.RoleDashboard;
import com.example.messystem.dashboard.service.ExecutiveDashboardService;
import com.example.messystem.dashboard.service.RoleDashboardService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/** 为 Vue 应用提供角色首页和经营看板两类有效接口。 */
@Path("/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DashboardResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final ExecutiveDashboardService executiveDashboardService = new ExecutiveDashboardService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final RoleDashboardService roleDashboardService = new RoleDashboardService();

    /** 构建当前角色的指标、权限边界和可处理待办队列。 */
    /**
     * 接口：GET /api/dashboard/my-summary。
     * 用例：按当前登录用户的岗位、权限和数据范围返回角色首页指标、待办与快捷入口。
     * 调用链：DashboardResource -> RoleDashboardService -> RoleDashboardDao -> PostgreSQL。
     */
    @GET
    @Path("/my-summary")
    public ApiResponse<RoleDashboard> mySummary(@Context ContainerRequestContext context) {
        return ApiResponse.ok(roleDashboardService.build(AuthFilter.currentUser(context)));
    }

    /** 返回仅供总经理查看的全厂只读经营视图。 */
    /**
     * 接口：GET /api/dashboard/executive。
     * 用例：聚合经营指标、生产趋势、产线、告警、部门报告和审计发现，供总经理驾驶舱展示。
     * 调用链：DashboardResource -> ExecutiveDashboardService -> ExecutiveDashboardDao -> PostgreSQL。
     */
    @GET
    @Path("/executive")
    public ApiResponse<ExecutiveDashboard> executive(@Context ContainerRequestContext context) {
        return ApiResponse.ok(executiveDashboardService.build(AuthFilter.currentUser(context)));
    }
}
