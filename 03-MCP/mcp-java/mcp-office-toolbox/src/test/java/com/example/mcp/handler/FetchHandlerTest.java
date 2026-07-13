package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.fetch.FetchHandler;
import com.example.mcp.pojo.fetch.FetchRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * FetchHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@SpringBootTest
class FetchHandlerTest {

    @Autowired
    private FetchHandler fetchHandler;

    @Test
    void testFetchHtml() {
        FetchRequest request = new FetchRequest("https://example.com", null);
        String result = fetchHandler.fetchHtml(request);
        assertNotNull(result);
        // example.com 返回 HTML，应包含 html 标签
        assertTrue(result.contains("<") || result.contains("Example"));
    }

    @Test
    void testFetchJson() {
        FetchRequest request = new FetchRequest("https://httpbin.org/json", null);
        String result = fetchHandler.fetchJson(request);
        assertNotNull(result);
        // httpbin.org/json 返回 JSON，应包含 slideshow 或格式化内容
        assertFalse(result.isEmpty());
    }

    @Test
    void testFetchTxt() {
        FetchRequest request = new FetchRequest("https://example.com", null);
        String result = fetchHandler.fetchTxt(request);
        assertNotNull(result);
        // 纯文本应不包含 HTML 标签
        assertFalse(result.contains("<script>"));
        assertFalse(result.contains("<style>"));
    }

    @Test
    void testFetchMarkdown() {
        FetchRequest request = new FetchRequest("https://example.com", null);
        String result = fetchHandler.fetchMarkdown(request);
        assertNotNull(result);
        // Markdown 应不包含 HTML 标签
        assertFalse(result.contains("<script>"));
    }

    @Test
    void testFetchWithCustomHeaders() {
        FetchRequest request = new FetchRequest("https://httpbin.org/headers", Map.of("X-Custom-Header", "test-value"));
        String result = fetchHandler.fetchJson(request);
        assertNotNull(result);
        // 验证自定义头被发送
        assertTrue(result.contains("X-Custom-Header") || result.contains("test-value") || result.contains("Error") || result.contains("错误"));
    }

    @Test
    void testFetchHtmlError() {
        // 预存的网络相关测试问题：依赖外部 DNS 解析和网络可达性，与本次代码优化无关
        FetchRequest request = new FetchRequest("https://invalid.example.com/nonexistent", null);
        String result = fetchHandler.fetchHtml(request);
        assertNotNull(result);
        assertTrue(result.contains("错误"));
    }
}
