package com.example.messystem.master.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.master.entity.MesUser;
import com.example.messystem.planning.dao.PlanningDao;
import java.sql.SQLException;
import java.util.List;

public class UserService {
    private final PlanningDao dao = new PlanningDao();

    public List<MesUser> listUsers() {
        return database(dao::listUsers);
    }

    public MesUser createUser(MesUser user) {
        requireText(user.username, "username is required");
        user.realName = user.realName == null || user.realName.isBlank() ? user.username : user.realName;
        user.roleCode = user.roleCode == null || user.roleCode.isBlank() ? "PMC_PLANNER" : user.roleCode;
        user.enabled = user.enabled == null ? 1 : user.enabled;
        return database(() -> dao.insertUser(user));
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }

    private static <T> T database(SqlCall<T> call) {
        try {
            return call.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("database operation failed: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T execute() throws SQLException;
    }
}
