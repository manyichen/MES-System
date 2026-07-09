package com.example.messystem.dashboard.entity;

import java.time.LocalDateTime;

public record MesDashboardMetric(
        Long metricId,
        String metricKey,
        String metricName,
        String metricValue,
        String metricType,
        LocalDateTime createdAt
) {
}
