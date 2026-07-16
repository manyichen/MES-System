package com.example.messystem.production.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.production.entity.MesWorkReport;
import com.example.messystem.production.service.ProductionService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/** 承载 /work-reports 生产报工接口契约的 JAX-RS 控制器。 */
@Path("/work-reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkReportResource {
    private final ProductionService service = new ProductionService();

    @GET
    public Response list(@Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        return ResourceSupport.ok(user.hasRole("PRODUCTION_OPERATOR")
                ? service.listWorkReportsByOperator(user.user.userId)
                : service.listWorkReports());
    }

    @GET
    @Path("/by-work-order/{workOrderId}")
    public Response listByWorkOrder(@PathParam("workOrderId") long workOrderId,
            @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            return ResourceSupport.ok(user.hasRole("PRODUCTION_OPERATOR")
                    ? service.listWorkReportsByWorkOrderAndOperator(workOrderId, user.user.userId)
                    : service.listWorkReportsByWorkOrder(workOrderId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            MesWorkReport report = service.getWorkReport(id);
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (user.hasRole("PRODUCTION_OPERATOR") && !user.user.userId.equals(report.operatorId)) {
                throw new BadRequestException("只能查看本人的报工记录");
            }
            return ResourceSupport.ok(report);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    public Response create(MesWorkReport report, @Context ContainerRequestContext context) {
        try {
            report.operatorId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.created("报工单已提交", service.createWorkReport(report));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") long id, MesWorkReport report, @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            MesWorkReport current = service.getWorkReport(id);
            if (!user.user.userId.equals(current.operatorId)
                    || (!"SUBMITTED".equals(current.reportStatus) && !"REJECTED".equals(current.reportStatus))) {
                throw new BadRequestException("只能修改本人待审核或已驳回的报工单");
            }
            report.operatorId = user.user.userId;
            report.reportStatus = "SUBMITTED";
            return ResourceSupport.action("报工单已更新", service.updateWorkReport(id, report));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") long id) {
        try {
            service.deleteWorkReport(id);
            return ResourceSupport.action("报工单已删除", null);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/approve")
    public Response approve(@PathParam("id") long id) {
        try {
            return ResourceSupport.action("报工单已审核通过", service.approveWorkReport(id));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @POST
    @Path("/{id}/reject")
    public Response reject(@PathParam("id") long id, Map<String, String> request) {
        try {
            String reason = request == null ? "" : request.getOrDefault("reason", "").trim();
            if (reason.isEmpty()) {
                throw new BadRequestException("驳回理由不能为空");
            }
            return ResourceSupport.action("报工单已驳回", service.rejectWorkReport(id, reason));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
