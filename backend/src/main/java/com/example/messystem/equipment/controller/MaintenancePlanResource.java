package com.example.messystem.equipment.controller;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.equipment.entity.MesMaintenancePlan;
import com.example.messystem.equipment.service.EquipmentService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;

/** 承载 /maintenance-plans 维护计划接口契约的 JAX-RS 控制器。 */
@Path("/maintenance-plans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaintenancePlanResource {

    private final EquipmentService service = new EquipmentService();

    @GET
    public ApiResponse<List<MesMaintenancePlan>> list() {
        try {
            return ApiResponse.ok(service.listMaintenancePlans());
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    public ApiResponse<Long> create(MesMaintenancePlan plan) {
        try {
            if (plan == null) {
                throw new BadRequestException("Maintenance plan body is required");
            }
            return ApiResponse.ok(service.createMaintenancePlan(plan));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
