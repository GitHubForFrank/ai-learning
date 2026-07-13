package com.example.mcp.pojo.fetch;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

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
        Objects.requireNonNull(url, "url 不能为空");
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("url 必须以 http:// 或 https:// 开头");
        }
        if (headers == null) {
            headers = Collections.emptyMap();
        }
    }
}
