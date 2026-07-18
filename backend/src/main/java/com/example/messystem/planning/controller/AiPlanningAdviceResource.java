/*
 * 答辩定位：订单、计划、齐套与工单 模块的 AiPlanningAdviceResource。
 * 分层职责：HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。
 * 典型调用链：浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final AiPlanningAdviceService service = new AiPlanningAdviceService();

    /**
     * 接口：POST /api/ai/planning/advice。
     * 用例：执行 advice 对应的业务步骤；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。
     * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。
     */
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
