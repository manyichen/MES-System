package com.example.messystem.master.controller;

import com.example.messystem.common.ResourceSupport;
import com.example.messystem.master.entity.MesProcessRoute;
import com.example.messystem.master.entity.MesProduct;
import com.example.messystem.master.entity.MesProductBom;
import com.example.messystem.master.entity.MesProductionLine;
import com.example.messystem.master.service.MasterDataService;
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

/** 承载产品、产线和工艺等主数据接口契约的 JAX-RS 控制器。 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MasterDataResource {
    private final MasterDataService service = new MasterDataService();

    @GET
    @Path("/products")
    public Response listProducts() {
        return ResourceSupport.ok(service.listProducts());
    }

    @POST
    @Path("/products")
    public Response createProduct(MesProduct product) {
        try {
            return ResourceSupport.created("product created", service.createProduct(product));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/products/{id}/bom")
    public Response listBom(@PathParam("id") long productId) {
        try {
            return ResourceSupport.ok(service.listBom(productId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/products/{id}/bom")
    public Response createBom(@PathParam("id") long productId, MesProductBom bom) {
        try {
            return ResourceSupport.created("bom item created", service.createBom(productId, bom));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/process-routes")
    public Response listProcessRoutes() {
        return ResourceSupport.ok(service.listProcessRoutes());
    }

    @POST
    @Path("/process-routes")
    public Response createProcessRoute(MesProcessRoute route) {
        try {
            return ResourceSupport.created("process route created", service.createProcessRoute(route));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @PUT
    @Path("/process-routes/{id}")
    public Response updateProcessRoute(@PathParam("id") long id, MesProcessRoute route) {
        try {
            return ResourceSupport.action("process route updated", service.updateProcessRoute(id, route));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @DELETE
    @Path("/process-routes/{id}")
    public Response deleteProcessRoute(@PathParam("id") long id) {
        try {
            service.deleteProcessRoute(id);
            return ResourceSupport.action("process route deleted", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/production-lines")
    public Response listProductionLines() {
        return ResourceSupport.ok(service.listProductionLines());
    }

    @POST
    @Path("/production-lines")
    public Response createProductionLine(MesProductionLine line) {
        try {
            return ResourceSupport.created("production line created", service.createProductionLine(line));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/sync-logs")
    public Response listSyncLogs() {
        return ResourceSupport.ok(service.listSyncLogs());
    }
}
