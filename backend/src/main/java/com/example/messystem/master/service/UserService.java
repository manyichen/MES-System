/*
 * 答辩定位：主数据与用户 模块的 UserService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.master.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.PasswordHasher;
import com.example.messystem.master.dao.UserDao;
import com.example.messystem.master.entity.MesUser;
import java.sql.SQLException;
import java.util.List;

/** 执行用户账号业务规则，并将持久化操作交给 {@link UserDao}。 */
public class UserService {
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final UserDao dao = new UserDao();

    /**
     * 业务用例：查询列表。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public List<MesUser> listUsers() {
        return database(dao::findAll);
    }

    /** 创建启用状态的账号，并在同一事务中关联主角色。 */
    public MesUser createUser(MesUser user) {
        requireUser(user);
        requireText(user.username, "username is required");
        requireText(user.password, "password is required");
        String username = user.username.trim();
        String roleCode = defaultText(user.roleCode, "PRODUCTION_OPERATOR");
        String realName = defaultText(user.realName, username);
        int enabled = user.enabled == null ? 1 : user.enabled;
        return database(() -> dao.insert(user, username, realName, roleCode, enabled,
                PasswordHasher.hash(user.password)));
    }

    /** 替换主角色并撤销活动会话，使权限在重新登录后立即刷新。 */
    public MesUser updateRole(long userId, String roleCode) {
        requireText(roleCode, "roleCode is required");
        return database(() -> dao.replaceRole(userId, roleCode.trim()));
    }

    /**
     * 业务用例：执行 defaultText 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 业务用例：执行 requireUser 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static void requireUser(MesUser user) {
        if (user == null) throw new BadRequestException("user is required");
    }

    /**
     * 业务用例：执行 requireText 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new BadRequestException(message);
    }

    /**
     * 业务用例：执行 database 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static <T> T database(SqlCall<T> call) {
        try {
            return call.execute();
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T execute() throws SQLException;
    }
}
