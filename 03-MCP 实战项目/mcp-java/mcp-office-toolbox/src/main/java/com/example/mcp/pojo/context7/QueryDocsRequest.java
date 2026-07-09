package com.example.mcp.pojo.context7;

/**
 * Context7 query-docs 请求参数
 *
 * @param libraryId Context7 标准库 ID（如 /vercel/next.js），必填
 * @param query     自然语言查询主题，用于过滤文档内容
 * @param tokens    返回文档的最大 token 数，默认 5000，最小 1000
 * @author Frank Kang
 * @since 2026-07-10
 */
public record QueryDocsRequest(String libraryId, String query, int tokens) {

    public QueryDocsRequest {
        if (tokens <= 0 || tokens < 1000) {
            tokens = 5000;
        }
    }
}
