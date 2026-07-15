package com.example.messystem.access;

import com.example.messystem.common.Db;
import com.example.messystem.common.BadRequestException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SystemMaintenanceService {
    public SystemMaintenanceSummary loadSummary() {
        try (Connection connection = Db.getConnection()) {
            List<SessionRow> sessions = safeList(() -> listSessions(connection));
            List<LockedUserRow> lockedUsers = safeList(() -> listLockedUsers(connection));
            List<AuditRow> auditLogs = safeList(() -> listAuditLogs(connection));
            List<AuditRow> failedLoginLogs = safeList(() -> listFailedLoginLogs(connection));
            List<SyncLogRow> syncLogs = safeList(() -> listSyncLogs(connection));
            List<SyncLogRow> syncFailures = safeList(() -> listSyncFailures(connection));
            return new SystemMaintenanceSummary(
                    List.of(
                            metric("enabledUsers", "启用账号", count(connection,
                                    "select count(*) from mes_user where enabled = 1"), "个", "normal"),
                            metric("lockedUsers", "锁定账号", count(connection,
                                    "select count(*) from mes_user where locked_until > current_timestamp"), "个", "warning"),
                            metric("activeSessions", "有效会话", count(connection,
                                    "select count(*) from mes_user_session where revoked_at is null and expires_at > current_timestamp"), "个", "normal"),
                            metric("failedLogins", "24小时失败登录", count(connection,
                                    "select count(*) from mes_audit_log where event_type = 'LOGIN' and result = 'FAILED' and created_at >= current_timestamp - interval '24 hours'"), "次", "danger"),
                            metric("pendingApplications", "待处理权限申请", count(connection,
                                    "select count(*) from mes_permission_apply where apply_status = 'SUBMITTED'"), "项", "warning"),
                            metric("syncFailures", "同步异常", count(connection,
                                    "select count(*) from mes_sync_log where sync_status in ('FAILED','ERROR')"), "条", "danger")),
                    sessions,
                    lockedUsers,
                    auditLogs,
                    syncLogs,
                    failedLoginLogs,
                    syncFailures);
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    private static SystemMetric metric(String code, String label, long value, String unit, String level) {
        return new SystemMetric(code, label, value, unit, level);
    }

    private static long count(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException ex) {
            return 0;
        }
    }

    private static <T> List<T> safeList(SqlListSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (SQLException ex) {
            return List.of();
        }
    }

    private static List<SessionRow> listSessions(Connection connection) throws SQLException {
        String sql = """
                select s.session_id, s.user_id, u.username, u.real_name, u.role_code, s.login_ip, s.issued_at as created_at, s.expires_at
                from mes_user_session s
                left join mes_user u on u.user_id = s.user_id
                where s.revoked_at is null and s.expires_at > current_timestamp
                order by s.issued_at desc
                limit 500
                """;
        List<SessionRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new SessionRow(rs.getLong("session_id"), rs.getLong("user_id"), rs.getString("username"),
                        rs.getString("real_name"), rs.getString("role_code"), rs.getString("login_ip"),
                        time(rs, "created_at"), time(rs, "expires_at")));
            }
        }
        return rows;
    }

    public int revokeSession(long sessionId, long actorUserId) {
        if (sessionId <= 0) throw new BadRequestException("会话ID不正确");
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Long userId = findSessionUserId(connection, sessionId);
                if (userId == null) throw new BadRequestException("会话不存在或已失效");
                if (userId == actorUserId) throw new BadRequestException("不能强制下线当前自己的会话");
                int updated;
                try (PreparedStatement statement = connection.prepareStatement("""
                        update mes_user_session
                        set revoked_at = current_timestamp
                        where user_id = ?
                          and revoked_at is null
                          and expires_at > current_timestamp
                        """)) {
                    statement.setLong(1, userId);
                    updated = statement.executeUpdate();
                }
                lockUser(connection, userId);
                connection.commit();
                return updated;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    public int revokeUserSessions(long userId, long actorUserId) {
        if (userId <= 0) throw new BadRequestException("用户ID不正确");
        if (userId == actorUserId) throw new BadRequestException("不能撤销当前自己的登录会话");
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int updated;
                try (PreparedStatement statement = connection.prepareStatement("""
                        update mes_user_session
                        set revoked_at = current_timestamp
                        where user_id = ?
                          and revoked_at is null
                          and expires_at > current_timestamp
                        """)) {
                    statement.setLong(1, userId);
                    updated = statement.executeUpdate();
                }
                lockUser(connection, userId);
                connection.commit();
                return updated;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    public int cleanupExpiredSessions() {
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        update mes_user_session
                        set revoked_at = current_timestamp
                        where revoked_at is null
                          and expires_at <= current_timestamp
                        """)) {
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    private static Long findSessionUserId(Connection connection, long sessionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select user_id
                from mes_user_session
                where session_id = ?
                  and revoked_at is null
                  and expires_at > current_timestamp
                """)) {
            statement.setLong(1, sessionId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong("user_id") : null;
            }
        }
    }

    private static void lockUser(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                update mes_user
                set locked_until = current_timestamp + interval '100 years',
                    failed_login_count = 0,
                    updated_at = current_timestamp
                where user_id = ?
                """)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    public int unlockUser(long userId) {
        if (userId <= 0) throw new BadRequestException("用户ID不正确");
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        update mes_user
                        set failed_login_count = 0,
                            locked_until = null,
                            updated_at = current_timestamp
                        where user_id = ?
                        """)) {
            statement.setLong(1, userId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    public int disableUser(long userId, long actorUserId) {
        if (userId <= 0) throw new BadRequestException("用户ID不正确");
        if (userId == actorUserId) throw new BadRequestException("不能删除当前自己的账号");
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int updated;
                try (PreparedStatement statement = connection.prepareStatement("""
                        update mes_user
                        set enabled = 0,
                            failed_login_count = 0,
                            locked_until = null,
                            updated_at = current_timestamp
                        where user_id = ?
                          and enabled = 1
                        """)) {
                    statement.setLong(1, userId);
                    updated = statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        update mes_user_session
                        set revoked_at = current_timestamp
                        where user_id = ?
                          and revoked_at is null
                        """)) {
                    statement.setLong(1, userId);
                    statement.executeUpdate();
                }
                connection.commit();
                return updated;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    public int markSyncLogHandled(long syncLogId) {
        if (syncLogId <= 0) throw new BadRequestException("同步日志ID不正确");
        try (Connection connection = Db.getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        update mes_sync_log
                        set sync_status = 'HANDLED',
                            error_message = coalesce(nullif(error_message, ''), '已由系统管理员标记处理')
                        where sync_log_id = ?
                          and sync_status in ('FAILED', 'ERROR')
                        """)) {
            statement.setLong(1, syncLogId);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    private static List<LockedUserRow> listLockedUsers(Connection connection) throws SQLException {
        String sql = """
                select user_id, username, real_name, role_code, failed_login_count, locked_until
                from mes_user
                where locked_until > current_timestamp
                order by locked_until desc
                limit 500
                """;
        List<LockedUserRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new LockedUserRow(rs.getLong("user_id"), rs.getString("username"),
                        rs.getString("real_name"), rs.getString("role_code"),
                        rs.getInt("failed_login_count"), time(rs, "locked_until")));
            }
        }
        return rows;
    }

    private static List<AuditRow> listAuditLogs(Connection connection) throws SQLException {
        String sql = """
                select audit_id, event_type, action_code, resource_type, user_id, username, role_code, result, created_at
                from mes_audit_log
                order by created_at desc
                limit 500
                """;
        List<AuditRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new AuditRow(rs.getLong("audit_id"), rs.getString("event_type"),
                        rs.getString("action_code"), rs.getString("resource_type"),
                        nullableLong(rs, "user_id"), rs.getString("username"), rs.getString("role_code"),
                        rs.getString("result"), time(rs, "created_at")));
            }
        }
        return rows;
    }

    private static List<AuditRow> listFailedLoginLogs(Connection connection) throws SQLException {
        String sql = """
                select audit_id, event_type, action_code, resource_type, user_id, username, role_code, result, created_at
                from mes_audit_log
                where event_type = 'LOGIN' and result = 'FAILED'
                  and created_at >= current_timestamp - interval '24 hours'
                order by created_at desc
                limit 500
                """;
        List<AuditRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new AuditRow(rs.getLong("audit_id"), rs.getString("event_type"),
                        rs.getString("action_code"), rs.getString("resource_type"),
                        nullableLong(rs, "user_id"), rs.getString("username"), rs.getString("role_code"),
                        rs.getString("result"), time(rs, "created_at")));
            }
        }
        return rows;
    }

    private static List<SyncLogRow> listSyncLogs(Connection connection) throws SQLException {
        String sql = """
                select sync_log_id, sync_object, source_system, business_key, sync_status, error_message, sync_time
                from mes_sync_log
                order by sync_time desc
                limit 500
                """;
        List<SyncLogRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new SyncLogRow(rs.getLong("sync_log_id"), rs.getString("sync_object"),
                        rs.getString("source_system"), rs.getString("business_key"),
                        rs.getString("sync_status"), rs.getString("error_message"), time(rs, "sync_time")));
            }
        }
        return rows;
    }

    private static List<SyncLogRow> listSyncFailures(Connection connection) throws SQLException {
        String sql = """
                select sync_log_id, sync_object, source_system, business_key, sync_status, error_message, sync_time
                from mes_sync_log
                where sync_status in ('FAILED','ERROR')
                order by sync_time desc
                limit 500
                """;
        List<SyncLogRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new SyncLogRow(rs.getLong("sync_log_id"), rs.getString("sync_object"),
                        rs.getString("source_system"), rs.getString("business_key"),
                        rs.getString("sync_status"), rs.getString("error_message"), time(rs, "sync_time")));
            }
        }
        return rows;
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    public record SystemMaintenanceSummary(List<SystemMetric> metrics, List<SessionRow> sessions,
            List<LockedUserRow> lockedUsers, List<AuditRow> auditLogs, List<SyncLogRow> syncLogs,
            List<AuditRow> failedLoginLogs, List<SyncLogRow> syncFailures) {
    }

    public record SystemMetric(String code, String label, long value, String unit, String level) {
    }

    public record SessionRow(long sessionId, long userId, String username, String realName, String roleCode,
            String loginIp, LocalDateTime createdAt, LocalDateTime expiresAt) {
    }

    public record LockedUserRow(long userId, String username, String realName, String roleCode,
            int failedLoginCount, LocalDateTime lockedUntil) {
    }

    public record AuditRow(long auditId, String eventType, String actionCode, String resourceType, Long actorUserId,
            String actorUsername, String actorRoleCode, String result, LocalDateTime createdAt) {
    }

    public record SyncLogRow(long syncLogId, String syncType, String sourceSystem, String targetTable,
            String syncStatus, String message, LocalDateTime createdAt) {
    }

    @FunctionalInterface
    private interface SqlListSupplier<T> {
        List<T> get() throws SQLException;
    }
}
