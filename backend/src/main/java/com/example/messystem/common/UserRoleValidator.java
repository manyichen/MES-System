package com.example.messystem.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class UserRoleValidator {
    private UserRoleValidator() {
    }

    public static void requireEnabledRole(long userId, String roleCode, String roleName) throws SQLException {
        String sql = """
                select 1
                from mes_user u
                join mes_user_role ur on ur.user_id = u.user_id
                join mes_role r on r.role_id = ur.role_id
                where u.user_id = ? and u.enabled = 1 and r.enabled = 1 and r.role_code = ?
                  and (ur.expires_at is null or ur.expires_at > current_timestamp)
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, roleCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new BadRequestException("只能分配给已启用的" + roleName);
                }
            }
        }
    }

    /** 返回当前仍启用且角色关联未过期的可派工人员，供业务表单生成下拉选项。 */
    public static List<AssignableUser> listEnabledUsers(String roleCode) throws SQLException {
        String sql = """
                select distinct u.user_id, u.username, u.real_name, r.role_code
                from mes_user u
                join mes_user_role ur on ur.user_id = u.user_id
                join mes_role r on r.role_id = ur.role_id
                where u.enabled = 1 and r.enabled = 1 and r.role_code = ?
                  and (ur.expires_at is null or ur.expires_at > current_timestamp)
                order by u.real_name, u.username
                """;
        try (Connection connection = Db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roleCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AssignableUser> users = new ArrayList<>();
                while (resultSet.next()) {
                    users.add(new AssignableUser(
                            resultSet.getLong("user_id"),
                            resultSet.getString("username"),
                            resultSet.getString("real_name"),
                            resultSet.getString("role_code"),
                            1));
                }
                return users;
            }
        }
    }

    public record AssignableUser(Long userId, String username, String realName, String roleCode, Integer enabled) {
    }
}
