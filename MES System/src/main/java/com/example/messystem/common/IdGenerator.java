package com.example.messystem.common;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmssSSS");
    private static final ConcurrentHashMap<String, AtomicLong> SEQUENCES = new ConcurrentHashMap<>();

    private IdGenerator() {
    }

    public static String nextCode(String prefix) {
        long next = SEQUENCES.computeIfAbsent(prefix, key -> new AtomicLong()).updateAndGet(value -> value >= 9999 ? 1 : value + 1);
        return prefix + "-" + DATE_FORMAT.format(LocalDate.now()) + "-" + TIME_FORMAT.format(LocalTime.now()) + "-" + String.format("%04d", next);
    }
}
