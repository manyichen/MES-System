package com.example.messystem.warehouse.resource;

import com.example.messystem.common.ResourceSupport;
import com.example.messystem.warehouse.entity.MesWarehouseLocation;
import com.example.messystem.warehouse.service.WarehouseService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/warehouse-locations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WarehouseLocationResource {
    private final WarehouseService service = new WarehouseService();

    @GET
    public Response list() {
        return ResourceSupport.ok(service.listLocations());
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getLocation(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    public Response create(MesWarehouseLocation location) {
        try {
            return ResourceSupport.created("location created", service.createLocation(location));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") long id, MesWarehouseLocation location) {
        try {
            return ResourceSupport.action("location updated", service.updateLocation(id, location));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        try {
            service.deleteLocation(id);
            return ResourceSupport.action("location deleted", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
