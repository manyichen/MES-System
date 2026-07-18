/*
 * 答辩定位：质检、质量追溯与返工 模块的 ReworkOrderResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.quality.controller;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.quality.entity.MesReworkOrder;
import com.example.messystem.quality.service.ReworkOrderService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;

/** 承载 /rework-orders 返工单接口契约的 JAX-RS 控制器。 */
@Path("/rework-orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReworkOrderResource {

    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final ReworkOrderService service = new ReworkOrderService();

    /**
     * 接口：POST /api/rework-orders。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    public ApiResponse<Long> createReworkOrder(MesReworkOrder order) {
        try {
            if (order == null) {
                throw new BadRequestException("返工单信息不能为空");
            }
            return ApiResponse.ok(service.createReworkOrder(order));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：GET /api/rework-orders/{id}。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}")
    public ApiResponse<MesReworkOrder> getReworkOrder(@PathParam("id") long id) {
        try {
            return service.getReworkOrder(id)
                    .map(ApiResponse::ok)
                    .orElseGet(() -> ApiResponse.fail("Rework order not found"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：GET /api/rework-orders。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public ApiResponse<java.util.List<MesReworkOrder>> listByInspection(@QueryParam("inspectionId") Long inspectionId) {
        try {
            if (inspectionId == null) {
                return ApiResponse.ok(service.listReworkOrders());
            }
            return ApiResponse.ok(service.listReworkOrdersByInspection(inspectionId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/rework-orders/{id}/dispatch。
     * 用例：派发业务任务；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/dispatch")
    public ApiResponse<Boolean> dispatch(@PathParam("id") long id) {
        try {
            return ApiResponse.ok(service.dispatch(id));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/rework-orders/{id}/finish。
     * 用例：完成业务任务；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/finish")
    public ApiResponse<Boolean> finish(@PathParam("id") long id) {
        try {
            return ApiResponse.ok(service.finish(id));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
