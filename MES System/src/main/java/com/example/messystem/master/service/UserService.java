package com.example.messystem.master.service;

import com.example.messystem.common.BadRequestException;
import com.example.messystem.planning.service.PlanningStore;
import com.example.messystem.master.entity.MesUser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    public List<MesUser> listUsers() {
        return new ArrayList<>(PlanningStore.users.values());
    }

    public MesUser createUser(MesUser user) {
        requireText(user.username, "username is required");
        user.userId = PlanningStore.nextId();
        user.realName = user.realName == null || user.realName.isBlank() ? user.username : user.realName;
        user.roleCode = user.roleCode == null || user.roleCode.isBlank() ? "PMC_PLANNER" : user.roleCode;
        user.enabled = user.enabled == null ? 1 : user.enabled;
        user.createdAt = LocalDateTime.now();
        user.updatedAt = user.createdAt;
        PlanningStore.users.put(user.userId, user);
        return user;
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }
}
