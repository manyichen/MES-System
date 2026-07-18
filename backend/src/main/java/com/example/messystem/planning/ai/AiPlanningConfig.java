/*
 * 答辩定位：订单、计划、齐套与工单 模块的 AiPlanningConfig。
 * 分层职责：运行基础设施：负责应用注册、服务器启动、配置读取或数据库连接，是业务模块共享的外部依赖边界。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.planning.ai;

import com.example.messystem.common.DbConfig;

/**
 * 订单、计划、齐套与工单 的 AiPlanningConfig，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public final class AiPlanningConfig {
    /**
     * 内部实现步骤：执行 AiPlanningConfig 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private AiPlanningConfig() {
    }

    /**
     * 公共能力：启用业务对象。
     * 由 AiPlanningConfig 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static boolean enabled() {
        return Boolean.parseBoolean(DbConfig.getValue("AI_PLANNING_ENABLED", "false"));
    }

    /**
     * 公共能力：执行 apiKey 对应的业务步骤。
     * 由 AiPlanningConfig 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static String apiKey() {
        return DbConfig.getValue("DASHSCOPE_API_KEY", "");
    }

    /**
     * 公共能力：执行 baseUrl 对应的业务步骤。
     * 由 AiPlanningConfig 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static String baseUrl() {
        String value = DbConfig.getValue("DASHSCOPE_BASE_URL",
                "https://dashscope.aliyuncs.com/compatible-mode/v1");
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * 公共能力：执行 model 对应的业务步骤。
     * 由 AiPlanningConfig 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static String model() {
        return DbConfig.getValue("DASHSCOPE_MODEL", "qwen-turbo");
    }
}
