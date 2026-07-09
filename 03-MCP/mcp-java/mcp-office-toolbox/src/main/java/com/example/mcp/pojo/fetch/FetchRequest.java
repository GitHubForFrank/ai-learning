package com.example.mcp.pojo.fetch;

import java.util.Collections;
import java.util.Map;

/**
 * Fetch MCP 工具请求参数
 *
 * @param url     要抓取的 URL，必填
 * @param headers 可选的自定义请求头，用于添加认证等自定义头部
 * @author Frank Kang
 * @since 2026-07-09
 */
public record FetchRequest(String url, Map<String, String> headers) {

    public FetchRequest {
        if (headers == null) {
            headers = Collections.emptyMap();
        }
    }
}
