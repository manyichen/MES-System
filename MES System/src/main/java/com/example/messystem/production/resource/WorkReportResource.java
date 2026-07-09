package com.example.messystem.production.resource;

import com.example.messystem.common.ResourceSupport;
import com.example.messystem.production.entity.MesWorkReport;
import com.example.messystem.production.service.ProductionService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/work-reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkReportResource {
    private final ProductionService service = new ProductionService();

    @GET
    public Response list() {
        return ResourceSupport.ok(service.listWorkReports());
    }

    @GET
    @Path("/by-work-order/{workOrderId}")
    public Response listByWorkOrder(@PathParam("workOrderId") long workOrderId) {
        try {
            return ResourceSupport.ok(service.listWorkReportsByWorkOrder(workOrderId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getWorkReport(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    public Response create(MesWorkReport report) {
        try {
            return ResourceSupport.created("work report submitted", service.createWorkReport(report));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/approve")
    public Response approve(@PathParam("id") long id) {
        try {
            return ResourceSupport.action("work report approved", service.approveWorkReport(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
