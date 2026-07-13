package com.example.mcp.pojo.barcode;

import java.util.Objects;

/**
 * 条形码生成请求参数，指定编码内容、类型、尺寸和输出路径。
 *
 * @param content    条形码编码内容
 * @param targetPath 输出图片文件的绝对路径（.png）
 * @param type       条形码类型：EAN_13/CODE_128/CODE39，默认 CODE_128
 * @param width      图片宽度（像素），默认 400
 * @param height     图片高度（像素），默认 150
 * @param showText   是否在条形码下方显示文本，默认 true
 * @author Frank Kang
 * @since 2026-07-12
 */
public record BarcodeGenerateRequest(String content, String targetPath, String type, Integer width, Integer height, Boolean showText) {

    public BarcodeGenerateRequest {
        Objects.requireNonNull(content, "content 不能为空");
        Objects.requireNonNull(targetPath, "targetPath 不能为空");
        if (width != null && width <= 0) {
            throw new IllegalArgumentException("width 必须大于 0");
        }
        if (height != null && height <= 0) {
            throw new IllegalArgumentException("height 必须大于 0");
        }
    }
}
