package com.example.messystem.auth.dao;

import com.example.messystem.common.Db;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.master.entity.MesUser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/** 持久化用户在个人资料中允许自行修改的字段。 */
public class ProfileDao {
    private static final String COLUMNS = """
            user_id, username, real_name, role_code, department, phone, email, avatar_url,
            profile_bio, employee_no, position_name, enabled, created_at, updated_at, last_login_at
            """;

    public MesUser findById(long userId) throws SQLException {
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select " + COLUMNS + " from mes_user where user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NotFoundException("用户不存在");
                return map(rs);
            }
        }
    }

    public MesUser update(long userId, String realName, String phone, String email,
            String avatarUrl, String bio) throws SQLException {
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

    private static java.time.LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
