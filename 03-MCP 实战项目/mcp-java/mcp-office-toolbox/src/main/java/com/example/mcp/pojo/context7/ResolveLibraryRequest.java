package com.example.mcp.pojo.context7;

/**
 * Context7 resolve-library-id 请求参数
 *
 * @param query      用户的原始问题或任务描述，用于相关性排序
 * @param libraryName 要搜索的库名称
 * @author Frank Kang
 * @since 2026-07-10
 */
public record ResolveLibraryRequest(String query, String libraryName) {
}
