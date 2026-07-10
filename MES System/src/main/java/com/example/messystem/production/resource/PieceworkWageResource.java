package com.example.messystem.production.resource;

import com.example.messystem.common.ResourceSupport;
import com.example.messystem.production.service.ProductionService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/piecework-wages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PieceworkWageResource {
    private final ProductionService service = new ProductionService();

    @GET
    public Response list() {
        return ResourceSupport.ok(service.listWages());
    }

    @GET
    @Path("/by-report/{workReportId}")
    public Response listByReport(@PathParam("workReportId") long workReportId) {
        try {
            return ResourceSupport.ok(service.listWagesByReport(workReportId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getWage(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
