package com.example.messystem.equipment.controller;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.equipment.entity.MesEquipment;
import com.example.messystem.equipment.service.EquipmentService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;

/** 承载 /equipment 设备台账接口契约的 JAX-RS 控制器。 */
@Path("/equipment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EquipmentResource {

    private final EquipmentService service = new EquipmentService();

    @GET
    public ApiResponse<List<MesEquipment>> listEquipment() {
        try {
            return ApiResponse.ok(service.listEquipment());
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/{id}")
    public ApiResponse<MesEquipment> getEquipment(@PathParam("id") long id) {
        try {
            return service.getEquipmentById(id)
                    .map(ApiResponse::ok)
                    .orElseGet(() -> ApiResponse.fail("Equipment not found"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/by-line/{lineId}")
    public ApiResponse<List<MesEquipment>> listByLine(@PathParam("lineId") long lineId) {
        try {
            return ApiResponse.ok(service.listEquipmentByLine(lineId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    public ApiResponse<Long> createEquipment(MesEquipment equipment) {
        try {
            if (equipment == null) {
                throw new BadRequestException("Equipment body is required");
            }
            return ApiResponse.ok(service.createEquipment(equipment));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @PUT
    @Path("/{id}/status")
    public ApiResponse<Boolean> updateStatus(@PathParam("id") long id, StatusUpdate statusUpdate) {
        try {
            if (statusUpdate == null || statusUpdate.status() == null) {
                throw new BadRequestException("Status value is required");
            }
            return ApiResponse.ok(service.updateEquipmentStatus(id, statusUpdate.status()));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    public record StatusUpdate(String status) {
    }
}
