package com.example.messystem.access.service;

import com.example.messystem.access.dao.AccessDao;
import com.example.messystem.auth.AuthenticatedUser;
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
}
