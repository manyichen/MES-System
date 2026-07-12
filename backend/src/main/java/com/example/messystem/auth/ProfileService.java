package com.example.messystem.auth;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.master.entity.MesUser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ProfileService {
    private static final String COLUMNS = """
            user_id, username, real_name, role_code, department, phone, email, avatar_url,
            profile_bio, employee_no, position_name, enabled, created_at, updated_at, last_login_at
            """;

    public MesUser get(long userId) {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select " + COLUMNS + " from mes_user where user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("用户不存在");
                return map(rs);
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    public MesUser update(long userId, MesUser profile) {
        if (profile == null) throw new BadRequestException("个人资料不能为空");
        String realName = text(profile.realName, 100, "姓名");
        String phone = optional(profile.phone, 50, "电话");
        String email = optional(profile.email, 150, "邮箱");
        String avatarUrl = optional(profile.avatarUrl, 1000, "头像地址");
        String bio = optional(profile.profileBio, 500, "个人简介");
        if (email != null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new BadRequestException("邮箱格式不正确");
        }
        String sql = """
                update mes_user set real_name = ?, phone = ?, email = ?, avatar_url = ?,
                    profile_bio = ?, updated_at = current_timestamp, updated_by = ?
                where user_id = ? returning %s
                """.formatted(COLUMNS);
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, realName);
            statement.setString(2, phone);
            statement.setString(3, email);
            statement.setString(4, avatarUrl);
            statement.setString(5, bio);
            statement.setLong(6, userId);
            statement.setLong(7, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("用户不存在");
                return map(rs);
            }
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    private static MesUser map(ResultSet rs) throws SQLException {
        MesUser user = new MesUser();
        user.userId = rs.getLong("user_id");
        user.username = rs.getString("username");
        user.realName = rs.getString("real_name");
        user.roleCode = rs.getString("role_code");
        user.department = rs.getString("department");
        user.phone = rs.getString("phone");
        user.email = rs.getString("email");
        user.avatarUrl = rs.getString("avatar_url");
        user.profileBio = rs.getString("profile_bio");
        user.employeeNo = rs.getString("employee_no");
        user.positionName = rs.getString("position_name");
        user.enabled = rs.getInt("enabled");
        user.createdAt = time(rs, "created_at");
        user.updatedAt = time(rs, "updated_at");
        user.lastLoginAt = time(rs, "last_login_at");
        return user;
    }

    private static String text(String value, int max, String label) {
        String result = optional(value, max, label);
        if (result == null) throw new BadRequestException(label + "不能为空");
        return result;
    }

    private static String optional(String value, int max, String label) {
        if (value == null || value.isBlank()) return null;
        String result = value.trim();
        if (result.length() > max) throw new BadRequestException(label + "长度不能超过" + max + "个字符");
        return result;
    }

    private static java.time.LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static IllegalStateException database(SQLException ex) {
        return new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
    }
}
