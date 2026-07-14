package com.example.messystem.planning.resource;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.planning.entity.MesProductionTask;
import com.example.messystem.planning.service.KittingService;
import com.example.messystem.planning.service.ProductionTaskService;
import com.example.messystem.security.DataScopeService;
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

@Path("/production-tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductionTaskResource {
    private final ProductionTaskService service = new ProductionTaskService();
    private final KittingService kittingService = new KittingService();
    private final DataScopeService dataScopeService = new DataScopeService();

    @GET
    public Response list(@Context ContainerRequestContext context) {
        var scope = dataScopeService.snapshot(AuthFilter.currentUser(context));
        return ResourceSupport.ok(service.listTasks().stream().filter(scope::canView).toList());
    }

    @POST
    public Response create(MesProductionTask task, @Context ContainerRequestContext context) {
        try {
            task.plannerId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.created("生产任务已创建", service.createTask(task));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/kitting")
    public Response analyzeKitting(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            dataScopeService.snapshot(AuthFilter.currentUser(context)).requireTask(id);
            return ResourceSupport.action("齐套分析已完成", kittingService.analyze(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/release")
    public Response release(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            dataScopeService.snapshot(AuthFilter.currentUser(context)).requireTask(id);
            return ResourceSupport.action("生产任务已发布", service.releaseTask(id));
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
