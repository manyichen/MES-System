/*
 * 答辩定位：设备与维修保养 模块的 EquipmentResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.equipment.controller;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.equipment.entity.MesEquipment;
import com.example.messystem.equipment.service.EquipmentService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;

/** 承载 /equipment 设备台账接口契约的 JAX-RS 控制器。 */
@Path("/equipment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EquipmentResource {

    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final EquipmentService service = new EquipmentService();

    /**
     * 接口：GET /api/equipment。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public ApiResponse<List<MesEquipment>> listEquipment() {
        try {
            return ApiResponse.ok(service.listEquipment());
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：GET /api/equipment/{id}。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}")
    public ApiResponse<MesEquipment> getEquipment(@PathParam("id") long id) {
        try {
            return service.getEquipmentById(id)
                    .map(ApiResponse::ok)
                    .orElseGet(() -> ApiResponse.fail("Equipment not found"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：GET /api/equipment/by-line/{lineId}。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/by-line/{lineId}")
    public ApiResponse<List<MesEquipment>> listByLine(@PathParam("lineId") long lineId) {
        try {
            return ApiResponse.ok(service.listEquipmentByLine(lineId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/equipment。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    public ApiResponse<Long> createEquipment(MesEquipment equipment) {
        try {
            if (equipment == null) {
                throw new BadRequestException("Equipment body is required");
            }
            return ApiResponse.ok(service.createEquipment(equipment));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：PUT /api/equipment/{id}/status。
     * 用例：更新业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @PUT
    @Path("/{id}/status")
    public ApiResponse<Boolean> updateStatus(@PathParam("id") long id, StatusUpdate statusUpdate) {
        try {
            if (statusUpdate == null || statusUpdate.status() == null) {
                throw new BadRequestException("Status value is required");
            }
            return ApiResponse.ok(service.updateEquipmentStatus(id, statusUpdate.status()));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 公共能力：执行 StatusUpdate 对应的业务步骤。
     * 由 EquipmentResource 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public record StatusUpdate(String status) {
    }
}
