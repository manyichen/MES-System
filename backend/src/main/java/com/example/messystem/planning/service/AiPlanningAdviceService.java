/*
 * 答辩定位：订单、计划、齐套与工单 模块的 AiPlanningAdviceService。
 * 分层职责：业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。
 * 典型调用链：Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.service;

import java.time.LocalDateTime;
import com.example.messystem.common.BadRequestException;
import com.example.messystem.planning.ai.AiPlanningAdviceRequest;
import com.example.messystem.planning.ai.AiPlanningAdviceResponse;
import com.example.messystem.planning.ai.AiPlanningClient;
import com.example.messystem.planning.ai.AiPlanningConfig;
import java.util.Set;
import java.util.Map;

/**
 * 订单、计划、齐套与工单 的 AiPlanningAdviceService，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public class AiPlanningAdviceService {
    /** 业务服务依赖；控制器只通过它编排用例，不直接访问数据库。 */
    private final AiPlanningDataService dataService = new AiPlanningDataService();
    private final AiPlanningClient client = new AiPlanningClient();

    /**
     * 业务用例：生成业务结果。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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

    /**
     * 业务用例：校验业务输入与约束。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
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

    /**
     * 业务用例：执行 toLong 对应的业务步骤。
     * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。
     * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。
     */
    private static Long toLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return value == null ? null : Long.valueOf(String.valueOf(value)); } catch (NumberFormatException ex) { return null; }
    }
}
