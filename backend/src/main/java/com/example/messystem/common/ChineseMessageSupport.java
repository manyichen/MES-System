/*
 * 答辩定位：公共基础设施 模块的 ChineseMessageSupport。
 * 分层职责：公共支撑代码：提供多个业务模块共享的响应、异常、编码或工具能力。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 公共基础设施 的 ChineseMessageSupport，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public final class ChineseMessageSupport {
    private static final Map<String, String> REQUIRED_FIELDS = Map.ofEntries(
            Map.entry("taskId", "生产任务"),
            Map.entry("lineId", "生产产线"),
            Map.entry("processId", "工序"),
            Map.entry("actorId", "操作人"),
            Map.entry("operatorId", "操作工"),
            Map.entry("productId", "产品"),
            Map.entry("workOrderId", "生产工单"),
            Map.entry("warehouseId", "仓库"),
            Map.entry("locationId", "库位"),
            Map.entry("materialId", "物料"),
            Map.entry("inspectionId", "质检单"),
            Map.entry("reportId", "报工单")
    );

    private static final Map<String, String> EXACT_MESSAGES = Map.ofEntries(
            Map.entry("work order not found", "生产工单不存在"),
            Map.entry("production task not found", "生产任务不存在"),
            Map.entry("order not found", "客户订单不存在"),
            Map.entry("product not found", "产品不存在"),
            Map.entry("material not found", "物料不存在"),
            Map.entry("warehouse not found", "仓库不存在"),
            Map.entry("warehouse location not found", "库位不存在"),
            Map.entry("inventory not found", "库存记录不存在"),
            Map.entry("requisition not found", "领料单不存在"),
            Map.entry("picking task not found", "拣货任务不存在"),
            Map.entry("delivery task not found", "配送任务不存在"),
            Map.entry("robot not found", "机器人不存在"),
            Map.entry("work report not found", "报工单不存在"),
            Map.entry("piecework wage not found", "计件工资记录不存在"),
            Map.entry("user not found", "用户不存在"),
            Map.entry("password is required", "密码不能为空"),
            Map.entry("product trace body is required", "产品追溯信息不能为空"),
            Map.entry("feedback body is required", "反馈内容不能为空"),
            Map.entry("metric body is required", "指标信息不能为空"),
            Map.entry("metric key and name are required", "指标编码和名称不能为空"),
            Map.entry("item body is required", "质检项目不能为空"),
            Map.entry("judgement status and result are required", "审核状态和判定结果不能为空"),
            Map.entry("rework order body is required", "返工单信息不能为空"),
            Map.entry("maintenance plan body is required", "维护计划信息不能为空")
    );

    private static final Map<String, String> CONTAINS_MESSAGES = new LinkedHashMap<>();

    static {
        CONTAINS_MESSAGES.put("inventory is not enough", "库存不足，请检查物料可用数量");
        CONTAINS_MESSAGES.put("only created requisitions can be approved", "只有待审核领料单可以审核");
        CONTAINS_MESSAGES.put("only created picking tasks can be completed", "只有待拣货任务可以完成拣货");
        CONTAINS_MESSAGES.put("only pending delivery tasks can arrive", "只有待配送任务可以标记到达");
        CONTAINS_MESSAGES.put("only arrived delivery tasks can be confirmed", "只有已到达的配送任务可以确认收料");
        CONTAINS_MESSAGES.put("only approved passed quality inspections can receive finished goods", "只有已审核通过且判定合格的质检单才能成品入库");
        CONTAINS_MESSAGES.put("only submitted reports can be approved", "只有已提交的报工单可以审核");
        CONTAINS_MESSAGES.put("reported quantity exceeds", "报工数量不能超过生产工单计划数量的 110%");
        CONTAINS_MESSAGES.put("work order status does not allow work report", "当前工单状态不允许报工");
        CONTAINS_MESSAGES.put("work order status does not allow requisition", "当前工单状态不允许创建领料单");
        CONTAINS_MESSAGES.put("materials have already been received", "该配送任务已确认收料，请勿重复操作");
        CONTAINS_MESSAGES.put("database operation failed", "数据库操作失败，请检查数据是否完整");
        CONTAINS_MESSAGES.put("is required", "必填信息不能为空");
        CONTAINS_MESSAGES.put("must be greater than", "填写的数量必须大于 0");
        CONTAINS_MESSAGES.put("cannot be negative", "填写的数量不能为负数");
        CONTAINS_MESSAGES.put("must be between", "填写的数值超出允许范围");
    }

    /**
     * 内部实现步骤：执行 ChineseMessageSupport 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private ChineseMessageSupport() {
    }

    /**
     * 公共能力：执行 translate 对应的业务步骤。
     * 由 ChineseMessageSupport 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static String translate(String message) {
        if (message == null || message.isBlank()) {
            return "操作失败，请稍后重试";
        }
        if (containsChinese(message)) {
            return message;
        }
        String normalized = message.trim().toLowerCase(Locale.ROOT);
        String exact = EXACT_MESSAGES.get(normalized);
        if (exact != null) {
            return exact;
        }
        String requiredField = translateRequiredField(message);
        if (requiredField != null) {
            return requiredField;
        }
        for (Map.Entry<String, String> entry : CONTAINS_MESSAGES.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "操作未完成，请检查填写内容或当前业务状态";
    }

    /**
     * 内部实现步骤：执行 translateRequiredField 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static String translateRequiredField(String message) {
        String trimmed = message == null ? "" : message.trim();
        int suffix = trimmed.lastIndexOf(" is required");
        if (suffix <= 0 || suffix != trimmed.length() - " is required".length()) {
            return null;
        }
        String field = trimmed.substring(0, suffix).trim();
        if (field.contains(" ")) {
            return null;
        }
        String label = REQUIRED_FIELDS.get(field);
        return label == null ? null : label + "不能为空";
    }

    /**
     * 内部实现步骤：执行 containsChinese 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private static boolean containsChinese(String value) {
        return value.codePoints().anyMatch(codePoint -> codePoint >= 0x4E00 && codePoint <= 0x9FFF);
    }
}
