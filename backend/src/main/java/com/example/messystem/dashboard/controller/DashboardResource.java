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
    private final ExecutiveDashboardService executiveDashboardService = new ExecutiveDashboardService();
    private final RoleDashboardService roleDashboardService = new RoleDashboardService();

    /** 构建当前角色的指标、权限边界和可处理待办队列。 */
    @GET
    @Path("/my-summary")
    public ApiResponse<RoleDashboard> mySummary(@Context ContainerRequestContext context) {
        return ApiResponse.ok(roleDashboardService.build(AuthFilter.currentUser(context)));
    }

    /** 返回仅供总经理查看的全厂只读经营视图。 */
    @GET
    @Path("/executive")
    public ApiResponse<ExecutiveDashboard> executive(@Context ContainerRequestContext context) {
        return ApiResponse.ok(executiveDashboardService.build(AuthFilter.currentUser(context)));
    }
}
