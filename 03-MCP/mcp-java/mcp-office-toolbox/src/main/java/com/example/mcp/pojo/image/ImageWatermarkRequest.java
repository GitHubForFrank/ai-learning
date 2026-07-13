package com.example.mcp.pojo.image;

import java.util.Objects;

/**
 * 图片水印请求参数，指定水印文字、位置、透明度和字体大小。
 *
 * @param fileAbsolutePath 图片文件的绝对路径
 * @param text             水印文字
 * @param position         水印位置：topLeft/topCenter/topRight/centerLeft/center/centerRight/bottomLeft/bottomCenter/bottomRight
 * @param opacity          透明度（0.0-1.0），默认 0.5
 * @param fontSize         字体大小，默认 36
 * @param targetPath       输出文件路径（可选）
 * @author Frank Kang
 * @since 2026-07-12
 */
public record ImageWatermarkRequest(String fileAbsolutePath, String text, String position, Float opacity, Integer fontSize, String targetPath) {

    public ImageWatermarkRequest {
        Objects.requireNonNull(fileAbsolutePath, "fileAbsolutePath 不能为空");
        Objects.requireNonNull(text, "text 不能为空");
        Objects.requireNonNull(targetPath, "targetPath 不能为空");
    }
}
