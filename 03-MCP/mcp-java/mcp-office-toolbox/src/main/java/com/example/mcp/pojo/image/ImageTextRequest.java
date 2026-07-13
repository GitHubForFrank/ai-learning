package com.example.mcp.pojo.image;

import java.util.Objects;

/**
 * 图片添加文字请求参数，指定文字内容、位置、字体大小和颜色。
 *
 * @param fileAbsolutePath 图片文件的绝对路径
 * @param text             要添加的文字
 * @param x                文字起始 X 坐标（像素）
 * @param y                文字起始 Y 坐标（像素）
 * @param fontSize         字体大小，默认 24
 * @param color            文字颜色，如 red/black/white/blue/green，默认 black
 * @param targetPath       输出文件路径（可选）
 * @author Frank Kang
 * @since 2026-07-12
 */
public record ImageTextRequest(String fileAbsolutePath, String text, int x, int y, Integer fontSize, String color, String targetPath) {

    public ImageTextRequest {
        Objects.requireNonNull(fileAbsolutePath, "fileAbsolutePath 不能为空");
        Objects.requireNonNull(text, "text 不能为空");
        Objects.requireNonNull(targetPath, "targetPath 不能为空");
    }
}
