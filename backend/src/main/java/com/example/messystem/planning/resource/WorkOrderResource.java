package com.example.messystem.planning.resource;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.planning.entity.MesWorkOrder;
import com.example.messystem.planning.service.WorkOrderService;
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

@Path("/work-orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkOrderResource {
    private final WorkOrderService service = new WorkOrderService();

    @GET
    public Response list(@Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        return ResourceSupport.ok(user.hasRole("PRODUCTION_OPERATOR")
                ? service.listWorkOrdersForOperator(user.user.userId)
                : service.listWorkOrders());
    }

    @POST
    public Response create(MesWorkOrder workOrder, @Context ContainerRequestContext context) {
        try {
            Long actorId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.created("work order created", service.createWorkOrder(workOrder, actorId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            return ResourceSupport.ok(user.hasRole("PRODUCTION_OPERATOR")
                    ? service.getWorkOrderForOperator(id, user.user.userId)
                    : service.getWorkOrder(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/dispatch")
    public Response dispatch(@PathParam("id") long id, @QueryParam("operatorId") Long operatorId,
            @Context ContainerRequestContext context) {
        try {
            Long actorId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.action("work order dispatched", service.dispatch(id, operatorId, actorId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/receive")
    public Response receive(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            return ResourceSupport.action("work order received",
                    service.receive(id, AuthFilter.currentUser(context).user.userId));
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
            return ResourceSupport.ok(service.listLogs(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
