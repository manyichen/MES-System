package com.example.messystem.master.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.common.PasswordHasher;
import com.example.messystem.master.entity.MesUser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    public List<MesUser> listUsers() {
        String sql = """
                select user_id, username, real_name, role_code, department, phone, enabled,
                       created_at, updated_at, last_login_at
                from mes_user
                order by user_id
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesUser> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(mapUser(rs));
            }
            return rows;
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    public MesUser createUser(MesUser user) {
        requireUser(user);
        requireText(user.username, "username is required");
        requireText(user.password, "password is required");
        String roleCode = defaultText(user.roleCode, "PRODUCTION_OPERATOR");
        String sql = """
                insert into mes_user
                    (username, real_name, role_code, department, phone, enabled, password_hash, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, current_timestamp)
                returning user_id, username, real_name, role_code, department, phone, enabled,
                          created_at, updated_at, last_login_at
                """;
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, user.username.trim());
                statement.setString(2, defaultText(user.realName, user.username.trim()));
                statement.setString(3, roleCode);
                statement.setString(4, user.department);
                statement.setString(5, user.phone);
                statement.setInt(6, user.enabled == null ? 1 : user.enabled);
                statement.setString(7, PasswordHasher.hash(user.password));
                MesUser created;
                try (ResultSet rs = statement.executeQuery()) {
                    rs.next();
                    created = mapUser(rs);
                }
                linkRole(connection, created.userId, roleCode);
                connection.commit();
                return created;
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

    public MesUser updateRole(long userId, String roleCode) {
        requireText(roleCode, "roleCode is required");
        String sql = """
                update mes_user
                set role_code = ?,
                    updated_at = current_timestamp
                where user_id = ?
                returning user_id, username, real_name, role_code, department, phone, enabled,
                          created_at, updated_at, last_login_at
                """;
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, roleCode.trim());
                statement.setLong(2, userId);
                MesUser updated;
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) throw new NotFoundException("user not found");
                    updated = mapUser(rs);
                }
                try (PreparedStatement delete = connection.prepareStatement("delete from mes_user_role where user_id = ?")) {
                    delete.setLong(1, userId);
                    delete.executeUpdate();
                }
                linkRole(connection, userId, roleCode.trim());
                try (PreparedStatement revoke = connection.prepareStatement(
                        "update mes_user_session set revoked_at = current_timestamp where user_id = ? and revoked_at is null")) {
                    revoke.setLong(1, userId);
                    revoke.executeUpdate();
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

    private static void linkRole(Connection connection, long userId, String roleCode) throws SQLException {
        String sql = """
                insert into mes_user_role (user_id, role_id)
                select ?, role_id from mes_role where role_code = ? and enabled = 1
                on conflict (user_id, role_id) do nothing
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, roleCode);
            if (statement.executeUpdate() == 0) {
                throw new BadRequestException("roleCode does not exist or is disabled");
            }
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
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void requireUser(MesUser user) {
        if (user == null) {
            throw new BadRequestException("user is required");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }
}
