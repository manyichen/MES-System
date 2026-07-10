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
        String username = user.username.trim();
        String sql = """
                insert into mes_user
                    (username, real_name, role_code, department, phone, enabled, password_hash, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, current_timestamp)
                returning user_id, username, real_name, role_code, department, phone, enabled,
                          created_at, updated_at, last_login_at
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, defaultText(user.realName, username));
            statement.setString(3, defaultText(user.roleCode, "PRODUCTION_OPERATOR"));
            statement.setString(4, user.department);
            statement.setString(5, user.phone);
            statement.setInt(6, user.enabled == null ? 1 : user.enabled);
            statement.setString(7, PasswordHasher.hash(user.password));
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return mapUser(rs);
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
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roleCode.trim());
            statement.setLong(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new NotFoundException("user not found");
                }
                return mapUser(rs);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
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
