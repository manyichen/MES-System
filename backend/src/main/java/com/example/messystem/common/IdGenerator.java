/*
 * 答辩定位：公共基础设施 模块的 IdGenerator。
 * 分层职责：公共支撑代码：提供多个业务模块共享的响应、异常、编码或工具能力。
 * 典型调用链：由应用启动、HTTP 过滤器或各业务模块按需调用。
 * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。
 */
package com.example.messystem.common;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 公共基础设施 的 IdGenerator，承担当前文件头所述职责，并保持与相邻层的单向依赖。
 */
public final class IdGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmssSSS");
    private static final ConcurrentHashMap<String, AtomicLong> SEQUENCES = new ConcurrentHashMap<>();

    /**
     * 内部实现步骤：执行 IdGenerator 对应的业务步骤。
     * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。
     */
    private IdGenerator() {
    }

    /**
     * 公共能力：执行 nextCode 对应的业务步骤。
     * 由 IdGenerator 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。
     */
    public static String nextCode(String prefix) {
        long next = SEQUENCES.computeIfAbsent(prefix, key -> new AtomicLong()).updateAndGet(value -> value >= 9999 ? 1 : value + 1);
        return prefix + "-" + DATE_FORMAT.format(LocalDate.now()) + "-" + TIME_FORMAT.format(LocalTime.now()) + "-" + String.format("%04d", next);
    }
}
