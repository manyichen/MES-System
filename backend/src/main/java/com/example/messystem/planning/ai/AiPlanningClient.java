package com.example.messystem.planning.ai;

import com.example.messystem.common.BadRequestException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class AiPlanningClient {
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public Map<String, Object> requestAdvice(Map<String, Object> snapshot) {
        if (!AiPlanningConfig.enabled()) {
            throw new BadRequestException("AI 排产建议未开启，请在 .env 设置 AI_PLANNING_ENABLED=true");
        }
        String apiKey = AiPlanningConfig.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("未配置百炼 API Key，请在 .env 设置 DASHSCOPE_API_KEY");
        }

        try {
            String snapshotJson = MAPPER.writeValueAsString(snapshot);
            HttpResponse<String> response = send(apiKey, snapshotJson, true);
            if ((response.statusCode() == 400 || response.statusCode() == 422)
                    && response.body() != null
                    && response.body().toLowerCase().contains("response_format")) {
                response = send(apiKey, snapshotJson, false);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BadRequestException("百炼 API 调用失败，状态码：" + response.statusCode());
            }
            JsonNode root = MAPPER.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new BadRequestException("百炼 API 未返回建议内容");
            }
            return parseAdvice(content);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("AI 排产建议生成失败：" + ex.getMessage());
        }
    }

    private HttpResponse<String> send(String apiKey, String snapshotJson, boolean jsonMode) throws Exception {
        Map<String, Object> body = jsonMode
                ? Map.of(
                        "model", AiPlanningConfig.model(),
                        "temperature", 0.2,
                        "response_format", Map.of("type", "json_object"),
                        "messages", messages(snapshotJson)
                )
                : Map.of(
                        "model", AiPlanningConfig.model(),
                        "temperature", 0.2,
                        "messages", messages(snapshotJson)
                );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AiPlanningConfig.baseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static List<Map<String, String>> messages(String snapshotJson) {
        return List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", "排产业务数据如下，请仅输出 JSON：\n" + snapshotJson)
        );
    }

    private static Map<String, Object> parseAdvice(String content) throws Exception {
        String json = stripMarkdownFence(content.trim());
        JsonNode node = MAPPER.readTree(json);
        if (!node.isObject()) {
            throw new BadRequestException("AI 返回内容不是 JSON 对象");
        }
        return MAPPER.convertValue(node, new TypeReference<>() {
        });
    }

    private static String stripMarkdownFence(String content) {
        if (!content.startsWith("```")) return content;
        int firstLine = content.indexOf('\n');
        int lastFence = content.lastIndexOf("```");
        if (firstLine >= 0 && lastFence > firstLine) {
            return content.substring(firstLine + 1, lastFence).trim();
        }
        return content;
    }

    private static String systemPrompt() {
        return """
                你是轮胎工厂 MES 的 PMC 排产辅助顾问，只能给 PMC 计划员提供建议，不能要求系统自动执行。
                本次只分析一个已齐套、等待制定生产工单的生产任务。candidateTasks 中明确了该任务对应的生产订单、客户、订单交付日期和任务完成期限；productionLines 中给出了每条产线的实时状态、日产能、在制工单数和待完成数量；selectedProcessRoute 是完整工艺路线。
                必须做出可执行的具体判断：必须从 schedulable=true 的 productionLines 中选择一条推荐产线，使用其中真实的 lineId、lineCode、lineName；根据计划数量、日产能和在制负荷给出建议开始/结束时间，并明确是否能在任务期限和订单交付日期前完成。
                严禁使用“视情况”“酌情安排”“建议评估”等空泛表述；不得输出不存在的产线或订单；建议只供人工确认，不能自动修改任何数据。
                输出必须是一个合法 JSON 对象，不要 Markdown，不要多余说明。字段固定为：
                {
                  "orderAssignment": "任务编号 / 生产订单编号 / 客户名称 / 订单交付日期",
                  "recommendedLineId": 1,
                  "recommendedLine": "真实产线编码 / 真实产线名称",
                  "recommendedStart": "yyyy-MM-dd HH:mm",
                  "recommendedEnd": "yyyy-MM-dd HH:mm",
                  "deadlineAssessment": "明确说明可否在任务期限和订单交付日期前完成，以及依据",
                  "overallAdvice": "结合齐套、产线状态和负荷给出的明确安排结论",
                  "schedulingMethod": "完整工艺路线的执行顺序，以及先后衔接方式"
                }
                """;
    }
}
