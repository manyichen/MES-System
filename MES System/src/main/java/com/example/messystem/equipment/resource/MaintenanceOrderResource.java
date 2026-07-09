package com.example.messystem.equipment.resource;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.equipment.entity.MesMaintenanceOrder;
import com.example.messystem.equipment.service.EquipmentService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;

@Path("/maintenance-orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaintenanceOrderResource {

    private final EquipmentService service = new EquipmentService();

    @GET
    public ApiResponse<List<MesMaintenanceOrder>> list() {
        try {
            return ApiResponse.ok(service.listMaintenanceOrders());
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/assign")
    public ApiResponse<Boolean> assign(@PathParam("id") long id) {
        try {
            return ApiResponse.ok(service.updateMaintenanceOrderStatus(id, "ASSIGNED"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/finish")
    public ApiResponse<Boolean> finish(@PathParam("id") long id) {
        try {
            return ApiResponse.ok(service.updateMaintenanceOrderStatus(id, "FINISHED"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/accept")
    public ApiResponse<Boolean> accept(@PathParam("id") long id) {
        try {
            return ApiResponse.ok(service.updateMaintenanceOrderStatus(id, "ACCEPTED"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
