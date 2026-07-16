package com.example.messystem.master.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.PasswordHasher;
import com.example.messystem.master.dao.UserDao;
import com.example.messystem.master.entity.MesUser;
import java.sql.SQLException;
import java.util.List;

/** 执行用户账号业务规则，并将持久化操作交给 {@link UserDao}。 */
public class UserService {
    private final UserDao dao = new UserDao();

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

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void requireUser(MesUser user) {
        if (user == null) throw new BadRequestException("user is required");
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new BadRequestException(message);
    }

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
