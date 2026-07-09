package com.example.messystem.planning.resource;

import com.example.messystem.common.ResourceSupport;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.service.KittingService;
import com.example.messystem.planning.service.ProductionTaskService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/production-tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductionTaskResource {
    private final ProductionTaskService service = new ProductionTaskService();
    private final KittingService kittingService = new KittingService();

    @GET
    public Response list() {
        return ResourceSupport.ok(service.listTasks());
    }

    @POST
    public Response create(MesProductionTask task) {
        try {
            return ResourceSupport.created("production task created", service.createTask(task));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/kitting")
    public Response analyzeKitting(@PathParam("id") long id) {
        try {
            return ResourceSupport.action("kitting analysis finished", kittingService.analyze(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/release")
    public Response release(@PathParam("id") long id) {
        try {
            return ResourceSupport.action("production task released", service.releaseTask(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/kitting-analyses")
    public Response listAnalyses() {
        return ResourceSupport.ok(kittingService.listAnalyses());
    }

    @GET
    @Path("/shortage-alerts")
    public Response listAlerts() {
        return ResourceSupport.ok(kittingService.listAlerts());
    }
}
