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
    private final TireTraceService service = new TireTraceService();
    private final QualityInspectionService qualityService = new QualityInspectionService();
    private final WarehouseService warehouseService = new WarehouseService();

    @GET
    public ApiResponse<List<TireTraceItem>> list(@Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        return ApiResponse.ok(service.list(isWarehouseRole(user) ? user.warehouseIds : null));
    }

    @GET
    @Path("/generate-options")
    public ApiResponse<Map<String, Object>> generateOptions(@Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            var inspections = qualityService.listInspections().stream()
                    .filter(item -> "APPROVED".equals(item.inspectionStatus())
                            && "PASS".equals(item.judgementResult()))
                    .toList();
            var warehouses = warehouseService.listWarehouses().stream()
                    .filter(item -> !isWarehouseRole(user) || user.warehouseIds.contains(item.warehouseId))
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

    @POST
    @Path("/generate")
    public ApiResponse<TireGenerationResult> generate(TireGenerationRequest request,
            @Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        requireWarehouseAccess(user, request == null ? null : request.warehouseId());
        long userId = user.user.userId;
        return ApiResponse.ok("单条轮胎二维码已生成", service.generate(request, userId));
    }

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

    @GET
    @Path("/{id}/qrcode")
    @Produces("image/png")
    public Response qrCode(@PathParam("id") long tireId) {
        return file(service.qrCode(tireId), "image/png", "qrcode.png", false);
    }

    @GET
    @Path("/{id}/label")
    @Produces("image/png")
    public Response label(@PathParam("id") long tireId) {
        return file(service.document(tireId, "LABEL_PNG"), "image/png", "tire-label.png", false);
    }

    @GET
    @Path("/{id}/document")
    @Produces("application/pdf")
    public Response document(@PathParam("id") long tireId) {
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

    private boolean isWarehouseRole(AuthenticatedUser user) {
        return user.hasRole("WAREHOUSE_ADMIN");
    }

    private void requireWarehouseAccess(AuthenticatedUser user, Long warehouseId) {
        if (user.hasRole("SYSTEM_ADMIN") || user.isSuperAdmin()) return;
        if (!isWarehouseRole(user) || warehouseId == null || !user.warehouseIds.contains(warehouseId)) {
            throw new BadRequestException("无权为该仓库生成或打印轮胎二维码标签");
        }
    }
}
