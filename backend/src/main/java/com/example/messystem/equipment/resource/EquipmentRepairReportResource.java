package com.example.messystem.equipment.resource;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.security.DataScopeService;
import com.example.messystem.equipment.entity.MesEquipmentRepairReport;
import com.example.messystem.equipment.service.EquipmentService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

@Path("/equipment-repair-reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EquipmentRepairReportResource {

    private final EquipmentService service = new EquipmentService();
    private final DataScopeService dataScopeService = new DataScopeService();

    @GET
    public ApiResponse<List<MesEquipmentRepairReport>> list() {
        try {
            return ApiResponse.ok(service.listRepairReports());
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    public ApiResponse<Long> create(MesEquipmentRepairReport report, @Context ContainerRequestContext context) {
        try {
            if (report == null) {
                throw new BadRequestException("Repair report body is required");
            }
            dataScopeService.snapshot(AuthFilter.currentUser(context)).requireEquipment(report.equipmentId());
            MesEquipmentRepairReport payload = new MesEquipmentRepairReport(
                    report.repairReportId(), report.repairReportNo(), report.equipmentId(),
                    report.workOrderId(), report.faultLevel(), report.faultDesc(),
                    AuthFilter.currentUser(context).user.userId, report.reportTime(), report.repairStatus());
            return ApiResponse.ok(service.createRepairReport(payload));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/approve")
    public ApiResponse<Boolean> approve(@PathParam("id") long id) {
        try {
            return ApiResponse.ok(service.approveRepairReport(id));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/to-maintenance-order")
    public ApiResponse<Long> toMaintenanceOrder(@PathParam("id") long id) {
        try {
            return ApiResponse.ok(service.convertRepairReportToMaintenanceOrder(id));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
