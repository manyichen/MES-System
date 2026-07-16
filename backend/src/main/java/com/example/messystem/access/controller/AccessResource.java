package com.example.messystem.access.controller;

import com.example.messystem.access.service.AccessService;
import com.example.messystem.access.service.SystemMaintenanceService;
import com.example.messystem.auth.AuthFilter;
import com.example.messystem.common.ResourceSupport;
import com.example.messystem.security.service.DataScopeService;
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

/** 承载 /access 权限管理接口契约的 JAX-RS 控制器。 */
@Path("/access")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccessResource {
    private final AccessService service = new AccessService();
    private final DataScopeService dataScopeService = new DataScopeService();
    private final SystemMaintenanceService systemMaintenanceService = new SystemMaintenanceService();

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
    @Path("/system-maintenance")
    public Response systemMaintenance() {
        try {
            return ResourceSupport.ok(systemMaintenanceService.loadSummary());
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @jakarta.ws.rs.POST
    @Path("/system-maintenance/sessions/{sessionId}/revoke")
    public Response revokeSession(@PathParam("sessionId") long sessionId,
            @Context ContainerRequestContext context) {
        try {
            long actorUserId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.action("登录会话已撤销",
                    systemMaintenanceService.revokeSession(sessionId, actorUserId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @jakarta.ws.rs.POST
    @Path("/system-maintenance/sessions/cleanup-expired")
    public Response cleanupExpiredSessions() {
        try {
            return ResourceSupport.action("过期会话已清理", systemMaintenanceService.cleanupExpiredSessions());
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @jakarta.ws.rs.POST
    @Path("/system-maintenance/users/{userId}/unlock")
    public Response unlockUser(@PathParam("userId") long userId) {
        try {
            return ResourceSupport.action("账号锁定已解除", systemMaintenanceService.unlockUser(userId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @jakarta.ws.rs.POST
    @Path("/system-maintenance/users/{userId}/restore")
    public Response restoreUser(@PathParam("userId") long userId,
            @Context ContainerRequestContext context) {
        try {
            long actorUserId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.action("账号已恢复启用",
                    systemMaintenanceService.restoreUser(userId, actorUserId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @jakarta.ws.rs.POST
    @Path("/system-maintenance/users/{userId}/revoke-sessions")
    public Response revokeUserSessions(@PathParam("userId") long userId,
            @Context ContainerRequestContext context) {
        try {
            long actorUserId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.action("用户登录会话已撤销",
                    systemMaintenanceService.revokeUserSessions(userId, actorUserId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @jakarta.ws.rs.POST
    @Path("/system-maintenance/users/{userId}/disable")
    public Response disableUser(@PathParam("userId") long userId,
            @Context ContainerRequestContext context) {
        try {
            long actorUserId = AuthFilter.currentUser(context).user.userId;
            return ResourceSupport.action("账号已删除并禁止登录",
                    systemMaintenanceService.disableUser(userId, actorUserId));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @jakarta.ws.rs.POST
    @Path("/system-maintenance/sync-logs/{syncLogId}/mark-handled")
    public Response markSyncLogHandled(@PathParam("syncLogId") long syncLogId) {
        try {
            return ResourceSupport.action("同步异常已标记处理",
                    systemMaintenanceService.markSyncLogHandled(syncLogId));
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
    @Path("/account-applications")
    public Response accountApplications(@Context ContainerRequestContext context) {
        try {
            return ResourceSupport.ok(service.listAccountApplications(AuthFilter.currentUser(context)));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @jakarta.ws.rs.POST
    @Path("/account-applications")
    public Response createAccountApplication(AccountApplicationRequest request,
            @Context ContainerRequestContext context) {
        try {
            AccessService.AccountApplicationRequest payload = request == null
                    ? null
                    : new AccessService.AccountApplicationRequest(request.username(), request.password(),
                            request.realName(), request.roleCode(), request.department(), request.phone(),
                            request.reason());
            return ResourceSupport.created("账号申请已提交",
                    service.createAccountApplication(payload, AuthFilter.currentUser(context)));
        } catch (RuntimeException ex) {
            return ResourceSupport.handle(ex);
        }
    }

    @jakarta.ws.rs.POST
    @Path("/account-applications/{applyId}/review")
    public Response reviewAccountApplication(@PathParam("applyId") long applyId,
            AccountApplicationReviewRequest request, @Context ContainerRequestContext context) {
        try {
            if (request == null) throw new com.example.messystem.common.BadRequestException("审核信息不能为空");
            return ResourceSupport.action("账号申请已处理", service.reviewAccountApplication(
                    applyId, request.decision(), request.comment(), AuthFilter.currentUser(context)));
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
            if (AuthFilter.currentUser(context).hasRole("SYSTEM_ADMIN")) {
                throw new com.example.messystem.common.BadRequestException("系统管理员请通过权限变更申请审批执行角色调整");
            }
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

    public record AccountApplicationRequest(String username, String password, String realName,
            String roleCode, String department, String phone, String reason) {
    }

    public record AccountApplicationReviewRequest(String decision, String comment) {
    }

    public record DataScopeRequest(List<Long> lineIds, List<Long> warehouseIds) {
    }
}
