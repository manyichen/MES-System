package com.example.messystem.planning.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.planning.entity.ReworkPlanningRequest;
import com.example.messystem.planning.service.ReworkPlanningService;
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

/** 承载 PMC 返工重排的规范计划接口。 */
@Path("/planning/reworks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReworkPlanningResource {
    private final ReworkPlanningService service = new ReworkPlanningService();

    @GET
    public Response list() {
        return ResourceSupport.ok(service.listReworkDemands());
    }

    @POST
    @Path("/{id}/tasks")
    public Response plan(@PathParam("id") long id, ReworkPlanningRequest request,
            @Context ContainerRequestContext context) {
        try {
            long plannerId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.created("返工生产任务已创建", service.plan(id, request, plannerId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
