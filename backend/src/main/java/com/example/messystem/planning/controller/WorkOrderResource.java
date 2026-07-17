package com.example.messystem.planning.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.planning.entity.MesWorkOrder;
import com.example.messystem.planning.service.WorkOrderService;
import com.example.messystem.security.service.DataScopeService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** 承载 /work-orders 生产工单接口契约的 JAX-RS 控制器。 */
@Path("/work-orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkOrderResource {
    private final WorkOrderService service = new WorkOrderService();
    private final DataScopeService dataScopeService = new DataScopeService();

    @GET
    public Response list(@Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        return ResourceSupport.ok(user.hasRole("PRODUCTION_OPERATOR")
                ? service.listWorkOrdersForOperator(user.user.userId)
                : service.listWorkOrders().stream()
                        .filter(dataScopeService.snapshot(user)::canView)
                        .toList());
    }

    @GET
    @Path("/operators")
    public Response operators() {
        return ResourceSupport.ok(service.listDispatchableOperators());
    }

    @POST
    public Response create(MesWorkOrder workOrder, @Context ContainerRequestContext context) {
        try {
            Long actorId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.created("生产工单已创建", service.createWorkOrder(workOrder, actorId));
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
                return ResourceSupport.ok(service.getWorkOrderForOperator(id, user.user.userId));
            }
            dataScopeService.snapshot(user).requireWorkOrder(id);
            return ResourceSupport.ok(service.getWorkOrder(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/dispatch")
    public Response dispatch(@PathParam("id") long id, @QueryParam("operatorId") Long operatorId,
            @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            dataScopeService.snapshot(user).requireWorkOrder(id);
            Long actorId = user.user.userId;
            return ResourceSupport.action("生产工单已派发", service.dispatch(id, operatorId, actorId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/receive")
    public Response receive(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (!user.canActAs("PRODUCTION_OPERATOR")) {
                throw new BadRequestException("只有被派发的生产操作工才能接收工单");
            }
            return ResourceSupport.action("生产工单已接收",
                    service.receive(id, user.user.userId, user.isSuperAdmin()));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/logs")
    public Response logs(@Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            return ResourceSupport.ok(user.hasRole("PRODUCTION_OPERATOR")
                    ? service.listLogsForOperator(user.user.userId)
                    : service.listAllLogs());
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/{id}/logs")
    public Response logs(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (user.hasRole("PRODUCTION_OPERATOR")) service.getWorkOrderForOperator(id, user.user.userId);
            else dataScopeService.snapshot(user).requireWorkOrder(id);
            return ResourceSupport.ok(service.listLogs(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
