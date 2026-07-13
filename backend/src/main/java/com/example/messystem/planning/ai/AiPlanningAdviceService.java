package com.example.messystem.planning.ai;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AiPlanningAdviceService {
    private final AiPlanningDataService dataService = new AiPlanningDataService();
    private final AiPlanningClient client = new AiPlanningClient();

    public AiPlanningAdviceResponse generate(AiPlanningAdviceRequest request) {
        Map<String, Object> snapshot = dataService.buildSnapshot(request);
        Map<String, Object> advice = client.requestAdvice(snapshot);

        AiPlanningAdviceResponse response = new AiPlanningAdviceResponse();
        response.enabled = true;
        response.model = AiPlanningConfig.model();
        response.generatedAt = LocalDateTime.now();
        response.inputTaskCount = dataService.validTaskIds(snapshot).size();
        response.advice = advice;
        response.validationWarnings = validateAdvice(advice, snapshot);
        return response;
    }

    private List<String> validateAdvice(Map<String, Object> advice, Map<String, Object> snapshot) {
        Set<Long> validTaskIds = dataService.validTaskIds(snapshot);
        Set<Long> validLineIds = dataService.validLineIds(snapshot);
        List<String> warnings = new ArrayList<>();
        Object tasks = advice.get("recommendedTasks");
        if (!(tasks instanceof List<?> rows)) {
            warnings.add("AI 未返回 recommendedTasks 数组，请只作为文字参考");
            return warnings;
        }
        for (Object item : rows) {
            if (!(item instanceof Map<?, ?> row)) continue;
            Long taskId = longValue(row.get("taskId"));
            Long lineId = longValue(row.get("suggestedLineId"));
            if (taskId == null || !validTaskIds.contains(taskId)) {
                warnings.add("AI 返回了不在本次候选范围内的生产任务 ID：" + row.get("taskId"));
            }
            if (lineId != null && !validLineIds.contains(lineId)) {
                warnings.add("AI 返回了不存在或不可选的产线 ID：" + lineId);
            }
        }
        return warnings;
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
