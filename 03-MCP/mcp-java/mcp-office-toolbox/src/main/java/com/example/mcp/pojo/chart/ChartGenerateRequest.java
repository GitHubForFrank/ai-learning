package com.example.mcp.pojo.chart;

/**
 * 图表生成请求参数，指定数据、图表类型、输出路径和尺寸。
 *
 * @param dataJson   JSON 格式的图表数据（labels + values 或 categories + series）
 * @param outputPath 输出 PNG 文件路径
 * @param width      图片宽度（像素），默认 800
 * @param height     图片高度（像素），默认 500
 * @author Frank Kang
 * @since 2026-07-12
 */
public record ChartGenerateRequest(String dataJson, String outputPath, Integer width, Integer height) {

}
