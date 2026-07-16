package com.example.messystem.common.controller;

import com.example.messystem.common.ApiResponse;
import com.example.messystem.common.service.HealthService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/** 供运维人员检查应用和数据库健康状态的控制器。 */
@Path("/db")
@Produces(MediaType.APPLICATION_JSON)
public class DbPingResource {
    private final HealthService service = new HealthService();

    /** 返回安全的连接元数据以及 MES 核心表可用状态。 */
    @GET
    @Path("/ping")
    public Response ping() {
        try {
            Map<String, Object> data = service.databaseHealth();
            return Response.ok(ApiResponse.ok("database connected", data)).build();
        } catch (RuntimeException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ApiResponse.fail("database connection failed: " + ex.getMessage()))
                    .build();
        }
    }
}
