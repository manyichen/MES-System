package com.example.messystem.auth.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.service.ProfileService;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.master.entity.MesUser;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** 承载 /profile 个人资料接口契约的 JAX-RS 控制器。 */
@Path("/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource {
    private final ProfileService service = new ProfileService();

    @GET
    public Response get(@Context ContainerRequestContext context) {
        try {
            return ResourceSupport.ok(service.get(AuthFilter.currentUser(context).user.userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @PUT
    public Response update(MesUser profile, @Context ContainerRequestContext context) {
        try {
            return ResourceSupport.action("个人资料已更新",
                    service.update(AuthFilter.currentUser(context).user.userId, profile));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
