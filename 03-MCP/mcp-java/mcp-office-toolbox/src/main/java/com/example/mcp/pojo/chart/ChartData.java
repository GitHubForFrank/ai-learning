package com.example.mcp.pojo.chart;

import java.util.ArrayList;
import java.util.List;

/**
 * 图表数据结构，包含标题、标签和数据系列。
 * <p>
 * 使用 {@link List} 替代数组，确保 {@code equals()} / {@code hashCode()} 基于内容比较，
 * 避免数组引用比较带来的语义问题。
 * </p>
 *
 * @param title  图表标题
 * @param labels X轴标签列表
 * @param series 数据系列列表
 * @author Frank Kang
 * @since 2026-07-12
 */
public record ChartData(String title, List<String> labels, List<ChartSeries> series) {

    /**
     * 创建空的图表数据对象
     */
    public ChartData() {
        this("图表", new ArrayList<>(), new ArrayList<>());
    }
}
