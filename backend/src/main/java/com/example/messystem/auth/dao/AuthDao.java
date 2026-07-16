package com.example.messystem.auth.dao;

import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.auth.LoginRequest;
import com.example.messystem.auth.LoginSession;
import com.example.messystem.auth.TokenHasher;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.PasswordHasher;
import com.example.messystem.master.entity.MesUser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/** 执行登录、会话查询和退出登录的原子持久化流程。 */
public class AuthDao {
    private static final int SESSION_HOURS = 8;
    private static final Set<String> DEPRECATED_LOGIN_USERNAMES = Set.of("mes_sysmaint", "mes_viewer");

    public LoginSession login(LoginRequest request, String loginIp, String userAgent) {
        if (request == null || request.username == null || request.username.isBlank()
                || request.password == null || request.password.isBlank()) {
            throw new BadRequestException("账号和密码不能为空");
        }

        String username = request.username.trim();
        if (DEPRECATED_LOGIN_USERNAMES.contains(username)) {
            throw new BadRequestException("账号或密码错误");
        }

        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                MesUser user = findLoginUser(connection, username);
                verifyLogin(connection, user, request.password);
                resetLoginState(connection, user.userId);

                String token = TokenHasher.newToken();
                LocalDateTime expiresAt = LocalDateTime.now().plusHours(SESSION_HOURS);
                createSession(connection, user.userId, token, loginIp, userAgent, expiresAt);
                AuthenticatedUser currentUser = loadAccess(connection, user, expiresAt);
                writeAudit(connection, "LOGIN", user, "SUCCESS", "用户登录成功");
                connection.commit();
                return new LoginSession(token, currentUser);
            } catch (RuntimeException | SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    public AuthenticatedUser authenticate(String token) {
        String sql = """
                select u.user_id, u.username, u.real_name, u.role_code, u.department, u.phone,
                       u.enabled, u.locked_until, u.created_at, u.updated_at, u.last_login_at, s.expires_at
                from mes_user_session s
                join mes_user u on u.user_id = s.user_id
                where s.token_hash = ? and s.revoked_at is null and s.expires_at > current_timestamp
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TokenHasher.hash(token));
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next() || rs.getInt("enabled") != 1) {
                    return null;
                }
                Timestamp lockedUntil = rs.getTimestamp("locked_until");
                if (lockedUntil != null && lockedUntil.toLocalDateTime().isAfter(LocalDateTime.now())) {
                    return null;
                }
                MesUser user = mapUser(rs);
                if (DEPRECATED_LOGIN_USERNAMES.contains(user.username)) {
                    return null;
                }
                return loadAccess(connection, user, getLocalDateTime(rs, "expires_at"));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    public void logout(String token, AuthenticatedUser currentUser) {
        String sql = "update mes_user_session set revoked_at = current_timestamp where token_hash = ? and revoked_at is null";
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TokenHasher.hash(token));
            statement.executeUpdate();
            writeAudit(connection, "LOGOUT", currentUser.user, "SUCCESS", "用户退出登录");
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    private static MesUser findLoginUser(Connection connection, String username) throws SQLException {
        String sql = """
                select user_id, username, real_name, role_code, department, phone, enabled,
                       password_hash, failed_login_count, locked_until, created_at, updated_at, last_login_at
                from mes_user where username = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new BadRequestException("账号或密码错误");
                }
                MesUser user = mapUser(rs);
                user.password = rs.getString("password_hash");
                Timestamp lockedUntil = rs.getTimestamp("locked_until");
                if (lockedUntil != null && lockedUntil.toLocalDateTime().isAfter(LocalDateTime.now())) {
                    throw new BadRequestException("账号因连续登录失败已临时锁定，请稍后再试");
                }
                return user;
            }
        }
    }

    private static void verifyLogin(Connection connection, MesUser user, String password) throws SQLException {
        if (user.enabled == null || user.enabled != 1) {
            throw new BadRequestException("账号已停用，请联系系统管理员");
        }
        if (PasswordHasher.verify(password, user.password)) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                update mes_user
                set failed_login_count = coalesce(failed_login_count, 0) + 1,
                    locked_until = case when coalesce(failed_login_count, 0) + 1 >= 5
                                        then current_timestamp + interval '15 minutes' else locked_until end
                where user_id = ?
                """)) {
            statement.setLong(1, user.userId);
            statement.executeUpdate();
        }
        writeAudit(connection, "LOGIN", user, "FAILED", "用户名或密码错误");
        connection.commit();
        throw new BadRequestException("账号或密码错误");
    }

    private static void resetLoginState(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                update mes_user set last_login_at = current_timestamp, failed_login_count = 0,
                    locked_until = null, updated_at = current_timestamp where user_id = ?
                """)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    private static void createSession(Connection connection, long userId, String token, String loginIp,
            String userAgent, LocalDateTime expiresAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into mes_user_session (user_id, token_hash, login_ip, user_agent, expires_at)
                values (?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, userId);
            statement.setString(2, TokenHasher.hash(token));
            statement.setString(3, trim(loginIp, 64));
            statement.setString(4, trim(userAgent, 500));
            statement.setTimestamp(5, Timestamp.valueOf(expiresAt));
            statement.executeUpdate();
        }
    }

    private static AuthenticatedUser loadAccess(Connection connection, MesUser user, LocalDateTime expiresAt) throws SQLException {
        Set<String> roles = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                select r.role_code
                from mes_user_role ur join mes_role r on r.role_id = ur.role_id
                where ur.user_id = ? and r.enabled = 1
                  and (ur.expires_at is null or ur.expires_at > current_timestamp)
                order by r.role_level, r.role_code
                """)) {
            statement.setLong(1, user.userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) roles.add(rs.getString(1));
            }
        }
        if (roles.isEmpty() && user.roleCode != null) roles.add(user.roleCode);

        Set<String> permissions = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                select distinct p.permission_code
                from mes_user_role ur
                join mes_role r on r.role_id = ur.role_id and r.enabled = 1
                join mes_role_permission rp on rp.role_id = r.role_id
                join mes_permission p on p.permission_id = rp.permission_id and p.enabled = 1
                where ur.user_id = ? and (ur.expires_at is null or ur.expires_at > current_timestamp)
                order by p.permission_code
                """)) {
            statement.setLong(1, user.userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) permissions.add(rs.getString(1));
            }
        }
        if (roles.contains(AuthenticatedUser.SUPER_ADMIN_ROLE)) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    select permission_code from mes_permission
                    where enabled = 1 order by permission_code
                    """)) {
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) permissions.add(rs.getString(1));
                }
            }
        }
        if (roles.contains("HR_MANAGER")) {
            permissions.add("user.update_role");
            permissions.add("data_scope.manage");
        }
        if (roles.contains("WORKSHOP_MANAGER")) {
            permissions.removeIf(permission -> permission.startsWith("warehouse."));
        }
        if (roles.contains("PRODUCTION_OPERATOR")) {
            permissions.add("warehouse.requisition.create");
            permissions.add("warehouse.purchase.request");
        }
        if (roles.contains("WAREHOUSE_ADMIN")) {
            permissions.add("warehouse.read");
            permissions.add("warehouse.requisition.approve");
            permissions.add("warehouse.purchase.request");
        }
        if (roles.contains("SYSTEM_ADMIN") && !roles.contains(AuthenticatedUser.SUPER_ADMIN_ROLE)) {
            permissions.removeIf(AuthDao::isProductionBusinessPermission);
        }
        if (roles.contains("PMC_PLANNER") && !roles.contains("SYSTEM_ADMIN")
                && !roles.contains(AuthenticatedUser.SUPER_ADMIN_ROLE)) {
            permissions.removeIf(permission -> permission.startsWith("warehouse.")
                    || permission.startsWith("quality.") || permission.startsWith("equipment."));
        }
        Set<Long> lineIds = loadScopeIds(connection, "mes_user_line_scope", "line_id", user.userId);
        Set<Long> warehouseIds = loadScopeIds(connection, "mes_user_warehouse_scope", "warehouse_id", user.userId);
        return new AuthenticatedUser(user, roles, permissions, lineIds, warehouseIds, expiresAt);
    }

    /**
     * 系统管理员只承担账号、权限、审计和运行维护职责。即使旧库尚未执行撤权迁移，
     * 也不能凭历史残留授权进入生产业务。
     */
    private static boolean isProductionBusinessPermission(String permission) {
        return permission.startsWith("planning.") || permission.startsWith("production.")
                || permission.startsWith("warehouse.") || permission.startsWith("quality.")
                || permission.startsWith("equipment.") || permission.startsWith("master.")
                || permission.startsWith("process.") || permission.startsWith("trace.")
                || permission.startsWith("feedback.") || permission.equals("business.delete")
                || permission.equals("demo.seed");
    }

    private static Set<Long> loadScopeIds(Connection connection, String table, String column, long userId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select " + column + " from " + table + " where user_id = ? order by " + column)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                Set<Long> values = new LinkedHashSet<>();
                while (rs.next()) values.add(rs.getLong(1));
                return values;
            }
        }
    }

    private static void writeAudit(Connection connection, String eventType, MesUser user, String result, String message)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into mes_audit_log
                    (event_type, module_code, action_code, resource_type, resource_id, user_id,
                     username, role_code, result, message)
                values (?, 'auth', ?, 'session', ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, eventType);
            statement.setString(2, eventType.toLowerCase());
            statement.setString(3, String.valueOf(user.userId));
            statement.setLong(4, user.userId);
            statement.setString(5, user.username);
            statement.setString(6, user.roleCode);
            statement.setString(7, result);
            statement.setString(8, message);
            statement.executeUpdate();
        }
    }

    private static MesUser mapUser(ResultSet rs) throws SQLException {
        MesUser user = new MesUser();
        user.userId = rs.getLong("user_id");
        user.username = rs.getString("username");
        user.realName = rs.getString("real_name");
        user.roleCode = rs.getString("role_code");
        user.department = rs.getString("department");
        user.phone = rs.getString("phone");
        user.enabled = rs.getInt("enabled");
        user.createdAt = getLocalDateTime(rs, "created_at");
        user.updatedAt = getLocalDateTime(rs, "updated_at");
        user.lastLoginAt = getLocalDateTime(rs, "last_login_at");
        return user;
    }

    private static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static String trim(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
