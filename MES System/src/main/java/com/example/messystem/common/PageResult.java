package com.example.messystem.common;

import java.util.List;

public class PageResult<T> {
    public List<T> items;
    public int page;
    public int pageSize;
    public long total;

    public PageResult() {
    }

    public PageResult(List<T> items, int page, int pageSize, long total) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }
}
