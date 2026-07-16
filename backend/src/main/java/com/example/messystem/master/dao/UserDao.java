package com.example.messystem.master.dao;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.common.Db;
import com.example.messystem.common.NotFoundException;
import com.example.messystem.master.entity.MesUser;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** 持久化用户账号、角色关系以及会话失效事务。 */
public class UserDao {
    private static final String USER_COLUMNS = """
            user_id, username, real_name, role_code, department, phone, enabled,
            created_at, updated_at, last_login_at
            """;

    public List<MesUser> findAll() throws SQLException {
        String sql = "select " + USER_COLUMNS + " from mes_user order by user_id";
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<MesUser> users = new ArrayList<>();
            while (rs.next()) users.add(mapUser(rs));
            return users;
        }
    }

    public MesUser insert(MesUser input, String username, String realName, String roleCode,
            int enabled, String passwordHash) throws SQLException {
        String sql = """
                insert into mes_user
                    (username, real_name, role_code, department, phone, enabled, password_hash, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, current_timestamp)
                returning %s
                """.formatted(USER_COLUMNS);
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                statement.setString(2, realName);
                statement.setString(3, roleCode);
                statement.setString(4, input.department);
                statement.setString(5, input.phone);
                statement.setInt(6, enabled);
                statement.setString(7, passwordHash);
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
        }
    }

    public MesUser replaceRole(long userId, String roleCode) throws SQLException {
        String sql = """
                update mes_user set role_code = ?, updated_at = current_timestamp
                where user_id = ? returning %s
                """.formatted(USER_COLUMNS);
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, roleCode);
                statement.setLong(2, userId);
                MesUser updated;
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) throw new NotFoundException("user not found");
                    updated = mapUser(rs);
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "delete from mes_user_role where user_id = ?")) {
                    delete.setLong(1, userId);
                    delete.executeUpdate();
                }
                linkRole(connection, userId, roleCode);
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
