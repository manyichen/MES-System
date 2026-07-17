package com.example.messystem.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChineseMessageSupportTest {
    @Test
    void requiredFieldMessagesShouldPreferBusinessFieldLabels() {
        assertEquals("生产产线不能为空", ChineseMessageSupport.translate("lineId is required"));
        assertEquals("生产任务不能为空", ChineseMessageSupport.translate("taskId is required"));
    }
}
