package com.example.messystem.auth;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.PasswordHasher;
import com.example.messystem.master.entity.MesUser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class AuthService {
    public LoginSession login(LoginRequest request) {
        if (request == null || request.username == null || request.username.isBlank()
                || request.password == null || request.password.isBlank()) {
            throw new BadRequestException("username and password are required");
        }
        String sql = """
                select user_id, username, real_name, role_code, department, phone, enabled,
                       password_hash, created_at, updated_at, last_login_at
                from mes_user
                where username = ?
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, request.username.trim());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new BadRequestException("invalid username or password");
                }
                if (rs.getInt("enabled") != 1) {
                    throw new BadRequestException("user is disabled");
                }
                String passwordHash = rs.getString("password_hash");
                if (!PasswordHasher.verify(request.password, passwordHash)) {
                    throw new BadRequestException("invalid username or password");
                }
                MesUser user = mapUser(rs);
                updateLastLogin(connection, user.userId);
                return new LoginSession(UUID.randomUUID().toString(), user);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
        }
    }

    private static void updateLastLogin(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "update mes_user set last_login_at = current_timestamp where user_id = ?")) {
            statement.setLong(1, userId);
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
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
