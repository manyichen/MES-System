package com.example.messystem.production.controller;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.production.service.ProductionService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** 承载 /piecework-wages 计件工资接口契约的 JAX-RS 控制器。 */
@Path("/piecework-wages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PieceworkWageResource {
    private final ProductionService service = new ProductionService();

    @GET
    public Response list(@Context ContainerRequestContext context) {
        AuthenticatedUser user = AuthFilter.currentUser(context);
        if (user.hasPermission("production.wage.read_all")) return ResourceSupport.ok(service.listWages());
        if (user.hasPermission("production.wage.read_self")) {
            return ResourceSupport.ok(service.listWagesByOperator(user.user.userId));
        }
        if (user.hasRole("WORKSHOP_MANAGER")) {
            return ResourceSupport.ok(service.wageSummaryForWorkshop(user.user.userId));
        }
        return ResourceSupport.ok(service.wageSummary());
    }

    @GET
    @Path("/by-report/{workReportId}")
    public Response listByReport(@PathParam("workReportId") long workReportId,
            @Context ContainerRequestContext context) {
        try {
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (user.hasPermission("production.wage.read_all")) {
                return ResourceSupport.ok(service.listWagesByReport(workReportId));
            }
            if (user.hasPermission("production.wage.read_self")) {
                return ResourceSupport.ok(service.listWagesByReportAndOperator(workReportId, user.user.userId));
            }
            throw new BadRequestException("汇总查看权限不能下钻到个人计件工资明细");
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") long id, @Context ContainerRequestContext context) {
        try {
            var wage = service.getWage(id);
            AuthenticatedUser user = AuthFilter.currentUser(context);
            if (!user.hasPermission("production.wage.read_all")
                    && (!user.hasPermission("production.wage.read_self") || !user.user.userId.equals(wage.operatorId))) {
                throw new BadRequestException("无权查看该员工的计件工资明细");
            }
            return ResourceSupport.ok(wage);
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }
}
