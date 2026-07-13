package com.example.mcp.pojo.batch;

import java.util.Objects;

/**
 * 批量重命名请求参数，指定重命名模式、匹配模式和子目录选项。
 *
 * @param dirPath        目标目录的绝对路径
 * @param mode           重命名模式：prefix（添加前缀）/suffix（添加后缀）/index（按序号重命名）/replace（替换文本）
 * @param value          重命名参数值：prefix/suffix 为要添加的文本，index 为基础名称，replace 为"旧文本|新文本"
 * @param pattern        文件名匹配模式（通配符），如 *.txt
 * @param includeSubdirs 是否包含子目录中的文件，默认 false
 * @author Frank Kang
 * @since 2026-07-12
 */
public record BatchRenameRequest(String dirPath, String mode, String value, String pattern, Boolean includeSubdirs) {

    public BatchRenameRequest {
        Objects.requireNonNull(dirPath, "dirPath 不能为空");
    }
}
