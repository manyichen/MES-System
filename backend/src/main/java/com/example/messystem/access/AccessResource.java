package com.example.messystem.access;

import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.security.DataScopeService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/access")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccessResource {
    private final AccessService service = new AccessService();
    private final DataScopeService dataScopeService = new DataScopeService();

    @GET
    @Path("/roles")
    public Response roles() {
        try {
            return ResourceSupport.ok(service.listRoles());
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/permissions")
    public Response permissions(@QueryParam("roleCode") String roleCode) {
        try {
            return ResourceSupport.ok(service.listPermissions(roleCode));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/permission-applications")
    public Response permissionApplications(@Context ContainerRequestContext context) {
        try {
            return ResourceSupport.ok(service.listPermissionApplications(AuthFilter.currentUser(context)));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @jakarta.ws.rs.POST
    @Path("/permission-applications")
    public Response createPermissionApplication(PermissionApplicationRequest request,
            @Context ContainerRequestContext context) {
        try {
            return ResourceSupport.created("权限申请已提交", service.createPermissionApplication(
                    request.targetUserId(), request.toRoleCode(), request.reason(), AuthFilter.currentUser(context)));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @jakarta.ws.rs.POST
    @Path("/permission-applications/{applyId}/review")
    public Response reviewPermissionApplication(@PathParam("applyId") long applyId,
            PermissionReviewRequest request, @Context ContainerRequestContext context) {
        try {
            return ResourceSupport.action("权限申请已复核", service.reviewPermissionApplication(
                    applyId, request.decision(), request.comment(), AuthFilter.currentUser(context)));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @jakarta.ws.rs.POST
    @Path("/permission-applications/{applyId}/apply")
    public Response applyPermissionApplication(@PathParam("applyId") long applyId,
            @Context ContainerRequestContext context) {
        try {
            return ResourceSupport.action("权限申请已应用", service.applyPermissionApplication(
                    applyId, AuthFilter.currentUser(context)));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/users/{userId}/roles")
    public Response userRoles(@PathParam("userId") long userId) {
        try {
            return ResourceSupport.ok(service.getUserRoles(userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @GET
    @Path("/users/{userId}/data-scopes")
    public Response userDataScopes(@PathParam("userId") long userId) {
        try {
            return ResourceSupport.ok(dataScopeService.getUserScopes(userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @PUT
    @Path("/users/{userId}/data-scopes")
    public Response updateUserDataScopes(@PathParam("userId") long userId, DataScopeRequest request,
            @Context ContainerRequestContext context) {
        try {
            if (request == null) throw new com.example.messystem.common.BadRequestException("数据范围不能为空");
            return ResourceSupport.action("用户数据范围已更新", dataScopeService.replaceUserScopes(
                    userId, request.lineIds(), request.warehouseIds(),
                    AuthFilter.currentUser(context).user.userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @PUT
    @Path("/users/{userId}/roles")
    public Response assignRoles(@PathParam("userId") long userId, UserRolesRequest request,
            @Context ContainerRequestContext context) {
        try {
            List<String> roleCodes = request == null ? null : request.roleCodes();
            return ResourceSupport.action("用户角色已更新，原登录会话已失效",
                    service.assignUserRoles(userId, roleCodes, AuthFilter.currentUser(context)));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    public record UserRolesRequest(List<String> roleCodes) {
    }

    public record PermissionApplicationRequest(long targetUserId, String toRoleCode, String reason) {
    }

    public record PermissionReviewRequest(String decision, String comment) {
    }

    public record DataScopeRequest(List<Long> lineIds, List<Long> warehouseIds) {
    }
}
