/*
 * 答辩定位：仓储、领料、拣货与机器人物流 模块的 RobotResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.warehouse.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.security.service.DataScopeService;
import com.example.messystem.warehouse.entity.MesRobot;
import com.example.messystem.warehouse.service.WarehouseService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

/** 承载 /robots 仓储机器人接口契约的 JAX-RS 控制器。 */
@Path("/robots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RobotResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final WarehouseService service = new WarehouseService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final DataScopeService dataScopeService = new DataScopeService();

    /**
     * 接口：GET /api/robots。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public Response list() {
        return ResourceSupport.ok(service.listRobots());
    }

    /**
     * 接口：GET /api/robots/{id}。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getRobot(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/robots。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    public Response create(MesRobot robot, @Context ContainerRequestContext context) {
        try {
            dataScopeService.snapshot(AuthFilter.currentUser(context)).requireWarehouse(robot.warehouseId);
            return ResourceSupport.created("仓储机器人已创建", service.createRobot(robot));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：PUT /api/robots/{id}。
     * 用例：更新业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") long id, MesRobot robot,
            @Context ContainerRequestContext context) {
        try {
            if (robot.warehouseId != null) {
                dataScopeService.snapshot(AuthFilter.currentUser(context)).requireWarehouse(robot.warehouseId);
            }
            return ResourceSupport.action("仓储机器人已更新", service.updateRobot(id, robot));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：DELETE /api/robots/{id}。
     * 用例：删除业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        try {
            service.deleteRobot(id);
            return ResourceSupport.action("仓储机器人已删除", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
