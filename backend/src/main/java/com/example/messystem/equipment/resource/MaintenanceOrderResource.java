package com.example.messystem.equipment.resource;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.equipment.entity.MesMaintenanceOrder;
import com.example.messystem.equipment.service.EquipmentService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

@Path("/maintenance-orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaintenanceOrderResource {

    private final EquipmentService service = new EquipmentService();

    @GET
    public ApiResponse<List<MesMaintenanceOrder>> list(@Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            return ApiResponse.ok(user.hasRole("EQUIPMENT_MAINTAINER")
                    ? service.listMaintenanceOrdersForMaintainer(user.user.userId)
                    : service.listMaintenanceOrders());
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/assign")
    public ApiResponse<Boolean> assign(@PathParam("id") long id, @QueryParam("maintainerId") long maintainerId) {
        try {
            return ApiResponse.ok(service.assignMaintenanceOrder(id, maintainerId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/finish")
    public ApiResponse<Boolean> finish(@PathParam("id") long id, MesMaintenanceOrder order,
            @Context ContainerRequestContext context) {
        try {
            return ApiResponse.ok(service.finishMaintenanceOrder(id,
                    AuthFilter.currentUser(context).user.userId,
                    order == null ? "" : order.resultDesc()));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/accept")
    public ApiResponse<Boolean> accept(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            return ApiResponse.ok(service.acceptMaintenanceOrder(id,
                    AuthFilter.currentUser(context).user.userId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
