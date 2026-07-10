package com.example.messystem.planning.resource;

import com.example.messystem.common.ResourceSupport;
import com.example.messystem.planning.entity.MesCustomerOrder;
import com.example.messystem.planning.service.CustomerOrderService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerOrderResource {
    private final CustomerOrderService service = new CustomerOrderService();

    @GET
    public Response list() {
        return ResourceSupport.ok(service.listOrders());
    }

    @POST
    public Response create(MesCustomerOrder order) {
        try {
            return ResourceSupport.created("order created", service.createOrder(order));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id) {
        try {
            return ResourceSupport.ok(service.getOrder(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
