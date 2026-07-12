package com.example.messystem.common;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ChineseMessageSupport {
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

    private ChineseMessageSupport() {
    }

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
        for (Map.Entry<String, String> entry : CONTAINS_MESSAGES.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "操作未完成，请检查填写内容或当前业务状态";
    }

    private static boolean containsChinese(String value) {
        return value.codePoints().anyMatch(codePoint -> codePoint >= 0x4E00 && codePoint <= 0x9FFF);
    }
}
