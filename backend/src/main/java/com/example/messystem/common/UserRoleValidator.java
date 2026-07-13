package com.example.messystem.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}
