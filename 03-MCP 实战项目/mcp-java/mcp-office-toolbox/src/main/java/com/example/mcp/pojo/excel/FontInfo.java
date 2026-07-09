package com.example.mcp.pojo.excel;

/**
 * 字体样式信息
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
public record FontInfo(
        Boolean bold,
        String color,
        Boolean italic,
        Double size,
        Boolean strike,
        String underline,
        String vertAlign) {
}