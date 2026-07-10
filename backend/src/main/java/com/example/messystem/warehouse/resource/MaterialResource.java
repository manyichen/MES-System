package com.example.messystem.warehouse.resource;

import com.example.messystem.common.ResourceSupport;
import com.example.messystem.warehouse.entity.MesMaterial;
import com.example.messystem.warehouse.service.WarehouseService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/materials")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaterialResource {
    private final WarehouseService service = new WarehouseService();

    @GET
    public Response list() {
        return ResourceSupport.ok(service.listMaterials());
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getMaterial(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    public Response create(MesMaterial material) {
        try {
            return ResourceSupport.created("material created", service.createMaterial(material));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") long id, MesMaterial material) {
        try {
            return ResourceSupport.action("material updated", service.updateMaterial(id, material));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        try {
            service.deleteMaterial(id);
            return ResourceSupport.action("material deleted", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
