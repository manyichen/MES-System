/*
 * 答辩定位：公共基础设施 模块的 UserRoleValidator。
 * 分层职责：公共支撑代码：提供多个业务模块共享的响应、异常、编码或工具能力。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 公共基础设施 的 UserRoleValidator，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public final class UserRoleValidator {
    /**
     * 内部实现步骤：执行 UserRoleValidator 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private UserRoleValidator() {
    }

    /**
     * 公共能力：执行 requireEnabledRole 对应的业务步骤。
     * 由 UserRoleValidator 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
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

    /**
     * 公共能力：分配执行人员或资源。
     * 由 UserRoleValidator 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public record AssignableUser(Long userId, String username, String realName, String roleCode, Integer enabled) {
    }
}
