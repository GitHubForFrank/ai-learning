package com.example.mcp.pojo.fetch;

/**
 * Fetch MCP 工具请求参数
 *
 * @param url        要抓取的 URL，必填
 * @param maxLength  返回的最大字符数，默认 5000，范围 (0, 1000000)
 * @param startIndex 返回内容的起始字符索引，默认 0
 * @param raw        是否返回原始 HTML 而不转换为 Markdown，默认 false
 * @author FrankKang
 * @since 2026-07-09
 */
public record FetchRequest(String url, int maxLength, int startIndex, boolean raw) {

    public FetchRequest {
        if (maxLength <= 0 || maxLength >= 1000000) {
            maxLength = 5000;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }
    }
}
