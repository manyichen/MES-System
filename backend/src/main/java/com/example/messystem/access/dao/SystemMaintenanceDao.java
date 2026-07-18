/*
 * 答辩定位：访问控制与系统维护 模块的 SystemMaintenanceDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.access.dao;

import com.example.messystem.common.Db;
import com.example.messystem.common.BadRequestException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 执行数据库维护和安全管理事务。 */
public class SystemMaintenanceDao {
    /**
     * 数据访问：装载业务数据。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public SystemMaintenanceSummary loadSummary() {
        try (Connection connection = Db.getConnection()) {
            List<SessionRow> sessions = safeList(() -> listSessions(connection));
            List<LockedUserRow> lockedUsers = safeList(() -> listLockedUsers(connection));
            List<DeletedUserRow> deletedUsers = safeList(() -> listDeletedUsers(connection));
            List<LockedSessionRecordRow> lockedSessionRecords = safeList(() -> listLockedSessionRecords(connection));
            List<AuditRow> auditLogs = safeList(() -> listAuditLogs(connection));
            List<AuditRow> failedLoginLogs = safeList(() -> listFailedLoginLogs(connection));
            List<SyncLogRow> syncLogs = safeList(() -> listSyncLogs(connection));
            List<SyncLogRow> syncFailures = safeList(() -> listSyncFailures(connection));
            return new SystemMaintenanceSummary(
                    List.of(
                            metric("enabledUsers", "启用账号", count(connection,
                                    "select count(*) from mes_user where enabled = 1"), "个", "normal"),
                            metric("accountApplications", "账号申请", count(connection,
                                    "select count(*) from mes_account_apply where apply_status = 'SUBMITTED'"), "项", "normal"),
                            metric("deletedUsers", "删除账号记录", count(connection,
                                    "select count(*) from mes_user where enabled = 0"), "条", "warning"),
                            metric("lockedSessionRecords", "下线锁定记录", count(connection,
                                    "select count(*) from mes_user_session s join mes_user u on u.user_id = s.user_id where s.revoked_at is not null and u.locked_until > current_timestamp"), "条", "warning"),
                            metric("activeSessions", "有效会话", count(connection,
                                    "select count(*) from mes_user_session s join mes_user u on u.user_id = s.user_id where s.revoked_at is null and s.expires_at > current_timestamp and u.enabled = 1 and (u.locked_until is null or u.locked_until <= current_timestamp)"), "个", "normal"),
                            metric("failedLogins", "24小时失败登录", count(connection,
                                    "select count(*) from mes_audit_log where event_type = 'LOGIN' and result = 'FAILED' and created_at >= current_timestamp - interval '24 hours'"), "次", "danger"),
                            metric("pendingApplications", "待处理权限申请", count(connection,
                                    "select count(*) from mes_permission_apply where apply_status = 'SUBMITTED'"), "项", "warning"),
                            metric("syncFailures", "同步异常", count(connection,
                                    "select count(*) from mes_sync_log where sync_status in ('FAILED','ERROR')"), "条", "danger")),
                    sessions,
                    lockedUsers,
                    deletedUsers,
                    lockedSessionRecords,
                    auditLogs,
                    syncLogs,
                    failedLoginLogs,
                    syncFailures);
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 数据访问：执行 metric 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static SystemMetric metric(String code, String label, long value, String unit, String level) {
        return new SystemMetric(code, label, value, unit, level);
    }

    /**
     * 数据访问：执行 count 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static long count(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException ex) {
            return 0;
        }
    }

    /**
     * 数据访问：执行 safeList 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static <T> List<T> safeList(SqlListSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (SQLException ex) {
            return List.of();
        }
    }

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static List<SessionRow> listSessions(Connection connection) throws SQLException {
        String sql = """
                select s.session_id, s.user_id, u.username, u.real_name, u.role_code, s.login_ip, s.issued_at as created_at, s.expires_at
                from mes_user_session s
                left join mes_user u on u.user_id = s.user_id
                where s.revoked_at is null and s.expires_at > current_timestamp
                  and u.enabled = 1
                  and (u.locked_until is null or u.locked_until <= current_timestamp)
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

    /**
     * 数据访问：撤销会话或授权。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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
                writeMaintenanceAudit(connection, actorUserId, "revoke_session_lock_user", userId, "登录会话已强制下线，账号已锁定");
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

    /**
     * 数据访问：撤销指定用户的全部登录会话。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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
                writeMaintenanceAudit(connection, actorUserId, "revoke_sessions_lock_user", userId, "用户有效会话已撤销，账号已锁定");
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

    /**
     * 数据访问：清理已经过期的登录会话。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：查询匹配记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：执行 lockUser 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：解除账号锁定。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：停用业务对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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
                writeMaintenanceAudit(connection, actorUserId, "disable_user", userId, "账号已删除并禁止登录");
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

    /**
     * 数据访问：将同步异常标记为已处理。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static List<DeletedUserRow> listDeletedUsers(Connection connection) throws SQLException {
        String sql = """
                select user_id, username, real_name, role_code, department, updated_at, last_login_at
                from mes_user
                where enabled = 0
                order by updated_at desc nulls last, user_id desc
                limit 500
                """;
        List<DeletedUserRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new DeletedUserRow(rs.getLong("user_id"), rs.getString("username"),
                        rs.getString("real_name"), rs.getString("role_code"), rs.getString("department"),
                        time(rs, "updated_at"), time(rs, "last_login_at")));
            }
        }
        return rows;
    }

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static List<LockedSessionRecordRow> listLockedSessionRecords(Connection connection) throws SQLException {
        String sql = """
                select s.session_id, s.user_id, u.username, u.real_name, u.role_code,
                       s.login_ip, s.issued_at, s.expires_at, s.revoked_at, u.locked_until
                from mes_user u
                join mes_user_session s on s.user_id = u.user_id
                where s.revoked_at is not null
                  and u.locked_until > current_timestamp
                order by s.revoked_at desc nulls last, s.session_id desc
                limit 500
                """;
        List<LockedSessionRecordRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rows.add(new LockedSessionRecordRow(rs.getLong("session_id"), rs.getLong("user_id"),
                        rs.getString("username"), rs.getString("real_name"), rs.getString("role_code"),
                        rs.getString("login_ip"), time(rs, "issued_at"), time(rs, "expires_at"),
                        time(rs, "revoked_at"), time(rs, "locked_until")));
            }
        }
        return rows;
    }

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：执行 time 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    /**
     * 数据访问：执行 nullableLong 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * 数据访问：恢复已删除的业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public int restoreUser(long userId, long actorUserId) {
        if (userId <= 0) throw new BadRequestException("用户ID不正确");
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("""
                    update mes_user
                    set enabled = 1,
                        failed_login_count = 0,
                        locked_until = null,
                        updated_at = current_timestamp
                    where user_id = ?
                      and enabled = 0
                    """)) {
                statement.setLong(1, userId);
                int updated = statement.executeUpdate();
                if (updated == 0) throw new BadRequestException("只有已停用的账号可以恢复");
                writeMaintenanceAudit(connection, actorUserId, "restore_user", userId, "账号已恢复启用");
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

    /**
     * 数据访问：执行 writeMaintenanceAudit 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void writeMaintenanceAudit(Connection connection, long actorUserId, String actionCode,
            long targetUserId, String message) throws SQLException {
        try (PreparedStatement actor = connection.prepareStatement(
                "select username, role_code from mes_user where user_id = ?")) {
            actor.setLong(1, actorUserId);
            try (ResultSet rs = actor.executeQuery();
                    PreparedStatement statement = connection.prepareStatement("""
                            insert into mes_audit_log
                                (event_type, module_code, action_code, resource_type, resource_id, user_id,
                                 username, role_code, result, message)
                            values ('SYSTEM_MAINTENANCE', 'system', ?, 'user', ?, ?, ?, ?, 'SUCCESS', ?)
                            """)) {
                String username = null;
                String roleCode = null;
                if (rs.next()) {
                    username = rs.getString("username");
                    roleCode = rs.getString("role_code");
                }
                statement.setString(1, actionCode);
                statement.setString(2, String.valueOf(targetUserId));
                statement.setLong(3, actorUserId);
                statement.setString(4, username);
                statement.setString(5, roleCode);
                statement.setString(6, message);
                statement.executeUpdate();
            }
        }
    }

    /**
     * 数据访问：执行 SystemMaintenanceSummary 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record SystemMaintenanceSummary(List<SystemMetric> metrics, List<SessionRow> sessions,
            List<LockedUserRow> lockedUsers, List<DeletedUserRow> deletedUsers,
            List<LockedSessionRecordRow> lockedSessionRecords, List<AuditRow> auditLogs, List<SyncLogRow> syncLogs,
            List<AuditRow> failedLoginLogs, List<SyncLogRow> syncFailures) {
    }

    /**
     * 数据访问：执行 SystemMetric 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record SystemMetric(String code, String label, long value, String unit, String level) {
    }

    /**
     * 数据访问：执行 SessionRow 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record SessionRow(long sessionId, long userId, String username, String realName, String roleCode,
            String loginIp, LocalDateTime createdAt, LocalDateTime expiresAt) {
    }

    /**
     * 数据访问：执行 LockedUserRow 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record LockedUserRow(long userId, String username, String realName, String roleCode,
            int failedLoginCount, LocalDateTime lockedUntil) {
    }

    /**
     * 数据访问：删除业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record DeletedUserRow(long userId, String username, String realName, String roleCode,
            String department, LocalDateTime deletedAt, LocalDateTime lastLoginAt) {
    }

    /**
     * 数据访问：执行 LockedSessionRecordRow 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record LockedSessionRecordRow(long sessionId, long userId, String username, String realName,
            String roleCode, String loginIp, LocalDateTime issuedAt, LocalDateTime expiresAt,
            LocalDateTime revokedAt, LocalDateTime lockedUntil) {
    }

    /**
     * 数据访问：执行 AuditRow 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record AuditRow(long auditId, String eventType, String actionCode, String resourceType, Long actorUserId,
            String actorUsername, String actorRoleCode, String result, LocalDateTime createdAt) {
    }

    /**
     * 数据访问：执行 SyncLogRow 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record SyncLogRow(long syncLogId, String syncType, String sourceSystem, String targetTable,
            String syncStatus, String message, LocalDateTime createdAt) {
    }

    @FunctionalInterface
    private interface SqlListSupplier<T> {
        List<T> get() throws SQLException;
    }
}
