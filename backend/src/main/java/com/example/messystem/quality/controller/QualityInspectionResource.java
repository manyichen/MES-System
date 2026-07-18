/*
 * 答辩定位：质检、质量追溯与返工 模块的 QualityInspectionResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.quality.controller;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.IdGenerator;
import com.example.messystem.common.UserRoleValidator;
import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.quality.entity.MesQualityInspection;
import com.example.messystem.quality.entity.MesQualityInspectionItem;
import com.example.messystem.quality.service.QualityInspectionService;
import com.example.messystem.production.service.ProductionService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

/** 承载 /quality-inspections 质量检验接口契约的 JAX-RS 控制器。 */
@Path("/quality-inspections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QualityInspectionResource {

    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final QualityInspectionService service = new QualityInspectionService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final ProductionService productionService = new ProductionService();

    /**
     * 接口：GET /api/quality-inspections/create-options。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/create-options")
    public ApiResponse<Map<String, Object>> createOptions() {
        try {
            Set<Long> inspectedReportIds = service.listInspections().stream()
                    .map(MesQualityInspection::workReportId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            var reports = productionService.listWorkReports().stream()
                    .filter(report -> "APPROVED".equals(report.reportStatus))
                    .filter(report -> report.reportId != null && !inspectedReportIds.contains(report.reportId))
                    .toList();
            return ApiResponse.ok(Map.of("workReports", reports));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：GET /api/quality-inspections/inspectors。
     * 用例：检查运行状态；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/inspectors")
    public ApiResponse<List<UserRoleValidator.AssignableUser>> inspectors() {
        try {
            return ApiResponse.ok(UserRoleValidator.listEnabledUsers("QUALITY_INSPECTOR"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：GET /api/quality-inspections。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public ApiResponse<List<MesQualityInspection>> list(@Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            return ApiResponse.ok(user.hasRole("QUALITY_INSPECTOR")
                    ? service.listAssignedInspections(user.user.userId)
                    : service.listInspections());
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：GET /api/quality-inspections/{id}。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}")
    public ApiResponse<MesQualityInspection> get(@PathParam("id") long id,
            @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (user.hasRole("QUALITY_INSPECTOR")) {
                return ApiResponse.ok(service.requireAssignedInspection(id, user.user.userId));
            }
            return service.getInspectionById(id)
                    .map(ApiResponse::ok)
                    .orElseGet(() -> ApiResponse.fail("Inspection not found"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/quality-inspections。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    public ApiResponse<Long> create(MesQualityInspection inspection) {
        try {
            if (inspection == null) {
                throw new BadRequestException("质检单内容不能为空");
            }
            Long workOrderId = inspection.workOrderId();
            Integer sampleQty = inspection.sampleQty();
            if (inspection.workReportId() != null) {
                var report = productionService.getWorkReport(inspection.workReportId());
                if (workOrderId == null) {
                    workOrderId = report.workOrderId;
                }
                if (sampleQty == null || sampleQty <= 0) {
                    sampleQty = report.qualifiedQty;
                }
            }
            if (sampleQty == null || sampleQty <= 0) {
                throw new BadRequestException("抽检数量必须大于 0");
            }
            String inspectionNo = inspection.inspectionNo();
            if (inspectionNo == null || inspectionNo.isBlank()) {
                inspectionNo = IdGenerator.nextCode("QI");
            }
            MesQualityInspection payload = new MesQualityInspection(
                    null, inspectionNo, workOrderId, inspection.workReportId(),
                    sampleQty, "CREATED", null, null, null, null,
                    null, null, null, null, null, null);
            long id = service.createInspection(payload);
            return ApiResponse.ok(id);
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：GET /api/quality-inspections/{id}/items。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}/items")
    public ApiResponse<List<MesQualityInspectionItem>> listItems(@PathParam("id") long id,
            @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (user.hasRole("QUALITY_INSPECTOR")) {
                service.requireAssignedInspection(id, user.user.userId);
            }
            return ApiResponse.ok(service.getInspectionItems(id));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/quality-inspections/{id}/items。
     * 用例：执行 addItem 对应的业务步骤；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/items")
    public ApiResponse<Long> addItem(@PathParam("id") long id, MesQualityInspectionItem item,
            @Context ContainerRequestContext context) {
        try {
            if (item == null) {
                throw new BadRequestException("质检项目不能为空");
            }
            AuthenticatedUser user = AuthFilter.currentUser(context);
            MesQualityInspectionItem payload = new MesQualityInspectionItem(
                    null,
                    id,
                    item.itemCode(),
                    item.itemName(),
                    item.standardValue(),
                    item.actualValue(),
                    item.itemResult(),
                    item.remark()
            );
            long itemId = user.hasRole("SYSTEM_ADMIN") || user.isSuperAdmin()
                    ? service.addInspectionItem(payload)
                    : service.addAssignedInspectionItem(payload, user.user.userId);
            return ApiResponse.ok(itemId);
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/quality-inspections/{id}/assign。
     * 用例：分配执行人员或资源；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/assign")
    public ApiResponse<Boolean> assign(@PathParam("id") long id, @QueryParam("inspectorId") long inspectorId) {
        try {
            return ApiResponse.ok(service.assignInspection(id, inspectorId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/quality-inspections/{id}/submit。
     * 用例：提交业务事项；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/submit")
    public ApiResponse<Boolean> submit(@PathParam("id") long id, QualitySubmitResult result,
            @Context ContainerRequestContext context) {
        try {
            if (result == null || result.result() == null || result.result().isBlank()) {
                throw new BadRequestException("质检结果不能为空");
            }
            AuthenticatedUser user = AuthFilter.currentUser(context);
            return ApiResponse.ok(service.submitInspection(id, user.user.userId,
                    result.result(), result.note(), user.isSuperAdmin()));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/quality-inspections/{id}/judge。
     * 用例：执行 judge 对应的业务步骤；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/judge")
    public ApiResponse<Boolean> judge(@PathParam("id") long id, QualityJudgement judgement,
            @Context ContainerRequestContext context) {
        try {
            if (judgement == null || judgement.status() == null || judgement.result() == null) {
                throw new BadRequestException("审核状态和判定结果不能为空");
            }
            return ApiResponse.ok(service.judgeInspection(id, judgement.status(), judgement.result(),
                    AuthFilter.currentUser(context).user.userId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 公共能力：执行 QualityJudgement 对应的业务步骤。
     * 由 QualityInspectionResource 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public record QualityJudgement(String status, String result) {
    }

    /**
     * 公共能力：执行 QualitySubmitResult 对应的业务步骤。
     * 由 QualityInspectionResource 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public record QualitySubmitResult(String result, String note) {
    }
}
