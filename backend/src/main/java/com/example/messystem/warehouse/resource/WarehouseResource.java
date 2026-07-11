package com.example.messystem.warehouse.resource;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.security.DataScopeService;
import com.example.messystem.warehouse.entity.MesWarehouse;
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
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

@Path("/warehouses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WarehouseResource {
    private final WarehouseService service = new WarehouseService();
    private final DataScopeService dataScopeService = new DataScopeService();

    @GET
    public Response listWarehouses() {
        return ResourceSupport.ok(service.listWarehouses());
    }

    @GET
    @Path("/{id}")
    public Response getWarehouse(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getWarehouse(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    public Response createWarehouse(MesWarehouse warehouse, @Context ContainerRequestContext context) {
        try {
            MesWarehouse created = service.createWarehouse(warehouse);
            var user = AuthFilter.currentUser(context);
            if (user.hasRole("WAREHOUSE_ADMIN")) {
                dataScopeService.assignWarehouse(user.user.userId, created.warehouseId, user.user.userId);
            }
            return ResourceSupport.created("warehouse created", created);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateWarehouse(@PathParam("id") long id, MesWarehouse warehouse) {
        try {
            return ResourceSupport.action("warehouse updated", service.updateWarehouse(id, warehouse));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteWarehouse(@PathParam("id") long id) {
        try {
            service.deleteWarehouse(id);
            return ResourceSupport.action("warehouse deleted", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/locations")
    public Response listLocations() {
        return ResourceSupport.ok(service.listLocations());
    }

    @GET
    @Path("/locations/{id}")
    public Response getLocation(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getLocation(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/locations")
    public Response createLocation(MesWarehouseLocation location, @Context ContainerRequestContext context) {
        try {
            dataScopeService.snapshot(AuthFilter.currentUser(context)).requireWarehouse(location.warehouseId);
            return ResourceSupport.created("location created", service.createLocation(location));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @PUT
    @Path("/locations/{id}")
    public Response updateLocation(@PathParam("id") long id, MesWarehouseLocation location,
            @Context ContainerRequestContext context) {
        try {
            if (location.warehouseId != null) {
                dataScopeService.snapshot(AuthFilter.currentUser(context)).requireWarehouse(location.warehouseId);
            }
            return ResourceSupport.action("location updated", service.updateLocation(id, location));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @DELETE
    @Path("/locations/{id}")
    public Response deleteLocation(@PathParam("id") long id) {
        try {
            service.deleteLocation(id);
            return ResourceSupport.action("location deleted", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
