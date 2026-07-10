package com.example.messystem.planning.resource;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/work-orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkOrderResource {
    private final WorkOrderService service = new WorkOrderService();

    @GET
    public Response list() {
        return ResourceSupport.ok(service.listWorkOrders());
    }

    @POST
    public Response create(MesWorkOrder workOrder) {
        try {
            return ResourceSupport.created("work order created", service.createWorkOrder(workOrder));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getWorkOrder(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/dispatch")
    public Response dispatch(@PathParam("id") long id, @QueryParam("operatorId") Long operatorId) {
        try {
            return ResourceSupport.action("work order dispatched", service.dispatch(id, operatorId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/receive")
    public Response receive(@PathParam("id") long id, @QueryParam("operatorId") Long operatorId) {
        try {
            return ResourceSupport.action("work order received", service.receive(id, operatorId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/{id}/logs")
    public Response logs(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.listLogs(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
