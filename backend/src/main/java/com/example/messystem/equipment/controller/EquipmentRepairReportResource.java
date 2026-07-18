/*
 * 答辩定位：设备与维修保养 模块的 EquipmentRepairReportResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.equipment.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.security.service.DataScopeService;
import com.example.messystem.equipment.entity.MesEquipmentRepairReport;
import com.example.messystem.equipment.service.EquipmentService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

/** 承载 /equipment-repair-reports 设备报修接口契约的 JAX-RS 控制器。 */
@Path("/equipment-repair-reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EquipmentRepairReportResource {

    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final EquipmentService service = new EquipmentService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final DataScopeService dataScopeService = new DataScopeService();

    /**
     * 接口：GET /api/equipment-repair-reports。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public ApiResponse<List<MesEquipmentRepairReport>> list() {
        try {
            return ApiResponse.ok(service.listRepairReports());
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/equipment-repair-reports。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    public ApiResponse<Long> create(MesEquipmentRepairReport report, @Context ContainerRequestContext context) {
        try {
            if (report == null) {
                throw new BadRequestException("Repair report body is required");
            }
            dataScopeService.snapshot(AuthFilter.currentUser(context)).requireEquipment(report.equipmentId());
            MesEquipmentRepairReport payload = new MesEquipmentRepairReport(
                    report.repairReportId(), report.repairReportNo(), report.equipmentId(),
                    report.workOrderId(), report.faultLevel(), report.faultDesc(),
                    AuthFilter.currentUser(context).user.userId, report.reportTime(), report.repairStatus());
            return ApiResponse.ok(service.createRepairReport(payload));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/equipment-repair-reports/{id}/approve。
     * 用例：审核通过业务事项；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/approve")
    public ApiResponse<Boolean> approve(@PathParam("id") long id) {
        try {
            return ApiResponse.ok(service.approveRepairReport(id));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/equipment-repair-reports/{id}/to-maintenance-order。
     * 用例：执行 toMaintenanceOrder 对应的业务步骤；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/to-maintenance-order")
    public ApiResponse<Long> toMaintenanceOrder(@PathParam("id") long id) {
        try {
            return ApiResponse.ok(service.convertRepairReportToMaintenanceOrder(id));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
