package com.example.mcp.pojo.ppt;

import java.util.Objects;

/**
 * PPT 添加图表请求参数，指定图表类型、标题、位置和大小。
 *
 * @param fileAbsolutePath PPT 文件的绝对路径
 * @param slideIndex       幻灯片索引（从0开始）
 * @param chartType        图表类型：bar/line/pie/area/scatter
 * @param title            图表标题
 * @param x                X坐标，默认 50
 * @param y                Y坐标，默认 100
 * @param width            图表宽度，默认 600
 * @param height           图表高度，默认 350
 * @author Frank Kang
 * @since 2026-07-12
 */
public record AddChartRequest(String fileAbsolutePath, int slideIndex, String chartType, String title, Integer x, Integer y, Integer width,
                              Integer height) {

    public AddChartRequest {
        Objects.requireNonNull(fileAbsolutePath, "fileAbsolutePath 不能为空");
        Objects.requireNonNull(chartType, "chartType 不能为空");
    }
}
