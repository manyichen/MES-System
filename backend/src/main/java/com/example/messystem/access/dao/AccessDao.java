/*
 * 答辩定位：访问控制与系统维护 模块的 AccessDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.access.dao;

import com.example.messystem.auth.AuthenticatedUser;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.common.PasswordHasher;
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
    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：查询单条记录或详情。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：分配执行人员或资源。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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
        if (actor.user.userId == userId && actor.isSuperAdmin()
                && !roleCodes.contains(AuthenticatedUser.SUPER_ADMIN_ROLE)) {
            throw new BadRequestException("超级管理员不能移除自己的最高权限，请由另一名超级管理员操作");
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

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：创建业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：审核业务申请。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：执行已审核的变更。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：查询列表。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public List<AccountApplication> listAccountApplications(boolean all, long applicantId) {
        String sql = """
                select apply_id, apply_no, applicant_id, username, real_name, role_code,
                       department, phone, apply_reason, apply_status, reviewer_id,
                       reviewed_at, review_comment, created_user_id, created_at
                from mes_account_apply
                """ + (all ? "" : " where applicant_id = ? ") + " order by apply_id asc";
        try (Connection connection = Db.getConnection()) {
            ensureAccountApplicationTable(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if (!all) statement.setLong(1, applicantId);
                try (ResultSet rs = statement.executeQuery()) {
                    List<AccountApplication> rows = new ArrayList<>();
                    while (rs.next()) rows.add(mapAccountApplication(rs));
                    return rows;
                }
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    /**
     * 数据访问：创建业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public AccountApplication createAccountApplication(AccountApplicationRequest request, long applicantId) {
        if (request == null) throw new BadRequestException("账号申请不能为空");
        String username = normalizeUsername(request.username());
        String realName = requireTextValue(request.realName(), "姓名不能为空");
        String roleCode = requireTextValue(request.roleCode(), "申请角色不能为空").toUpperCase();
        if ("SYSTEM_ADMIN".equals(roleCode)) throw new BadRequestException("不能通过账号申请创建系统管理员账号");
        String passwordHash = PasswordHasher.hash(requireTextValue(request.password(), "初始密码不能为空"));
        if (request.password().trim().length() < 6) throw new BadRequestException("初始密码至少需要 6 位");

        String sql = """
                insert into mes_account_apply
                    (apply_no, applicant_id, username, real_name, role_code, department,
                     phone, password_hash, apply_reason, apply_status, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, 'SUBMITTED', current_timestamp)
                returning apply_id, apply_no, applicant_id, username, real_name, role_code,
                          department, phone, apply_reason, apply_status, reviewer_id,
                          reviewed_at, review_comment, created_user_id, created_at
                """;
        try (Connection connection = Db.getConnection()) {
            ensureAccountApplicationTable(connection);
            if (usernameExists(connection, username)) throw new BadRequestException("账号已存在");
            if (submittedAccountApplicationExists(connection, username)) {
                throw new BadRequestException("该登录账号已有待审核申请");
            }
            ensureRoleExists(connection, roleCode);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, "AA-" + System.currentTimeMillis());
                statement.setLong(2, applicantId);
                statement.setString(3, username);
                statement.setString(4, realName);
                statement.setString(5, roleCode);
                statement.setString(6, trimToNull(request.department()));
                statement.setString(7, trimToNull(request.phone()));
                statement.setString(8, passwordHash);
                statement.setString(9, trimToNull(request.reason()));
                try (ResultSet rs = statement.executeQuery()) {
                    rs.next();
                    return mapAccountApplication(rs);
                }
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    /**
     * 数据访问：审核业务申请。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public AccountApplication reviewAccountApplication(long applyId, String decision,
            String comment, long actorUserId) {
        String normalizedDecision = String.valueOf(decision == null ? "" : decision).trim().toUpperCase();
        if (!Set.of("APPROVED", "REJECTED").contains(normalizedDecision)) {
            throw new BadRequestException("审核结果只能是通过或拒绝");
        }
        try (Connection connection = Db.getConnection()) {
            ensureAccountApplicationTable(connection);
            connection.setAutoCommit(false);
            try {
                AccountApplication application = findAccountApplicationForUpdate(connection, applyId);
                if (!"SUBMITTED".equals(application.applyStatus())) {
                    throw new BadRequestException("只有待审批的账号申请可以处理");
                }
                if (application.applicantId() != null && application.applicantId() == actorUserId) {
                    throw new BadRequestException("账号申请人不能审核自己的申请");
                }
                AccountApplication reviewed;
                if ("REJECTED".equals(normalizedDecision)) {
                    reviewed = updateAccountApplicationStatus(connection, applyId, "REJECTED",
                            actorUserId, comment, null);
                    writeAccountApplicationAudit(connection, actorUserId, "reject_account_application",
                            reviewed, "账号申请已拒绝");
                } else {
                    if (usernameExists(connection, application.username())) {
                        throw new BadRequestException("申请的登录账号已存在，不能通过");
                    }
                    ensureRoleExists(connection, application.roleCode());
                    long userId = createUserFromAccountApplication(connection, application);
                    linkRole(connection, userId, application.roleCode(), actorUserId);
                    reviewed = updateAccountApplicationStatus(connection, applyId, "APPROVED",
                            actorUserId, comment, userId);
                    writeAccountApplicationAudit(connection, actorUserId, "approve_account_application",
                            reviewed, "账号申请已通过并创建账号");
                }
                connection.commit();
                return reviewed;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    /**
     * 数据访问：查询匹配记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：查询匹配记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static AccountApplication findAccountApplicationForUpdate(Connection connection, long applyId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select apply_id, apply_no, applicant_id, username, real_name, role_code,
                       department, phone, apply_reason, apply_status, reviewer_id,
                       reviewed_at, review_comment, created_user_id, created_at
                from mes_account_apply
                where apply_id = ?
                for update
                """)) {
            statement.setLong(1, applyId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("账号申请不存在");
                return mapAccountApplication(rs);
            }
        }
    }

    /**
     * 数据访问：更新业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static AccountApplication updateAccountApplicationStatus(Connection connection, long applyId,
            String status, long reviewerId, String comment, Long createdUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                update mes_account_apply
                set apply_status = ?, reviewer_id = ?, reviewed_at = current_timestamp,
                    review_comment = ?, created_user_id = ?, updated_at = current_timestamp
                where apply_id = ?
                returning apply_id, apply_no, applicant_id, username, real_name, role_code,
                          department, phone, apply_reason, apply_status, reviewer_id,
                          reviewed_at, review_comment, created_user_id, created_at
                """)) {
            statement.setString(1, status);
            statement.setLong(2, reviewerId);
            statement.setString(3, trimToNull(comment));
            if (createdUserId == null) statement.setNull(4, java.sql.Types.BIGINT);
            else statement.setLong(4, createdUserId);
            statement.setLong(5, applyId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapAccountApplication(rs);
            }
        }
    }

    /**
     * 数据访问：创建业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static long createUserFromAccountApplication(Connection connection,
            AccountApplication application) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into mes_user
                    (username, real_name, role_code, department, phone, enabled, password_hash, updated_at)
                select username, real_name, role_code, department, phone, 1, password_hash, current_timestamp
                from mes_account_apply
                where apply_id = ?
                returning user_id
                """)) {
            statement.setLong(1, application.applyId());
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong("user_id");
            }
        }
    }

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static AccountApplication mapAccountApplication(ResultSet rs) throws SQLException {
        java.sql.Timestamp reviewed = rs.getTimestamp("reviewed_at");
        java.sql.Timestamp created = rs.getTimestamp("created_at");
        return new AccountApplication(rs.getLong("apply_id"), rs.getString("apply_no"),
                nullableLong(rs, "applicant_id"), rs.getString("username"),
                rs.getString("real_name"), rs.getString("role_code"), rs.getString("department"),
                rs.getString("phone"), rs.getString("apply_reason"), rs.getString("apply_status"),
                nullableLong(rs, "reviewer_id"), reviewed == null ? null : reviewed.toLocalDateTime(),
                rs.getString("review_comment"), nullableLong(rs, "created_user_id"),
                created == null ? null : created.toLocalDateTime());
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
     * 数据访问：执行 ensureUser 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void ensureUser(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select 1 from mes_user where user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("用户不存在");
            }
        }
    }

    /**
     * 数据访问：执行 ensureAccountApplicationTable 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void ensureAccountApplicationTable(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("""
                    create table if not exists mes_account_apply (
                        apply_id bigserial primary key,
                        apply_no varchar(50) not null,
                        applicant_id bigint,
                        username varchar(50) not null,
                        real_name varchar(100) not null,
                        role_code varchar(50) not null,
                        department varchar(100),
                        phone varchar(30),
                        password_hash varchar(255) not null,
                        apply_reason varchar(500),
                        apply_status varchar(30) not null default 'SUBMITTED',
                        reviewer_id bigint,
                        reviewed_at timestamp,
                        review_comment varchar(500),
                        created_user_id bigint,
                        created_at timestamp not null default current_timestamp,
                        updated_at timestamp not null default current_timestamp
                    )
                    """);
            statement.execute("create unique index if not exists uk_mes_account_apply_apply_no on mes_account_apply (apply_no)");
            statement.execute("create index if not exists idx_mes_account_apply_applicant_id on mes_account_apply (applicant_id)");
            statement.execute("create index if not exists idx_mes_account_apply_status on mes_account_apply (apply_status)");
            statement.execute("create index if not exists idx_mes_account_apply_username_status on mes_account_apply (lower(username), apply_status)");
        }
    }

    /**
     * 数据访问：规范化输入并补齐默认值。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static String normalizeUsername(String username) {
        String value = requireTextValue(username, "登录账号不能为空");
        if (value.length() < 3 || value.length() > 50) {
            throw new BadRequestException("登录账号长度需要在 3-50 个字符之间");
        }
        if (!value.matches("[A-Za-z0-9_.-]+")) {
            throw new BadRequestException("登录账号只能包含字母、数字、下划线、点和短横线");
        }
        return value;
    }

    /**
     * 数据访问：执行 requireTextValue 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static String requireTextValue(String value, String message) {
        if (value == null || value.isBlank()) throw new BadRequestException(message);
        return value.trim();
    }

    /**
     * 数据访问：执行 trimToNull 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 数据访问：执行 usernameExists 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static boolean usernameExists(Connection connection, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select 1 from mes_user where lower(username) = lower(?) limit 1")) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 数据访问：提交业务事项。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static boolean submittedAccountApplicationExists(Connection connection, String username)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select 1 from mes_account_apply
                where lower(username) = lower(?)
                  and apply_status = 'SUBMITTED'
                limit 1
                """)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 数据访问：执行 ensureRoleExists 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void ensureRoleExists(Connection connection, String roleCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select 1 from mes_role where role_code = ? and enabled = 1")) {
            statement.setString(1, roleCode);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new BadRequestException("申请角色不存在或已停用");
            }
        }
    }

    /**
     * 数据访问：执行 linkRole 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void linkRole(Connection connection, long userId, String roleCode, long assignedBy)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into mes_user_role (user_id, role_id, assigned_by)
                select ?, role_id, ?
                from mes_role
                where role_code = ? and enabled = 1
                on conflict (user_id, role_id) do nothing
                """)) {
            statement.setLong(1, userId);
            statement.setLong(2, assignedBy);
            statement.setString(3, roleCode);
            if (statement.executeUpdate() == 0) {
                throw new BadRequestException("申请角色不存在或已停用");
            }
        }
    }

    /**
     * 数据访问：查询匹配记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：执行 writeAudit 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：执行 writeAccountApplicationAudit 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static void writeAccountApplicationAudit(Connection connection, long actorUserId, String actionCode,
            AccountApplication application, String message) throws SQLException {
        try (PreparedStatement actor = connection.prepareStatement(
                "select username, role_code from mes_user where user_id = ?")) {
            actor.setLong(1, actorUserId);
            try (ResultSet rs = actor.executeQuery();
                    PreparedStatement statement = connection.prepareStatement("""
                            insert into mes_audit_log
                                (event_type, module_code, action_code, resource_type, resource_id, user_id,
                                 username, role_code, result, message)
                            values ('ACCOUNT_APPLICATION', 'system', ?, 'account_apply', ?, ?, ?, ?, 'SUCCESS', ?)
                            """)) {
                String username = null;
                String roleCode = null;
                if (rs.next()) {
                    username = rs.getString("username");
                    roleCode = rs.getString("role_code");
                }
                statement.setString(1, actionCode);
                statement.setString(2, String.valueOf(application.applyId()));
                statement.setLong(3, actorUserId);
                statement.setString(4, username);
                statement.setString(5, roleCode);
                statement.setString(6, message);
                statement.executeUpdate();
            }
        }
    }

    /**
     * 数据访问：执行 database 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static IllegalStateException database(SQLException ex) {
        return new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
    }

    /**
     * 数据访问：执行 RoleInfo 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record RoleInfo(long roleId, String roleCode, String roleName, String roleType, int roleLevel,
            String dataScope, String description, int permissionCount, int userCount) {
    }

    /**
     * 数据访问：执行 PermissionInfo 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record PermissionInfo(String permissionCode, String permissionName, String moduleCode,
            String resourceType, String actionCode, String riskLevel, boolean granted) {
    }

    /**
     * 数据访问：执行 PermissionApplication 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record PermissionApplication(long applyId, String applyNo, Long applicantId,
            long targetUserId, String fromRoleCode, String toRoleCode, String applyReason,
            String applyStatus, Long reviewerId, LocalDateTime reviewedAt, String reviewComment,
            LocalDateTime createdAt) {
    }

    /**
     * 数据访问：执行 AccountApplication 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record AccountApplication(long applyId, String applyNo, Long applicantId,
            String username, String realName, String roleCode, String department, String phone,
            String applyReason, String applyStatus, Long reviewerId, LocalDateTime reviewedAt,
            String reviewComment, Long createdUserId, LocalDateTime createdAt) {
    }

    /**
     * 数据访问：执行 AccountApplicationRequest 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    public record AccountApplicationRequest(String username, String password, String realName,
            String roleCode, String department, String phone, String reason) {
    }
}
