/*
 * 答辩定位：设备与维修保养 模块的 MaintenanceOrderResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.equipment.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.UserRoleValidator;
import com.example.messystem.equipment.entity.MesMaintenanceOrder;
import com.example.messystem.equipment.service.EquipmentService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

/** 承载 /maintenance-orders 维修工单接口契约的 JAX-RS 控制器。 */
@Path("/maintenance-orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaintenanceOrderResource {

    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final EquipmentService service = new EquipmentService();

    /**
     * 接口：GET /api/maintenance-orders/maintainers。
     * 用例：执行 maintainers 对应的业务步骤；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/maintainers")
    public ApiResponse<List<UserRoleValidator.AssignableUser>> maintainers() {
        try {
            return ApiResponse.ok(UserRoleValidator.listEnabledUsers("EQUIPMENT_MAINTAINER"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：GET /api/maintenance-orders。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public ApiResponse<List<MesMaintenanceOrder>> list(@Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            return ApiResponse.ok(user.hasRole("EQUIPMENT_MAINTAINER")
                    ? service.listMaintenanceOrdersForMaintainer(user.user.userId)
                    : service.listMaintenanceOrders());
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/maintenance-orders/{id}/assign。
     * 用例：分配执行人员或资源；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/assign")
    public ApiResponse<Boolean> assign(@PathParam("id") long id, @QueryParam("maintainerId") long maintainerId) {
        try {
            return ApiResponse.ok(service.assignMaintenanceOrder(id, maintainerId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/maintenance-orders/{id}/finish。
     * 用例：完成业务任务；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/finish")
    public ApiResponse<Boolean> finish(@PathParam("id") long id, MesMaintenanceOrder order,
            @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            return ApiResponse.ok(service.finishMaintenanceOrder(id,
                    user.user.userId,
                    order == null ? "" : order.resultDesc(),
                    user.isSuperAdmin()));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/maintenance-orders/{id}/accept。
     * 用例：受理业务事项；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/accept")
    public ApiResponse<Boolean> accept(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            return ApiResponse.ok(service.acceptMaintenanceOrder(id,
                    AuthFilter.currentUser(context).user.userId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
