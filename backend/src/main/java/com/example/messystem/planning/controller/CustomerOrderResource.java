package com.example.messystem.planning.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.planning.entity.MesCustomerOrder;
import com.example.messystem.planning.service.CustomerOrderService;
import com.example.messystem.security.service.DataScopeService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** 承载 /orders 客户订单接口契约的 JAX-RS 控制器。 */
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerOrderResource {
    private final CustomerOrderService service = new CustomerOrderService();
    private final DataScopeService dataScopeService = new DataScopeService();

    @GET
    public Response list(@Context ContainerRequestContext context) {
        var scope = dataScopeService.snapshot(AuthFilter.currentUser(context));
        return ResourceSupport.ok(service.listOrders().stream().filter(scope::canView).toList());
    }

    @POST
    public Response create(MesCustomerOrder order) {
        try {
            return ResourceSupport.created("客户订单已创建", service.createOrder(order));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            var scope = dataScopeService.snapshot(AuthFilter.currentUser(context));
            scope.requireOrder(id);
            return ResourceSupport.ok(service.getOrder(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
