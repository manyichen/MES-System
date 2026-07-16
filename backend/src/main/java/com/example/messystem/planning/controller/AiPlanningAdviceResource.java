package com.example.messystem.planning.controller;

import com.example.messystem.common.ResourceSupport;
import com.example.messystem.planning.ai.AiPlanningAdviceRequest;
import com.example.messystem.planning.service.AiPlanningAdviceService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** 承载 /ai/planning 智能排产建议接口契约的 JAX-RS 控制器。 */
@Path("/ai/planning")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AiPlanningAdviceResource {
    private final AiPlanningAdviceService service = new AiPlanningAdviceService();

    @POST
    @Path("/advice")
    public Response advice(AiPlanningAdviceRequest request) {
        try {
            return ResourceSupport.action("AI 排产建议已生成", service.generate(request));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
