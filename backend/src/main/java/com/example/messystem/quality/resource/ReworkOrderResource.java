package com.example.messystem.quality.resource;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.quality.entity.MesReworkOrder;
import com.example.messystem.quality.service.ReworkOrderService;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.sql.SQLException;

@Path("/rework-orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReworkOrderResource {

    private final ReworkOrderService service = new ReworkOrderService();

    @POST
    public ApiResponse<Long> createReworkOrder(MesReworkOrder order) {
        try {
            if (order == null) {
                throw new BadRequestException("返工单信息不能为空");
            }
            return ApiResponse.ok(service.createReworkOrder(order));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    @Path("/{id}")
    public ApiResponse<MesReworkOrder> getReworkOrder(@PathParam("id") long id) {
        try {
            return service.getReworkOrder(id)
                    .map(ApiResponse::ok)
                    .orElseGet(() -> ApiResponse.fail("Rework order not found"));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GET
    public ApiResponse<java.util.List<MesReworkOrder>> listByInspection(@QueryParam("inspectionId") Long inspectionId) {
        try {
            if (inspectionId == null) {
                return ApiResponse.ok(service.listReworkOrders());
            }
            return ApiResponse.ok(service.listReworkOrdersByInspection(inspectionId));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/dispatch")
    public ApiResponse<Boolean> dispatch(@PathParam("id") long id) {
        try {
            return ApiResponse.ok(service.dispatch(id));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @POST
    @Path("/{id}/finish")
    public ApiResponse<Boolean> finish(@PathParam("id") long id) {
        try {
            return ApiResponse.ok(service.finish(id));
        } catch (SQLException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
