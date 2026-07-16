package com.example.messystem.access.service;

import com.example.messystem.access.dao.AccessDao;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import java.util.List;
import java.util.Set;

/** 编排角色分配和权限申请业务。 */
public class AccessService {
    private final AccessDao dao = new AccessDao();

    public List<AccessDao.RoleInfo> listRoles() {
        return dao.listRoles();
    }

    public List<AccessDao.PermissionInfo> listPermissions(String roleCode) {
        return dao.listPermissions(roleCode);
    }

    public Set<String> getUserRoles(long userId) {
        return dao.getUserRoles(userId);
    }

    /** 通过 DAO 事务替换用户角色集合，并防止管理员误锁定自身权限。 */
    public Set<String> assignUserRoles(long userId, List<String> roleCodes, AuthenticatedUser actor) {
        return dao.assignUserRoles(userId, roleCodes, actor);
    }

    public List<AccessDao.PermissionApplication> listPermissionApplications(AuthenticatedUser actor) {
        return dao.listPermissionApplications(actor);
    }

    public AccessDao.PermissionApplication createPermissionApplication(long targetUserId, String toRoleCode,
            String reason, AuthenticatedUser actor) {
        return dao.createPermissionApplication(targetUserId, toRoleCode, reason, actor);
    }

    public AccessDao.PermissionApplication reviewPermissionApplication(long applyId, String decision,
            String comment, AuthenticatedUser actor) {
        return dao.reviewPermissionApplication(applyId, decision, comment, actor);
    }

    public AccessDao.PermissionApplication applyPermissionApplication(long applyId, AuthenticatedUser actor) {
        return dao.applyPermissionApplication(applyId, actor);
    }

    public List<AccessDao.AccountApplication> listAccountApplications(AuthenticatedUser actor) {
        requireAccountApplicationAccess(actor);
        boolean all = actor.hasPermission("permission.review") || actor.hasPermission("role.manage");
        return dao.listAccountApplications(all, actor.user.userId);
    }

    public AccessDao.AccountApplication createAccountApplication(AccountApplicationRequest request,
            AuthenticatedUser actor) {
        if (actor == null || !actor.canActAs("HR_MANAGER") || !actor.hasPermission("permission.apply")) {
            throw new BadRequestException("只有人事经理可以发起账号申请");
        }
        if (request == null) throw new BadRequestException("账号申请不能为空");
        return dao.createAccountApplication(new AccessDao.AccountApplicationRequest(
                request.username(), request.password(), request.realName(), request.roleCode(),
                request.department(), request.phone(), request.reason()), actor.user.userId);
    }

    public AccessDao.AccountApplication reviewAccountApplication(long applyId, String decision,
            String comment, AuthenticatedUser actor) {
        if (actor == null || !actor.hasPermission("permission.review")) {
            throw new BadRequestException("只有账号申请审核人可以处理账号申请");
        }
        String normalized = decision == null ? "" : decision.trim().toUpperCase();
        if ("REJECTED".equals(normalized) && (comment == null || comment.isBlank())) {
            throw new BadRequestException("拒绝账号申请时必须填写审核意见");
        }
        return dao.reviewAccountApplication(applyId, normalized, comment, actor.user.userId);
    }

    private static void requireAccountApplicationAccess(AuthenticatedUser actor) {
        if (actor == null || !(actor.hasPermission("permission.apply")
                || actor.hasPermission("permission.review") || actor.hasPermission("role.manage"))) {
            throw new BadRequestException("无权查看账号申请");
        }
    }

    public record AccountApplicationRequest(String username, String password, String realName,
            String roleCode, String department, String phone, String reason) {
    }
}
