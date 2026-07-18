/*
 * 答辩定位：登录认证与会话 模块的 ProfileDao。
 * 分层职责：数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。
 * 典型调用链：Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
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

    /**
     * 数据访问：按主键查询记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：更新业务记录。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：把 JDBC 结果行映射为领域对象。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
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

    /**
     * 数据访问：执行 time 对应的业务步骤。
     * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。
     * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。
     */
    private static java.time.LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }
}
