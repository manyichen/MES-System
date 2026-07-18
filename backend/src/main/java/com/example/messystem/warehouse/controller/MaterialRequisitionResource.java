/*
 * 答辩定位：仓储、领料、拣货与机器人物流 模块的 MaterialRequisitionResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.warehouse.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.master.entity.MesProductBom;
import com.example.messystem.master.service.MasterDataService;
import com.example.messystem.planning.service.WorkOrderService;
import com.example.messystem.security.service.DataScopeService;
import com.example.messystem.warehouse.entity.MesMaterial;
import com.example.messystem.warehouse.entity.MesMaterialRequisition;
import com.example.messystem.warehouse.service.WarehouseService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 承载 /requisitions 领料申请接口契约的 JAX-RS 控制器。 */
@Path("/requisitions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaterialRequisitionResource {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final WarehouseService service = new WarehouseService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final WorkOrderService workOrderService = new WorkOrderService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final MasterDataService masterDataService = new MasterDataService();
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final DataScopeService dataScopeService = new DataScopeService();

    /**
     * 接口：GET /api/requisitions。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    public Response list(@Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        if (user.hasRole("PRODUCTION_OPERATOR")) {
            return ResourceSupport.ok(service.listRequisitionsByRequester(user.user.userId));
        }
        return ResourceSupport.ok(service.listRequisitions().stream()
                .filter(dataScopeService.snapshot(user)::canView)
                .toList());
    }

    /**
     * 接口：GET /api/requisitions/create-options。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/create-options")
    public Response createOptions(@Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            var workOrders = user.hasRole("PRODUCTION_OPERATOR")
                    ? workOrderService.listWorkOrdersForOperator(user.user.userId)
                    : workOrderService.listWorkOrders();
            var eligibleWorkOrders = workOrders.stream()
                    .filter(item -> item.workOrderStatus != null
                            && ("DISPATCHED".equals(item.workOrderStatus)
                            || "RECEIVED".equals(item.workOrderStatus)
                            || "RUNNING".equals(item.workOrderStatus)))
                    .toList();
            var materials = service.listMaterials();
            return ResourceSupport.ok(Map.of(
                    "workOrders", attachRequisitionDefaults(eligibleWorkOrders, materials),
                    "materials", materials,
                    "warehouses", service.listWarehouses(),
                    "inventory", service.listInventory()
            ));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 内部实现步骤：执行 attachRequisitionDefaults 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private List<Map<String, Object>> attachRequisitionDefaults(
            List<com.example.messystem.planning.entity.MesWorkOrder> workOrders,
            List<MesMaterial> materials) {
        Map<Long, MesMaterial> materialById = materials.stream()
                .filter(item -> item.materialId != null)
                .collect(Collectors.toMap(item -> item.materialId, item -> item, (left, right) -> left));
        Map<Long, List<MesProductBom>> bomByProduct = masterDataService.listAllBom().stream()
                .filter(item -> item.productId != null && item.materialId != null)
                .filter(item -> item.enabled == null || item.enabled == 1)
                .collect(Collectors.groupingBy(item -> item.productId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var workOrder : workOrders) {
            Map<String, Object> row = new HashMap<>();
            row.put("workOrderId", workOrder.workOrderId);
            row.put("workOrderNo", workOrder.workOrderNo);
            row.put("taskId", workOrder.taskId);
            row.put("productId", workOrder.productId);
            row.put("lineId", workOrder.lineId);
            row.put("processId", workOrder.processId);
            row.put("plannedQty", workOrder.plannedQty);
            row.put("actualQty", workOrder.actualQty);
            row.put("priorityLevel", workOrder.priorityLevel);
            row.put("workOrderStatus", workOrder.workOrderStatus);
            row.put("batchNo", workOrder.batchNo);
            row.put("assignedTo", workOrder.assignedTo);
            row.put("acceptedBy", workOrder.acceptedBy);
            row.put("dispatchTime", workOrder.dispatchTime);
            row.put("receiveTime", workOrder.receiveTime);
            row.put("completedTime", workOrder.completedTime);
            row.put("createdAt", workOrder.createdAt);
            row.put("updatedAt", workOrder.updatedAt);

            List<Map<String, Object>> items = new ArrayList<>();
            Long suggestedWarehouseId = null;
            int plannedQty = workOrder.plannedQty == null ? 0 : workOrder.plannedQty;
            for (MesProductBom bom : bomByProduct.getOrDefault(workOrder.productId, List.of())) {
                MesMaterial material = materialById.get(bom.materialId);
                BigDecimal qtyPerUnit = bom.qtyPerUnit == null ? BigDecimal.ZERO : bom.qtyPerUnit;
                BigDecimal requiredQty = qtyPerUnit.multiply(BigDecimal.valueOf(plannedQty));
                if (requiredQty.signum() <= 0) continue;
                Map<String, Object> line = new HashMap<>();
                line.put("materialId", bom.materialId);
                line.put("requiredQty", requiredQty.stripTrailingZeros());
                line.put("unit", firstNonBlank(bom.unit, material == null ? null : material.unit));
                line.put("batchNo", "");
                items.add(line);
                if (suggestedWarehouseId == null && material != null && material.defaultWarehouseId != null) {
                    suggestedWarehouseId = material.defaultWarehouseId;
                }
            }
            row.put("suggestedWarehouseId", suggestedWarehouseId);
            row.put("suggestedItems", items.isEmpty()
                    ? List.of(Map.of("materialId", "", "requiredQty", "", "unit", "", "batchNo", ""))
                    : items);
            rows.add(row);
        }
        return rows;
    }

    /**
     * 内部实现步骤：执行 firstNonBlank 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return "";
    }

    /**
     * 接口：GET /api/requisitions/by-work-order/{workOrderId}。
     * 用例：查询列表；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/by-work-order/{workOrderId}")
    public Response listByWorkOrder(@PathParam("workOrderId") long workOrderId,
            @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (user.hasRole("PRODUCTION_OPERATOR")) {
                service.requireOperatorWorkOrderAccess(workOrderId, user.user.userId);
                return ResourceSupport.ok(service.listRequisitionsByRequester(user.user.userId).stream()
                        .filter(item -> item.workOrderId != null && item.workOrderId.longValue() == workOrderId)
                        .toList());
            }
            dataScopeService.snapshot(user).requireWorkOrder(workOrderId);
            return ResourceSupport.ok(service.listRequisitionsByWorkOrder(workOrderId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：GET /api/requisitions/{id}。
     * 用例：查询单条记录或详情；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (user.hasRole("PRODUCTION_OPERATOR")) {
                return ResourceSupport.ok(service.getRequisitionForRequester(id, user.user.userId));
            }
            dataScopeService.snapshot(user).requireWarehouseEntity("requisition", id);
            return ResourceSupport.ok(service.getRequisition(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/requisitions。
     * 用例：创建业务记录；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    public Response create(MesMaterialRequisition requisition, @Context ContainerRequestContext context) {
        try {
            if (requisition.workOrderId == null || requisition.workOrderId <= 0) {
                throw new BadRequestException("生产工单ID不能为空");
            }
            if (requisition.warehouseId == null || requisition.warehouseId <= 0) {
                throw new BadRequestException("仓库ID不能为空");
            }
            var user = AuthFilter.currentUser(context);
            if (user == null || !user.canActAs("PRODUCTION_OPERATOR")) {
                throw new BadRequestException("只有生产操作工可以发起领料申请");
            }
            dataScopeService.snapshot(user).requireWorkOrder(requisition.workOrderId);
            return ResourceSupport.created("领料单已创建",
                    service.createRequisitionForRequester(
                            requisition, user.user.userId, user.isSuperAdmin()));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/requisitions/{id}/receive。
     * 用例：接收已派发任务；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/receive")
    public Response receive(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            requireWarehouseAdmin(user);
            dataScopeService.snapshot(user).requireWarehouseEntity("requisition", id);
            return ResourceSupport.action("requisition received",
                    service.receiveRequisition(id, user.user.userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/requisitions/{id}/approve。
     * 用例：审核通过业务事项；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/approve")
    public Response approve(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            requireWarehouseAdmin(user);
            dataScopeService.snapshot(user).requireWarehouseEntity("requisition", id);
            return ResourceSupport.action("领料单已审核通过",
                    service.approveRequisition(id, user.user.userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 接口：POST /api/requisitions/{id}/reject。
     * 用例：驳回业务事项；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
    @POST
    @Path("/{id}/reject")
    public Response reject(@PathParam("id") long id, MesMaterialRequisition request,
            @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            requireWarehouseAdmin(user);
            dataScopeService.snapshot(user).requireWarehouseEntity("requisition", id);
            String reason = request == null ? null : request.remark;
            return ResourceSupport.action("领料单已驳回",
                    service.rejectRequisition(id, user.user.userId, reason));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    /**
     * 内部实现步骤：执行 requireWarehouseAdmin 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static void requireWarehouseAdmin(AuthenticatedUser user) {
        if (user == null || !user.canActAs("WAREHOUSE_ADMIN")) {
            throw new BadRequestException("only warehouse admins can receive or approve requisitions");
        }
    }

}
