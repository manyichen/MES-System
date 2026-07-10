package com.example.messystem.master.resource;

import com.example.messystem.common.ResourceSupport;
import com.example.messystem.master.entity.MesUser;
import com.example.messystem.master.service.UserService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {
    private final UserService service = new UserService();

    @GET
    public Response list() {
        try {
            return ResourceSupport.ok(service.listUsers());
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    public Response create(MesUser user) {
        try {
            return ResourceSupport.created("user created", service.createUser(user));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @PUT
    @Path("/{userId}/role")
    public Response updateRole(@PathParam("userId") long userId, MesUser user) {
        try {
            return ResourceSupport.action("user role updated", service.updateRole(userId, user.roleCode));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
