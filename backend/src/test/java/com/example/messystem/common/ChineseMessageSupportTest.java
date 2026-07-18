/*
 * 答辩定位：公共基础设施 模块的 ChineseMessageSupportTest。
 * 分层职责：自动化回归测试：固定关键业务规则、接口契约和架构边界，防止重构时出现静默回归。
 * 典型调用链：Maven Surefire -> JUnit 5 -> 被测类；测试替身用于隔离远程数据库或文件系统。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 公共基础设施 的 ChineseMessageSupportTest，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
class ChineseMessageSupportTest {
    /**
     * 回归场景：验证 requiredFieldMessagesShouldPreferBusinessFieldLabels 所描述的行为。
     * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。
     */
    @Test
    void requiredFieldMessagesShouldPreferBusinessFieldLabels() {
        assertEquals("生产产线不能为空", ChineseMessageSupport.translate("lineId is required"));
        assertEquals("生产任务不能为空", ChineseMessageSupport.translate("taskId is required"));
    }
}
