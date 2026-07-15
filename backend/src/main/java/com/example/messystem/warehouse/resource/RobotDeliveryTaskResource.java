package com.example.messystem.warehouse.resource;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.security.DataScopeService;
import com.example.messystem.warehouse.entity.MesRobotDeliveryTask;
import com.example.messystem.warehouse.service.WarehouseService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

@Path("/robot-delivery-tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RobotDeliveryTaskResource {
    private final WarehouseService service = new WarehouseService();
    private final DataScopeService dataScopeService = new DataScopeService();

    @GET
    public Response list() {
        return ResourceSupport.ok(service.listDeliveryTasks());
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getDeliveryTask(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    public Response create(MesRobotDeliveryTask task, @Context ContainerRequestContext context) {
        try {
            dataScopeService.snapshot(AuthFilter.currentUser(context))
                    .requireWarehouseEntity("picking", task.pickingTaskId);
            return ResourceSupport.created("配送任务已创建", service.createDeliveryTask(task));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/arrive")
    public Response arrive(@PathParam("id") long id) {
        try {
            return ResourceSupport.action("配送任务已到达", service.markDeliveryArrived(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/confirm-receipt")
    public Response confirmReceipt(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (!user.hasRole("PRODUCTION_OPERATOR")) {
                throw new BadRequestException("only production operators can confirm requisition receipt");
            }
            return ResourceSupport.action("物料已确认接收", service.confirmDeliveryReceipt(id, user.user.userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
