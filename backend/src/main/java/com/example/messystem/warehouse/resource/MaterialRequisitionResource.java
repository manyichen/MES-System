package com.example.messystem.warehouse.resource;

import com.example.messystem.common.ResourceSupport;
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

@Path("/requisitions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaterialRequisitionResource {
    private final WarehouseService service = new WarehouseService();

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
    public Response create(MesMaterialRequisition requisition) {
        try {
            return ResourceSupport.created("requisition created", service.createRequisition(requisition));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/approve")
    public Response approve(@PathParam("id") long id, @QueryParam("approvedBy") Long approvedBy) {
        try {
            return ResourceSupport.action("requisition approved", service.approveRequisition(id, approvedBy));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
