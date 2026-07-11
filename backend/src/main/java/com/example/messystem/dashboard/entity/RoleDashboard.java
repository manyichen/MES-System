package com.example.messystem.dashboard.entity;

import java.util.List;
import java.util.Set;

public record RoleDashboard(
        String primaryRole,
        String roleName,
        String dataScope,
        Set<String> visibleModules,
        List<String> prohibitedActions,
        List<DashboardCard> metrics,
        List<DashboardTodo> todos) {

    public record DashboardCard(String code, String label, String value, String unit,
            String level, String targetTab) {
    }

    public record DashboardTodo(String code, String title, String description, long count,
            String priority, String targetTab, String targetAnchor, String requiredPermission) {
    }
}
