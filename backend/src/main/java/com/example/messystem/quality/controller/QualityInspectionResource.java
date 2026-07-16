package com.example.messystem.quality.controller;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
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

    private final QualityInspectionService service = new QualityInspectionService();
    private final ProductionService productionService = new ProductionService();

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

    @GET
    @Path("/inspectors")
    public ApiResponse<List<UserRoleValidator.AssignableUser>> inspectors() {
        try {
            return ApiResponse.ok(UserRoleValidator.listEnabledUsers("QUALITY_INSPECTOR"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

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

    @POST
    public ApiResponse<Long> create(MesQualityInspection inspection) {
        try {
            if (inspection == null) {
                throw new BadRequestException("质检单内容不能为空");
            }
            Long workOrderId = inspection.workOrderId();
            if (workOrderId == null && inspection.workReportId() != null) {
                workOrderId = productionService.getWorkReport(inspection.workReportId()).workOrderId;
            }
            MesQualityInspection payload = new MesQualityInspection(
                    null, inspection.inspectionNo(), workOrderId, inspection.workReportId(),
                    inspection.sampleQty(), "CREATED", null, null, null, null,
                    null, null, null, null, null, null);
            long id = service.createInspection(payload);
            return ApiResponse.ok(id);
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

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

    @POST
    @Path("/{id}/assign")
    public ApiResponse<Boolean> assign(@PathParam("id") long id, @QueryParam("inspectorId") long inspectorId) {
        try {
            return ApiResponse.ok(service.assignInspection(id, inspectorId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/submit")
    public ApiResponse<Boolean> submit(@PathParam("id") long id, QualitySubmitResult result,
            @Context ContainerRequestContext context) {
        try {
            if (result == null || result.result() == null || result.result().isBlank()) {
                throw new BadRequestException("质检结果不能为空");
            }
            return ApiResponse.ok(service.submitInspection(id, AuthFilter.currentUser(context).user.userId,
                    result.result(), result.note()));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

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

    public record QualityJudgement(String status, String result) {
    }

    public record QualitySubmitResult(String result, String note) {
    }
}
