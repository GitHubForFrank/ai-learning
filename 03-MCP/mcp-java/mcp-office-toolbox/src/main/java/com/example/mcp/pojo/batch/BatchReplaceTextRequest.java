package com.example.mcp.pojo.batch;

import java.util.Objects;

/**
 * 批量文本替换请求参数，指定查找替换文本、匹配模式和子目录选项。
 *
 * @param dirPath        目标目录的绝对路径
 * @param searchText     要查找的文本（支持正则表达式）
 * @param replaceText    替换后的文本
 * @param pattern        文件名匹配模式（通配符），如 *.txt
 * @param useRegex       是否使用正则表达式匹配，默认 false
 * @param includeSubdirs 是否包含子目录中的文件，默认 false
 * @author Frank Kang
 * @since 2026-07-12
 */
public record BatchReplaceTextRequest(String dirPath, String searchText, String replaceText, String pattern, Boolean useRegex,
                                      Boolean includeSubdirs) {

    public BatchReplaceTextRequest {
        Objects.requireNonNull(dirPath, "dirPath 不能为空");
        Objects.requireNonNull(searchText, "searchText 不能为空");
        Objects.requireNonNull(replaceText, "replaceText 不能为空");
    }
}
