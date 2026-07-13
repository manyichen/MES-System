package com.example.messystem.dashboard.entity;

import java.time.LocalDateTime;

public record MesManagementFeedback(
        Long feedbackId,
        String feedbackNo,
        Long orderId,
        Long taskId,
        Long workOrderId,
        String feedbackType,
        String feedbackContent,
        String decisionAction,
        String feedbackStatus,
        LocalDateTime createdAt,
        LocalDateTime closedAt
) {
}
