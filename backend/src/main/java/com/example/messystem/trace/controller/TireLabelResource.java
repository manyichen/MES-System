/*
 * 答辩定位：轮胎标签与公开追溯 模块的 TireLabelResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.trace.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.ApiResponse;
import com.example.messystem.trace.entity.TireGenerationRequest;
import com.example.messystem.trace.entity.TireGenerationResult;
import com.example.messystem.trace.entity.TirePrintRequest;
import com.example.messystem.trace.entity.TireTraceItem;
import com.example.messystem.trace.service.TireTraceService;
import com.example.messystem.quality.service.QualityInspectionService;
import com.example.messystem.security.service.DataScopeService;
import com.example.messystem.warehouse.service.WarehouseService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;

/** 承载 /tire-labels 轮胎标签接口契约的 JAX-RS 控制器。 */
@Path("/tire-labels")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TireLabelResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final TireTraceService service = new TireTraceService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final QualityInspectionService qualityService = new QualityInspectionService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final WarehouseService warehouseService = new WarehouseService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final DataScopeService dataScopeService = new DataScopeService();

    /**
     * 接口：GET /api/tire-labels。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public ApiResponse<List<TireTraceItem>> list(@Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        var scope = dataScopeService.snapshot(user);
        return ApiResponse.ok(service.list().stream().filter(scope::canView).toList());
    }

    /**
     * 接口：GET /api/tire-labels/generate-options。
     * 用例：生成业务结果；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/generate-options")
    public ApiResponse<Map<String, Object>> generateOptions(@Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            var scope = dataScopeService.snapshot(user);
            var inspections = qualityService.listInspections().stream()
                    .filter(item -> "APPROVED".equals(item.inspectionStatus())
                            && "PASS".equals(item.judgementResult()))
                    .filter(scope::canView)
                    .toList();
            var warehouses = warehouseService.listWarehouses().stream()
                    .filter(scope::canView)
                    .toList();
            var warehouseIds = warehouses.stream().map(item -> item.warehouseId).collect(java.util.stream.Collectors.toSet());
            var locations = warehouseService.listLocations().stream()
                    .filter(item -> warehouseIds.contains(item.warehouseId))
                    .toList();
            return ApiResponse.ok(Map.of(
                    "inspections", inspections,
                    "warehouses", warehouses,
                    "locations", locations));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * 接口：POST /api/tire-labels/generate。
     * 用例：生成业务结果；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/generate")
    public ApiResponse<TireGenerationResult> generate(TireGenerationRequest request,
            @Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        requireWarehouseAccess(user, request == null ? null : request.warehouseId());
        long userId = user.user.userId;
        return ApiResponse.ok("单条轮胎二维码已生成", service.generate(request, userId));
    }

    /**
     * 接口：POST /api/tire-labels/{id}/print。
     * 用例：执行 recordPrint 对应的业务步骤；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/print")
    public ApiResponse<Void> recordPrint(@PathParam("id") long tireId, TirePrintRequest request,
            @Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        requireWarehouseAccess(user, service.requireById(tireId).warehouseId());
        long userId = user.user.userId;
        service.recordPrint(tireId, userId, request == null ? null : request.remark());
        return ApiResponse.ok("打印记录已保存", null);
    }

    /**
     * 接口：GET /api/tire-labels/{id}/qrcode。
     * 用例：读取二维码文件；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}/qrcode")
    @Produces("image/png")
    public Response qrCode(@PathParam("id") long tireId, @Context ContainerRequestContext context) {
        requireVisible(AuthFilter.currentUser(context), tireId);
        return file(service.qrCode(tireId), "image/png", "qrcode.png", false);
    }

    /**
     * 接口：GET /api/tire-labels/{id}/label。
     * 用例：读取标签图片；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}/label")
    @Produces("image/png")
    public Response label(@PathParam("id") long tireId, @Context ContainerRequestContext context) {
        requireVisible(AuthFilter.currentUser(context), tireId);
        return file(service.document(tireId, "LABEL_PNG"), "image/png", "tire-label.png", false);
    }

    /**
     * 接口：GET /api/tire-labels/{id}/document。
     * 用例：执行 document 对应的业务步骤；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}/document")
    @Produces("application/pdf")
    public Response document(@PathParam("id") long tireId, @Context ContainerRequestContext context) {
        requireVisible(AuthFilter.currentUser(context), tireId);
        return file(service.document(tireId, "PDF"), "application/pdf", "product-info.pdf", false);
    }

    static Response file(java.nio.file.Path path, String mediaType, String name, boolean attachment) {
        StreamingOutput stream = output -> Files.copy(path, output);
        String disposition = (attachment ? "attachment" : "inline") + "; filename=\"" + name + "\"";
        try {
            return Response.ok(stream, mediaType)
                    .header("Content-Disposition", disposition)
                    .header("Content-Length", Files.size(path))
                    .header("Cache-Control", "private, max-age=300")
                    .build();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("读取追溯文件失败", exception);
        }
    }

    /**
     * 内部实现步骤：执行 isWarehouseRole 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private boolean isWarehouseRole(AuthenticatedUser user) {
        return user.hasRole("WAREHOUSE_ADMIN");
    }

    /**
     * 内部实现步骤：执行 requireWarehouseAccess 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private void requireWarehouseAccess(AuthenticatedUser user, Long warehouseId) {
        if (user.hasRole("SYSTEM_ADMIN") || user.isSuperAdmin()) return;
        if (!isWarehouseRole(user) || warehouseId == null || !user.warehouseIds.contains(warehouseId)) {
            throw new BadRequestException("无权为该仓库生成或打印轮胎二维码标签");
        }
    }

    /**
     * 内部实现步骤：执行 requireVisible 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private TireTraceItem requireVisible(AuthenticatedUser user, long tireId) {
        TireTraceItem tire = service.requireById(tireId);
        var scope = dataScopeService.snapshot(user);
        if (scope.lineRestricted()) {
            if (tire.workOrderId() == null) throw new BadRequestException("轮胎未关联制造工单，无法校验数据权限");
            scope.requireWorkOrder(tire.workOrderId());
        }
        if (scope.warehouseRestricted()) {
            if (tire.warehouseId() == null) throw new BadRequestException("轮胎未关联仓库，无法校验数据权限");
            scope.requireWarehouse(tire.warehouseId());
        }
        return tire;
    }
}
