package com.example.mcp.util;

import java.util.ArrayList;
import java.util.List;

/**
 * CSV 解析工具类，提供 CSV 行解析等通用方法。
 *
 * @author Frank Kang
 * @since 2026-07-13
 */
public final class CsvUtil {

    private CsvUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 解析 CSV 行，按逗号分隔，支持双引号转义。
     *
     * @param line CSV 行字符串
     * @return 解析后的字段数组
     */
    public static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }
}