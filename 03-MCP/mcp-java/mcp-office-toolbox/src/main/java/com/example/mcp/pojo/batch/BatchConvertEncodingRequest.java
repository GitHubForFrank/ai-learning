package com.example.mcp.pojo.batch;

import java.util.Objects;

/**
 * 批量编码转换请求参数，指定源编码、目标编码、匹配模式和子目录选项。
 *
 * @param dirPath        目标目录的绝对路径
 * @param sourceEncoding 源编码名称，如 GBK、ISO-8859-1
 * @param targetEncoding 目标编码名称，如 UTF-8
 * @param pattern        文件名匹配模式（通配符），如 *.txt
 * @param includeSubdirs 是否包含子目录中的文件，默认 false
 * @author Frank Kang
 * @since 2026-07-12
 */
public record BatchConvertEncodingRequest(String dirPath, String sourceEncoding, String targetEncoding, String pattern, Boolean includeSubdirs) {

    public BatchConvertEncodingRequest {
        Objects.requireNonNull(dirPath, "dirPath 不能为空");
        Objects.requireNonNull(targetEncoding, "targetEncoding 不能为空");
    }
}
