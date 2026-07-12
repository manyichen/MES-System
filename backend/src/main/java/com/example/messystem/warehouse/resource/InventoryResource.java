package com.example.messystem.warehouse.resource;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.security.DataScopeService;
import com.example.messystem.warehouse.entity.MesInventory;
import com.example.messystem.warehouse.entity.MesInventoryTransaction;
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

@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {
    private final WarehouseService service = new WarehouseService();
    private final DataScopeService dataScopeService = new DataScopeService();

    @GET
    public Response list() {
        return ResourceSupport.ok(service.listInventory());
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getInventory(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/material/{materialId}")
    public Response listByMaterial(@PathParam("materialId") long materialId) {
        try {
            return ResourceSupport.ok(service.listInventoryByMaterial(materialId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    public Response create(MesInventory inventory, @Context ContainerRequestContext context) {
        try {
            var scope = dataScopeService.snapshot(AuthFilter.currentUser(context));
            scope.requireWarehouse(inventory.warehouseId);
            scope.requireWarehouseEntity("location", inventory.locationId);
            return ResourceSupport.created("库存记录已创建", service.createInventory(inventory));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") long id, MesInventory inventory,
            @Context ContainerRequestContext context) {
        try {
            var scope = dataScopeService.snapshot(AuthFilter.currentUser(context));
            if (inventory.warehouseId != null) scope.requireWarehouse(inventory.warehouseId);
            if (inventory.locationId != null) scope.requireWarehouseEntity("location", inventory.locationId);
            return ResourceSupport.action("库存记录已更新", service.updateInventory(id, inventory));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        try {
            service.deleteInventory(id);
            return ResourceSupport.action("库存记录已删除", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/transactions")
    public Response listTransactions() {
        return ResourceSupport.ok(service.listTransactions());
    }

    @GET
    @Path("/transactions/{id}")
    public Response getTransaction(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getTransaction(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/transactions")
    public Response createTransaction(MesInventoryTransaction transaction, @Context ContainerRequestContext context) {
        try {
            if (transaction.inventoryId != null) {
                dataScopeService.snapshot(AuthFilter.currentUser(context))
                        .requireWarehouseEntity("inventory", transaction.inventoryId);
            }
            transaction.operatorId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.created("库存流水已创建", service.createTransaction(transaction));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
