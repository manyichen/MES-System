package com.example.messystem.trace.resource;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.trace.service.TireTraceService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/public/tire-traces")
@Produces(MediaType.APPLICATION_JSON)
public class PublicTireTraceResource {
    private final TireTraceService service = new TireTraceService();

    @GET
    @Path("/{token}")
    public ApiResponse<Map<String, Object>> get(@PathParam("token") String token) {
        return ApiResponse.ok(service.publicView(token));
    }

    @GET
    @Path("/{token}/document")
    @Produces("application/pdf")
    public Response document(@PathParam("token") String token) {
        return TireLabelResource.file(service.publicDocument(token), "application/pdf", "tire-trace.pdf", false);
    }
}
