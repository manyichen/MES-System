package com.example.messystem.planning.service;

import java.time.LocalDateTime;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.planning.ai.AiPlanningAdviceRequest;
import com.example.messystem.planning.ai.AiPlanningAdviceResponse;
import com.example.messystem.planning.ai.AiPlanningClient;
import com.example.messystem.planning.ai.AiPlanningConfig;
import java.util.Set;
import java.util.Map;

public class AiPlanningAdviceService {
    private final AiPlanningDataService dataService = new AiPlanningDataService();
    private final AiPlanningClient client = new AiPlanningClient();

    public AiPlanningAdviceResponse generate(AiPlanningAdviceRequest request) {
        Map<String, Object> snapshot = dataService.buildSnapshot(request);
        Map<String, Object> advice = client.requestAdvice(snapshot);
        validateConcreteAdvice(advice, dataService.validLineIds(snapshot));

        AiPlanningAdviceResponse response = new AiPlanningAdviceResponse();
        response.enabled = true;
        response.model = AiPlanningConfig.model();
        response.generatedAt = LocalDateTime.now();
        response.inputTaskCount = dataService.validTaskIds(snapshot).size();
        response.advice = advice;
        response.validationWarnings = java.util.List.of();
        return response;
    }

    private static void validateConcreteAdvice(Map<String, Object> advice, Set<Long> validLineIds) {
        String[] required = {"orderAssignment", "recommendedLine", "recommendedStart", "recommendedEnd", "deadlineAssessment", "overallAdvice", "schedulingMethod"};
        for (String field : required) {
            Object value = advice.get(field);
            if (value == null || String.valueOf(value).isBlank()) {
                throw new BadRequestException("AI 未返回完整的具体排产建议，请重新生成");
            }
        }
        Long lineId = toLong(advice.get("recommendedLineId"));
        if (lineId == null || !validLineIds.contains(lineId)) {
            throw new BadRequestException("AI 返回的推荐产线无效，请重新生成");
        }
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return value == null ? null : Long.valueOf(String.valueOf(value)); } catch (NumberFormatException ex) { return null; }
    }
}
