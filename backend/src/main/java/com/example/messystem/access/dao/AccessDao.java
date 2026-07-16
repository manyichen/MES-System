package com.example.messystem.access.dao;

import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.NotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;

/** 执行访问控制查询以及角色、权限申请的原子状态变更。 */
public class AccessDao {
    public List<RoleInfo> listRoles() {
        String sql = """
                select r.role_id, r.role_code, r.role_name, r.role_type, r.role_level,
                       r.data_scope, r.description,
                       count(distinct rp.permission_id) as permission_count,
                       count(distinct ur.user_id) as user_count
                from mes_role r
                left join mes_role_permission rp on rp.role_id = r.role_id
                left join mes_user_role ur on ur.role_id = r.role_id
                where r.enabled = 1
                group by r.role_id
                order by r.role_level, r.role_code
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<RoleInfo> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new RoleInfo(rs.getLong("role_id"), rs.getString("role_code"),
                        rs.getString("role_name"), rs.getString("role_type"), rs.getInt("role_level"),
                        rs.getString("data_scope"), rs.getString("description"),
                        rs.getInt("permission_count"), rs.getInt("user_count")));
            }
            return rows;
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    public List<PermissionInfo> listPermissions(String roleCode) {
        String sql = """
                select p.permission_code, p.permission_name, p.module_code, p.resource_type,
                       p.action_code, p.risk_level,
                       case when rp.role_id is null then false else true end as granted
                from mes_permission p
                left join mes_role r on r.role_code = ?
                left join mes_role_permission rp on rp.role_id = r.role_id and rp.permission_id = p.permission_id
                where p.enabled = 1
                order by p.module_code, p.permission_code
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roleCode);
            try (ResultSet rs = statement.executeQuery()) {
                List<PermissionInfo> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new PermissionInfo(rs.getString("permission_code"), rs.getString("permission_name"),
                            rs.getString("module_code"), rs.getString("resource_type"),
                            rs.getString("action_code"), rs.getString("risk_level"), rs.getBoolean("granted")));
                }
                return rows;
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    public Set<String> getUserRoles(long userId) {
        String sql = """
                select r.role_code from mes_user_role ur
                join mes_role r on r.role_id = ur.role_id
                where ur.user_id = ? and (ur.expires_at is null or ur.expires_at > current_timestamp)
                order by r.role_level, r.role_code
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                Set<String> roles = new LinkedHashSet<>();
                while (rs.next()) roles.add(rs.getString(1));
                return roles;
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    public Set<String> assignUserRoles(long userId, List<String> requestedRoles, AuthenticatedUser actor) {
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            throw new BadRequestException("用户至少需要一个角色");
        }
        Set<String> roleCodes = new LinkedHashSet<>();
        for (String roleCode : requestedRoles) {
            if (roleCode != null && !roleCode.isBlank()) roleCodes.add(roleCode.trim());
        }
        if (roleCodes.isEmpty()) throw new BadRequestException("用户至少需要一个有效角色");
        if (actor.user.userId == userId && actor.hasRole("SYSTEM_ADMIN") && !roleCodes.contains("SYSTEM_ADMIN")) {
            throw new BadRequestException("系统管理员不能移除自己的最高权限，请由另一名系统管理员操作");
        }

        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureUser(connection, userId);
                List<Long> roleIds = findRoleIds(connection, roleCodes);
                if (roleIds.size() != roleCodes.size()) throw new BadRequestException("包含不存在或已停用的角色");

                try (PreparedStatement delete = connection.prepareStatement("delete from mes_user_role where user_id = ?")) {
                    delete.setLong(1, userId);
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "insert into mes_user_role (user_id, role_id, assigned_by) values (?, ?, ?)")) {
                    for (Long roleId : roleIds) {
                        insert.setLong(1, userId);
                        insert.setLong(2, roleId);
                        insert.setLong(3, actor.user.userId);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                String primaryRole = roleCodes.iterator().next();
                try (PreparedStatement update = connection.prepareStatement(
                        "update mes_user set role_code = ?, updated_at = current_timestamp where user_id = ?")) {
                    update.setString(1, primaryRole);
                    update.setLong(2, userId);
                    update.executeUpdate();
                }
                try (PreparedStatement revoke = connection.prepareStatement(
                        "update mes_user_session set revoked_at = current_timestamp where user_id = ? and revoked_at is null")) {
                    revoke.setLong(1, userId);
                    revoke.executeUpdate();
                }
                writeAudit(connection, userId, roleCodes, actor);
                connection.commit();
                return roleCodes;
            } catch (RuntimeException | SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    public List<PermissionApplication> listPermissionApplications(AuthenticatedUser actor) {
        boolean all = actor.hasPermission("permission.review") || actor.hasPermission("role.manage");
        String sql = """
                select a.apply_id, a.apply_no, a.applicant_id, a.target_user_id, a.from_role_code,
                       a.to_role_code, a.apply_reason, a.apply_status, a.reviewer_id,
                       a.reviewed_at, a.review_comment, a.created_at
                from mes_permission_apply a
                """ + (all ? "" : " where a.applicant_id = ? ") + " order by a.apply_id asc";
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!all) statement.setLong(1, actor.user.userId);
            try (ResultSet rs = statement.executeQuery()) {
                List<PermissionApplication> rows = new ArrayList<>();
                while (rs.next()) rows.add(mapApplication(rs));
                return rows;
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    public PermissionApplication createPermissionApplication(long targetUserId, String toRoleCode,
            String reason, AuthenticatedUser actor) {
        if (toRoleCode == null || toRoleCode.isBlank()) throw new BadRequestException("申请角色不能为空");
        String sql = """
                insert into mes_permission_apply
                    (apply_no, applicant_id, target_user_id, from_role_code, to_role_code,
                     apply_reason, apply_status, updated_at)
                select ?, ?, u.user_id, u.role_code, ?, ?, 'SUBMITTED', current_timestamp
                from mes_user u
                where u.user_id = ?
                  and exists (select 1 from mes_role r where r.role_code = ? and r.enabled = 1)
                returning apply_id, apply_no, applicant_id, target_user_id, from_role_code,
                          to_role_code, apply_reason, apply_status, reviewer_id, reviewed_at,
                          review_comment, created_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "PA-" + System.currentTimeMillis());
            statement.setLong(2, actor.user.userId);
            statement.setString(3, toRoleCode.trim());
            statement.setString(4, reason);
            statement.setLong(5, targetUserId);
            statement.setString(6, toRoleCode.trim());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new BadRequestException("目标用户不存在，或申请角色无效");
                return mapApplication(rs);
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    public PermissionApplication reviewPermissionApplication(long applyId, String decision,
            String comment, AuthenticatedUser actor) {
        String status = "REJECTED".equalsIgnoreCase(decision) ? "REJECTED" : "REVIEWED";
        String sql = """
                update mes_permission_apply
                set apply_status = ?, reviewer_id = ?, reviewed_at = current_timestamp,
                    review_comment = ?, updated_at = current_timestamp
                where apply_id = ? and apply_status = 'SUBMITTED'
                returning apply_id, apply_no, applicant_id, target_user_id, from_role_code,
                          to_role_code, apply_reason, apply_status, reviewer_id, reviewed_at,
                          review_comment, created_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, actor.user.userId);
            statement.setString(3, comment);
            statement.setLong(4, applyId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new BadRequestException("只有待复核的申请可以处理");
                return mapApplication(rs);
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    public PermissionApplication applyPermissionApplication(long applyId, AuthenticatedUser actor) {
        PermissionApplication application = findApplication(applyId);
        if (!Set.of("SUBMITTED", "REVIEWED").contains(application.applyStatus())) {
            throw new BadRequestException("只有待处理或已复核申请可以最终应用");
        }
        assignUserRoles(application.targetUserId(), List.of(application.toRoleCode()), actor);
        String sql = """
                update mes_permission_apply
                set apply_status = 'APPLIED', reviewer_id = ?, reviewed_at = current_timestamp,
                    review_comment = coalesce(review_comment, '') || ' [系统管理员已应用]',
                    updated_at = current_timestamp
                where apply_id = ?
                returning apply_id, apply_no, applicant_id, target_user_id, from_role_code,
                          to_role_code, apply_reason, apply_status, reviewer_id, reviewed_at,
                          review_comment, created_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, actor.user.userId);
            statement.setLong(2, applyId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapApplication(rs);
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    private PermissionApplication findApplication(long applyId) {
        String sql = """
                select apply_id, apply_no, applicant_id, target_user_id, from_role_code,
                       to_role_code, apply_reason, apply_status, reviewer_id, reviewed_at,
                       review_comment, created_at
                from mes_permission_apply where apply_id = ?
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, applyId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("权限申请不存在");
                return mapApplication(rs);
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    private static PermissionApplication mapApplication(ResultSet rs) throws SQLException {
        java.sql.Timestamp reviewed = rs.getTimestamp("reviewed_at");
        java.sql.Timestamp created = rs.getTimestamp("created_at");
        return new PermissionApplication(rs.getLong("apply_id"), rs.getString("apply_no"),
                nullableLong(rs, "applicant_id"), rs.getLong("target_user_id"),
                rs.getString("from_role_code"), rs.getString("to_role_code"),
                rs.getString("apply_reason"), rs.getString("apply_status"),
                nullableLong(rs, "reviewer_id"), reviewed == null ? null : reviewed.toLocalDateTime(),
                rs.getString("review_comment"), created == null ? null : created.toLocalDateTime());
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static void ensureUser(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select 1 from mes_user where user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("用户不存在");
            }
        }
    }

    private static List<Long> findRoleIds(Connection connection, Set<String> roleCodes) throws SQLException {
        String placeholders = String.join(",", roleCodes.stream().map(item -> "?").toList());
        try (PreparedStatement statement = connection.prepareStatement(
                "select role_id from mes_role where enabled = 1 and role_code in (" + placeholders + ") order by role_level")) {
            int index = 1;
            for (String roleCode : roleCodes) statement.setString(index++, roleCode);
            try (ResultSet rs = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getLong(1));
                return ids;
            }
        }
    }

    private static void writeAudit(Connection connection, long targetUserId, Set<String> roles,
            AuthenticatedUser actor) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into mes_audit_log
                    (event_type, module_code, action_code, resource_type, resource_id, user_id,
                     username, role_code, request_method, request_path, result, message)
                values ('ROLE_CHANGE', 'system', 'update_role', 'user', ?, ?, ?, ?, 'PUT',
                        '/api/access/users/' || ? || '/roles', 'SUCCESS', ?)
                """)) {
            statement.setString(1, String.valueOf(targetUserId));
            statement.setLong(2, actor.user.userId);
            statement.setString(3, actor.user.username);
            statement.setString(4, actor.user.roleCode);
            statement.setLong(5, targetUserId);
            statement.setString(6, "角色修改为：" + String.join(", ", roles));
            statement.executeUpdate();
        }
    }

    private static IllegalStateException database(SQLException ex) {
        return new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
    }

    public record RoleInfo(long roleId, String roleCode, String roleName, String roleType, int roleLevel,
            String dataScope, String description, int permissionCount, int userCount) {
    }

    public record PermissionInfo(String permissionCode, String permissionName, String moduleCode,
            String resourceType, String actionCode, String riskLevel, boolean granted) {
    }

    public record PermissionApplication(long applyId, String applyNo, Long applicantId,
            long targetUserId, String fromRoleCode, String toRoleCode, String applyReason,
            String applyStatus, Long reviewerId, LocalDateTime reviewedAt, String reviewComment,
            LocalDateTime createdAt) {
    }
}
