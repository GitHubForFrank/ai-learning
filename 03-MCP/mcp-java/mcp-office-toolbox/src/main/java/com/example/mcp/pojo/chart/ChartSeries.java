package com.example.mcp.pojo.chart;

import java.util.List;

/**
 * 图表数据系列，包含系列名称和数据值列表。
 * <p>
 * 使用 {@link List} 替代数组，确保 {@code equals()} / {@code hashCode()} 基于内容比较。
 * </p>
 *
 * @param name   系列名称
 * @param values 数据值列表
 * @author Frank Kang
 * @since 2026-07-12
 */
public record ChartSeries(String name, List<Double> values) {

}
