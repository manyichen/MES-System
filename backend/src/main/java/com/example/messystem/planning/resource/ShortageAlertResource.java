package com.example.messystem.planning.resource;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.planning.service.KittingService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

@Path("/shortage-alerts")
@Produces(MediaType.APPLICATION_JSON)
public class ShortageAlertResource {
    private final KittingService service = new KittingService();

    @GET
    public Response list() { return ResourceSupport.ok(service.listAlerts()); }

    @POST
    @Path("/{id}/accept")
    public Response accept(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            Long userId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.action("缺料预警已接收，请完成实际备料后通知 PMC 复核", service.acceptShortageAlert(id, userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
