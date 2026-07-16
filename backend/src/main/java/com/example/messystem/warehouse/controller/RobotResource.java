package com.example.messystem.warehouse.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.security.service.DataScopeService;
import com.example.messystem.warehouse.entity.MesRobot;
import com.example.messystem.warehouse.service.WarehouseService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

/** 承载 /robots 仓储机器人接口契约的 JAX-RS 控制器。 */
@Path("/robots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RobotResource {
    private final WarehouseService service = new WarehouseService();
    private final DataScopeService dataScopeService = new DataScopeService();

    @GET
    public Response list() {
        return ResourceSupport.ok(service.listRobots());
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getRobot(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    public Response create(MesRobot robot, @Context ContainerRequestContext context) {
        try {
            dataScopeService.snapshot(AuthFilter.currentUser(context)).requireWarehouse(robot.warehouseId);
            return ResourceSupport.created("仓储机器人已创建", service.createRobot(robot));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") long id, MesRobot robot,
            @Context ContainerRequestContext context) {
        try {
            if (robot.warehouseId != null) {
                dataScopeService.snapshot(AuthFilter.currentUser(context)).requireWarehouse(robot.warehouseId);
            }
            return ResourceSupport.action("仓储机器人已更新", service.updateRobot(id, robot));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        try {
            service.deleteRobot(id);
            return ResourceSupport.action("仓储机器人已删除", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
