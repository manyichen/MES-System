/*
 * 答辩定位：访问控制与系统维护 模块的 AccessService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.access.service;

import com.example.messystem.access.dao.AccessDao;
import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import java.util.List;
import java.util.Set;

/** 编排角色分配和权限申请业务。 */
public class AccessService {
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final AccessDao dao = new AccessDao();

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<AccessDao.RoleInfo> listRoles() {
        return dao.listRoles();
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<AccessDao.PermissionInfo> listPermissions(String roleCode) {
        return dao.listPermissions(roleCode);
    }

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public Set<String> getUserRoles(long userId) {
        return dao.getUserRoles(userId);
    }

    /** 通过 DAO 事务替换用户角色集合，并防止管理员误锁定自身权限。 */
    public Set<String> assignUserRoles(long userId, List<String> roleCodes, AuthenticatedUser actor) {
        return dao.assignUserRoles(userId, roleCodes, actor);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<AccessDao.PermissionApplication> listPermissionApplications(AuthenticatedUser actor) {
        return dao.listPermissionApplications(actor);
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public AccessDao.PermissionApplication createPermissionApplication(long targetUserId, String toRoleCode,
            String reason, AuthenticatedUser actor) {
        return dao.createPermissionApplication(targetUserId, toRoleCode, reason, actor);
    }

    /**
     * 业务用例：审核业务申请。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public AccessDao.PermissionApplication reviewPermissionApplication(long applyId, String decision,
            String comment, AuthenticatedUser actor) {
        return dao.reviewPermissionApplication(applyId, decision, comment, actor);
    }

    /**
     * 业务用例：执行已审核的变更。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public AccessDao.PermissionApplication applyPermissionApplication(long applyId, AuthenticatedUser actor) {
        return dao.applyPermissionApplication(applyId, actor);
    }

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<AccessDao.AccountApplication> listAccountApplications(AuthenticatedUser actor) {
        requireAccountApplicationAccess(actor);
        boolean all = actor.hasPermission("permission.review") || actor.hasPermission("role.manage");
        return dao.listAccountApplications(all, actor.user.userId);
    }

    /**
     * 业务用例：创建业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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

    /**
     * 业务用例：审核业务申请。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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

    /**
     * 业务用例：执行 requireAccountApplicationAccess 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static void requireAccountApplicationAccess(AuthenticatedUser actor) {
        if (actor == null || !(actor.hasPermission("permission.apply")
                || actor.hasPermission("permission.review") || actor.hasPermission("role.manage"))) {
            throw new BadRequestException("无权查看账号申请");
        }
    }

    /**
     * 业务用例：执行 AccountApplicationRequest 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public record AccountApplicationRequest(String username, String password, String realName,
            String roleCode, String department, String phone, String reason) {
    }
}
