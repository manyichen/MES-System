package com.example.messystem.auth.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.auth.dao.ProfileDao;
import com.example.messystem.master.entity.MesUser;
import java.sql.SQLException;

public class ProfileService {
    private final ProfileDao dao = new ProfileDao();

    public MesUser get(long userId) {
        try {
            return dao.findById(userId);
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
        try {
            return dao.update(userId, realName, phone, email, avatarUrl, bio);
        } catch (SQLException ex) {
            throw database(ex);
        }
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

    private static IllegalStateException database(SQLException ex) {
        return new IllegalStateException("database operation failed: " + ex.getMessage(), ex);
    }
}
