package com.example.mcp.pojo.excel;

import java.util.List;

/**
 * 填充样式信息
 *
 * @author FrankKang
 * @since 2026-07-09
 */
public record FillInfo(
        String type,
        String pattern,
        List<String> color,
        String shading) {
}