package com.example.messystem.warehouse.resource;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.security.DataScopeService;
import com.example.messystem.warehouse.entity.MesMaterialRequisition;
import com.example.messystem.warehouse.service.WarehouseService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Path("/requisitions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaterialRequisitionResource {
    private final WarehouseService service = new WarehouseService();
    private final DataScopeService dataScopeService = new DataScopeService();

    @GET
    public Response list() {
        return ResourceSupport.ok(service.listRequisitions());
    }

    @GET
    @Path("/by-work-order/{workOrderId}")
    public Response listByWorkOrder(@PathParam("workOrderId") long workOrderId) {
        try {
            return ResourceSupport.ok(service.listRequisitionsByWorkOrder(workOrderId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getRequisition(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    public Response create(MesMaterialRequisition requisition, @Context ContainerRequestContext context) {
        try {
            if (requisition.warehouseId == null || requisition.warehouseId <= 0) {
                throw new BadRequestException("仓库ID不能为空");
            }
            var user = AuthFilter.currentUser(context);
            dataScopeService.snapshot(user).requireWorkOrder(requisition.workOrderId);
            if (user.hasRole("PRODUCTION_OPERATOR")) {
                requireOperatorWorkOrder(requisition.workOrderId, user.user.userId);
            }
            requisition.requestedBy = user.user.userId;
            return ResourceSupport.created("领料单已创建", service.createRequisition(requisition));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/approve")
    public Response approve(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            return ResourceSupport.action("领料单已审核通过",
                    service.approveRequisition(id, AuthFilter.currentUser(context).user.userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    private static void requireOperatorWorkOrder(long workOrderId, long userId) {
        String sql = """
                select 1 from mes_work_order
                where work_order_id = ?
                  and (assigned_to = ? or accepted_by = ?)
                """;
        try (var connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, workOrderId);
            statement.setLong(2, userId);
            statement.setLong(3, userId);
            try (var rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new BadRequestException("只能为本人被派或已接收工单创建领料申请");
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }
}
