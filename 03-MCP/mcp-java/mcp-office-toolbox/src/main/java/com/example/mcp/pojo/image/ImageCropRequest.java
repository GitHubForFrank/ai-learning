package com.example.mcp.pojo.image;

import java.util.Objects;

/**
 * 图片裁剪请求参数，指定裁剪区域和目标输出路径。
 *
 * @param fileAbsolutePath 图片文件的绝对路径
 * @param x                裁剪区域起始 X 坐标（像素）
 * @param y                裁剪区域起始 Y 坐标（像素）
 * @param width            裁剪区域宽度（像素）
 * @param height           裁剪区域高度（像素）
 * @param targetPath       输出文件路径（可选）
 * @author Frank Kang
 * @since 2026-07-12
 */
public record ImageCropRequest(String fileAbsolutePath, int x, int y, int width, int height, String targetPath) {

    public ImageCropRequest {
        Objects.requireNonNull(fileAbsolutePath, "fileAbsolutePath 不能为空");
        Objects.requireNonNull(targetPath, "targetPath 不能为空");
        if (width <= 0) {
            throw new IllegalArgumentException("width 必须大于 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height 必须大于 0");
        }
    }
}
