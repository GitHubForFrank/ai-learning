package com.zmz.sdd.fullstack.core.common;

import lombok.Data;

import java.util.List;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 02-功能规范.md §3.2] 分页响应
 */
@Data
public class PageVO<T> {
    private int page;
    private int size;
    private long total;
    private int totalPages;
    private List<T> list;

    public static <T> PageVO<T> of(int page, int size, long total, List<T> list) {
        PageVO<T> p = new PageVO<>();
        p.page = page;
        p.size = size;
        p.total = total;
        p.totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        p.list = list;
        return p;
    }
}
