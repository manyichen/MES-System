package com.example.messystem.planning.ai;

import com.example.messystem.common.DbConfig;

public final class AiPlanningConfig {
    private AiPlanningConfig() {
    }

    public static boolean enabled() {
        return Boolean.parseBoolean(DbConfig.getValue("AI_PLANNING_ENABLED", "false"));
    }

    public static String apiKey() {
        return DbConfig.getValue("DASHSCOPE_API_KEY", "");
    }

    public static String baseUrl() {
        String value = DbConfig.getValue("DASHSCOPE_BASE_URL",
                "https://dashscope.aliyuncs.com/compatible-mode/v1");
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public static String model() {
        return DbConfig.getValue("DASHSCOPE_MODEL", "qwen-turbo");
    }
}
