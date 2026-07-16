package com.example.messystem.warehouse.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.planning.service.WorkOrderService;
import com.example.messystem.security.service.DataScopeService;
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
import java.util.Map;

/** 承载 /requisitions 领料申请接口契约的 JAX-RS 控制器。 */
@Path("/requisitions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaterialRequisitionResource {
    private final WarehouseService service = new WarehouseService();
    private final WorkOrderService workOrderService = new WorkOrderService();
    private final DataScopeService dataScopeService = new DataScopeService();

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

    @GET
    @Path("/create-options")
    public Response createOptions(@Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            var workOrders = user.hasRole("PRODUCTION_OPERATOR")
                    ? workOrderService.listWorkOrdersForOperator(user.user.userId)
                    : workOrderService.listWorkOrders();
            return ResourceSupport.ok(Map.of(
                    "workOrders", workOrders.stream()
                            .filter(item -> item.workOrderStatus != null
                                    && ("DISPATCHED".equals(item.workOrderStatus)
                                    || "RECEIVED".equals(item.workOrderStatus)
                                    || "RUNNING".equals(item.workOrderStatus)))
                            .toList(),
                    "materials", service.listMaterials(),
                    "warehouses", service.listWarehouses(),
                    "inventory", service.listInventory()
            ));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

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
            if (user == null || !user.hasRole("PRODUCTION_OPERATOR")) {
                throw new BadRequestException("只有生产操作工可以发起领料申请");
            }
            dataScopeService.snapshot(user).requireWorkOrder(requisition.workOrderId);
            return ResourceSupport.created("领料单已创建",
                    service.createOperatorRequisition(requisition, user.user.userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

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

    private static void requireWarehouseAdmin(AuthenticatedUser user) {
        if (user == null || !user.hasRole("WAREHOUSE_ADMIN")) {
            throw new BadRequestException("only warehouse admins can receive or approve requisitions");
        }
    }

}
