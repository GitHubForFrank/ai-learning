package com.example.mcp.pojo.ppt;

import java.util.Objects;

/**
 * PPT 添加图形请求参数，指定图形类型、位置、大小和样式。
 *
 * @param fileAbsolutePath PPT 文件的绝对路径
 * @param slideIndex       幻灯片索引（从0开始）
 * @param shapeType        图形类型：rectangle/circle/arrow_right/star/triangle/diamond/rounded_rect 等
 * @param x                X坐标，默认 100
 * @param y                Y坐标，默认 100
 * @param width            图形宽度，默认 200
 * @param height           图形高度，默认 200
 * @param fillColor        填充颜色，十六进制格式，默认 #4472C4
 * @param lineColor        边框颜色，十六进制格式，默认 #333333
 * @param lineWidth        边框宽度，默认 1.0
 * @author Frank Kang
 * @since 2026-07-12
 */
public record AddShapeRequest(String fileAbsolutePath, int slideIndex, String shapeType, Integer x, Integer y, Integer width, Integer height,
                              String fillColor, String lineColor, Double lineWidth) {

    public AddShapeRequest {
        Objects.requireNonNull(fileAbsolutePath, "fileAbsolutePath 不能为空");
        Objects.requireNonNull(shapeType, "shapeType 不能为空");
    }
}
