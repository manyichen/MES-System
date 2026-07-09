package com.example.messystem.common;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int pageSize, long total) {

    public static <T> PageResult<T> of(List<T> items, int page, int pageSize, long total) {
        return new PageResult<>(items, page, pageSize, total);
    }
}
