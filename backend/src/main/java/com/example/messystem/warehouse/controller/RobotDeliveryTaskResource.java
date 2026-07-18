/*
 * 答辩定位：仓储、领料、拣货与机器人物流 模块的 RobotDeliveryTaskResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.warehouse.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.security.service.DataScopeService;
import com.example.messystem.warehouse.entity.MesRobotDeliveryTask;
import com.example.messystem.warehouse.service.WarehouseService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

/** 承载 /robot-delivery-tasks 机器人配送接口契约的 JAX-RS 控制器。 */
@Path("/robot-delivery-tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RobotDeliveryTaskResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final WarehouseService service = new WarehouseService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final DataScopeService dataScopeService = new DataScopeService();

    /**
     * 接口：GET /api/robot-delivery-tasks。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public Response list() {
        return ResourceSupport.ok(service.listDeliveryTasks());
    }

    /**
     * 接口：GET /api/robot-delivery-tasks/{id}。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getDeliveryTask(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/robot-delivery-tasks。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    public Response create(MesRobotDeliveryTask task, @Context ContainerRequestContext context) {
        try {
            dataScopeService.snapshot(AuthFilter.currentUser(context))
                    .requireWarehouseEntity("picking", task.pickingTaskId);
            return ResourceSupport.created("配送任务已创建", service.createDeliveryTask(task));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/robot-delivery-tasks/{id}/arrive。
     * 用例：确认物流到达；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/arrive")
    public Response arrive(@PathParam("id") long id) {
        try {
            return ResourceSupport.action("配送任务已到达", service.markDeliveryArrived(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/robot-delivery-tasks/{id}/confirm-receipt。
     * 用例：确认物流任务已经收货；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/confirm-receipt")
    public Response confirmReceipt(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (!user.canActAs("PRODUCTION_OPERATOR")) {
                throw new BadRequestException("only production operators can confirm requisition receipt");
            }
            return ResourceSupport.action("物料已确认接收", user.isSuperAdmin()
                    ? service.confirmDeliveryReceipt(id)
                    : service.confirmDeliveryReceipt(id, user.user.userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
