package com.example.messystem.auth;

import com.example.messystem.common.ResourceSupport;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    private final AuthService service = new AuthService();

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            return ResourceSupport.ok(service.login(request));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
