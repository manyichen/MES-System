package com.example.messystem.equipment.resource;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.equipment.entity.MesEquipment;
import com.example.messystem.equipment.entity.MesEquipmentRepairReport;
import com.example.messystem.equipment.entity.MesMaintenanceOrder;
import com.example.messystem.equipment.entity.MesMaintenancePlan;
import com.example.messystem.equipment.service.EquipmentService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

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

    @POST
    @Path("/{id}/status")
    public ApiResponse<Boolean> postStatus(@PathParam("id") long id, StatusUpdate statusUpdate) {
        return updateStatus(id, statusUpdate);
    }

    @POST
    @Path("/{equipmentId}/repair-reports")
    public ApiResponse<Long> createRepairReport(@PathParam("equipmentId") long equipmentId,
            MesEquipmentRepairReport report, @Context ContainerRequestContext context) {
        try {
            if (report == null) {
                throw new BadRequestException("Repair report body is required");
            }
            MesEquipmentRepairReport payload = new MesEquipmentRepairReport(
                    null,
                    report.repairReportNo(),
                    equipmentId,
                    report.workOrderId(),
                    report.faultLevel(),
                    report.faultDesc(),
                    AuthFilter.currentUser(context).user.userId,
                    report.reportTime(),
                    report.repairStatus()
            );
            return ApiResponse.ok(service.createRepairReport(payload));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/{equipmentId}/repair-reports")
    public ApiResponse<List<MesEquipmentRepairReport>> listRepairReports(@PathParam("equipmentId") long equipmentId) {
        try {
            return ApiResponse.ok(service.listRepairReportsForEquipment(equipmentId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{equipmentId}/maintenance-orders")
    public ApiResponse<Long> createMaintenanceOrder(@PathParam("equipmentId") long equipmentId, MesMaintenanceOrder order) {
        try {
            if (order == null) {
                throw new BadRequestException("Maintenance order body is required");
            }
            MesMaintenanceOrder payload = new MesMaintenanceOrder(
                    null,
                    order.maintenanceOrderNo(),
                    order.repairReportId(),
                    equipmentId,
                    order.maintainerId(),
                    order.maintenanceStatus(),
                    order.dispatchTime(),
                    order.finishTime(),
                    order.resultDesc()
            );
            return ApiResponse.ok(service.createMaintenanceOrder(payload));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/maintenance-orders/{id}")
    public ApiResponse<MesMaintenanceOrder> getMaintenanceOrder(@PathParam("id") long id) {
        try {
            return service.getMaintenanceOrder(id)
                    .map(ApiResponse::ok)
                    .orElseGet(() -> ApiResponse.fail("Maintenance order not found"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/maintenance-plans")
    public ApiResponse<Long> createMaintenancePlan(MesMaintenancePlan plan) {
        try {
            if (plan == null) {
                throw new BadRequestException("Maintenance plan body is required");
            }
            return ApiResponse.ok(service.createMaintenancePlan(plan));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/{equipmentId}/maintenance-plans")
    public ApiResponse<List<MesMaintenancePlan>> listMaintenancePlans(@PathParam("equipmentId") long equipmentId) {
        try {
            return ApiResponse.ok(service.listMaintenancePlansForEquipment(equipmentId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    public record StatusUpdate(String status) {
    }
}
