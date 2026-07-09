package com.example.mcp.pojo.excel;

import java.util.List;

/**
 * 单元格样式信息（用于 format_range 的 styles 参数）
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
public record CellStyleInfo(
        List<BorderInfo> border,
        Integer decimalPlaces,
        FillInfo fill,
        FontInfo font,
        String numFmt) {
}