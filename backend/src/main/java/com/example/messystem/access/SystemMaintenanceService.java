package com.example.messystem.access;

import com.example.messystem.common.Db;
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
                    listSessions(connection),
                    listLockedUsers(connection),
                    listAuditLogs(connection),
                    listSyncLogs(connection));
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
        }
    }

    private static List<SessionRow> listSessions(Connection connection) throws SQLException {
        String sql = """
                select s.session_id, u.username, u.real_name, u.role_code, s.login_ip, s.created_at, s.expires_at
                from mes_user_session s
                join mes_user u on u.user_id = s.user_id
                where s.revoked_at is null and s.expires_at > current_timestamp
                order by s.created_at desc
                limit 10
                """;
        List<SessionRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new SessionRow(rs.getLong("session_id"), rs.getString("username"),
                        rs.getString("real_name"), rs.getString("role_code"), rs.getString("login_ip"),
                        time(rs, "created_at"), time(rs, "expires_at")));
            }
        }
        return rows;
    }

    private static List<LockedUserRow> listLockedUsers(Connection connection) throws SQLException {
        String sql = """
                select user_id, username, real_name, role_code, failed_login_count, locked_until
                from mes_user
                where locked_until > current_timestamp
                order by locked_until desc
                limit 10
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
                select audit_id, event_type, action_code, resource_type, username, role_code, result, created_at
                from mes_audit_log
                order by created_at desc
                limit 15
                """;
        List<AuditRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new AuditRow(rs.getLong("audit_id"), rs.getString("event_type"),
                        rs.getString("action_code"), rs.getString("resource_type"),
                        rs.getString("username"), rs.getString("role_code"),
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
                limit 15
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

    public record SystemMaintenanceSummary(List<SystemMetric> metrics, List<SessionRow> sessions,
            List<LockedUserRow> lockedUsers, List<AuditRow> auditLogs, List<SyncLogRow> syncLogs) {
    }

    public record SystemMetric(String code, String label, long value, String unit, String level) {
    }

    public record SessionRow(long sessionId, String username, String realName, String roleCode,
            String loginIp, LocalDateTime createdAt, LocalDateTime expiresAt) {
    }

    public record LockedUserRow(long userId, String username, String realName, String roleCode,
            int failedLoginCount, LocalDateTime lockedUntil) {
    }

    public record AuditRow(long auditId, String eventType, String actionCode, String resourceType,
            String actorUsername, String actorRoleCode, String result, LocalDateTime createdAt) {
    }

    public record SyncLogRow(long syncLogId, String syncType, String sourceSystem, String targetTable,
            String syncStatus, String message, LocalDateTime createdAt) {
    }
}
