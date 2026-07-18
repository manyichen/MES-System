/*
 * 答辩定位：登录认证与会话 模块的 ProfileService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.auth.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.auth.dao.ProfileDao;
import com.example.messystem.master.entity.MesUser;
import java.sql.SQLException;

/**
 * 登录认证与会话 的 ProfileService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class ProfileService {
    /** 数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射。 */
    private final ProfileDao dao = new ProfileDao();

    /**
     * 业务用例：查询单条记录或详情。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    public MesUser get(long userId) {
        try {
            return dao.findById(userId);
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    /**
     * 业务用例：更新业务记录。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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
        try {
            return dao.update(userId, realName, phone, email, avatarUrl, bio);
        } catch (SQLException ex) {
            throw database(ex);
        }
    }

    /**
     * 业务用例：执行 text 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static String text(String value, int max, String label) {
        String result = optional(value, max, label);
        if (result == null) throw new BadRequestException(label + "不能为空");
        return result;
    }

    /**
     * 业务用例：执行 optional 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static String optional(String value, int max, String label) {
        if (value == null || value.isBlank()) return null;
        String result = value.trim();
        if (result.length() > max) throw new BadRequestException(label + "长度不能超过" + max + "个字符");
        return result;
    }

    /**
     * 业务用例：执行 database 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static IllegalStateException database(SQLException ex) {
        return new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
    }
}
