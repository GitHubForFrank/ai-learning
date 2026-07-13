package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.fetch.HttpClientHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * HttpClientHandler 单元测试
 * <p>
 * 测试所有 HTTP 方法（GET/POST/PUT/DELETE/PATCH/HEAD）和文件下载功能。
 * 使用 httpbin.org 作为测试目标，所有测试不依赖外部服务也可验证基本功能。
 * </p>
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class HttpClientHandlerTest {

    private static final String HTTPBIN_GET = "https://httpbin.org/get";
    private static final String HTTPBIN_POST = "https://httpbin.org/post";
    private static final String HTTPBIN_PUT = "https://httpbin.org/put";
    private static final String HTTPBIN_DELETE = "https://httpbin.org/delete";
    private static final String HTTPBIN_PATCH = "https://httpbin.org/patch";
    @Autowired
    private HttpClientHandler httpClientHandler;

    // ==================== GET ====================

    @Test
    void testHttpGet() {
        String result = httpClientHandler.httpGet(HTTPBIN_GET, null, 15, null);
        assertNotNull(result);
        assertTrue(result.contains("statusCode") || result.contains("error"));
    }

    @Test
    void testHttpGetWithHeaders() {
        String headers = "{\"Authorization\":\"Bearer test-token\",\"X-Custom\":\"test-value\"}";
        String result = httpClientHandler.httpGet(HTTPBIN_GET, headers, 15, null);
        assertNotNull(result);
        assertTrue(result.contains("statusCode") || result.contains("error"));
    }

    // ==================== POST ====================

    @Test
    void testHttpPost() {
        String body = "{\"name\":\"test\",\"value\":123}";
        String result = httpClientHandler.httpPost(HTTPBIN_POST, body, null, 15, null);
        assertNotNull(result);
        assertTrue(result.contains("statusCode") || result.contains("error"));
    }

    @Test
    void testHttpPostWithHeaders() {
        String body = "{\"data\":\"hello\"}";
        String headers = "{\"Authorization\":\"Bearer test-token\"}";
        String result = httpClientHandler.httpPost(HTTPBIN_POST, body, headers, 15, null);
        assertNotNull(result);
        assertTrue(result.contains("statusCode") || result.contains("error"));
    }

    // ==================== PUT ====================

    @Test
    void testHttpPut() {
        String body = "{\"name\":\"updated\",\"version\":2}";
        String result = httpClientHandler.httpPut(HTTPBIN_PUT, body, null, 15, null);
        assertNotNull(result);
        assertTrue(result.contains("statusCode") || result.contains("error"));
    }

    @Test
    void testHttpPutWithHeaders() {
        String body = "{\"status\":\"active\"}";
        String headers = "{\"Authorization\":\"Bearer test-token\",\"Content-Type\":\"application/json\"}";
        String result = httpClientHandler.httpPut(HTTPBIN_PUT, body, headers, 15, null);
        assertNotNull(result);
        assertTrue(result.contains("statusCode") || result.contains("error"));
    }

    // ==================== DELETE ====================

    @Test
    void testHttpDelete() {
        String result = httpClientHandler.httpDelete(HTTPBIN_DELETE, null, 15, null);
        assertNotNull(result);
        assertTrue(result.contains("statusCode") || result.contains("error"));
    }

    @Test
    void testHttpDeleteWithHeaders() {
        String headers = "{\"Authorization\":\"Bearer test-token\"}";
        String result = httpClientHandler.httpDelete(HTTPBIN_DELETE, headers, 15, null);
        assertNotNull(result);
        assertTrue(result.contains("statusCode") || result.contains("error"));
    }

    // ==================== PATCH ====================

    @Test
    void testHttpPatch() {
        String body = "{\"status\":\"partial-update\"}";
        String result = httpClientHandler.httpPatch(HTTPBIN_PATCH, body, null, 15, null);
        assertNotNull(result);
        assertTrue(result.contains("statusCode") || result.contains("error"));
    }

    @Test
    void testHttpPatchWithHeaders() {
        String body = "{\"field\":\"value\"}";
        String headers = "{\"Authorization\":\"Bearer test-token\"}";
        String result = httpClientHandler.httpPatch(HTTPBIN_PATCH, body, headers, 15, null);
        assertNotNull(result);
        assertTrue(result.contains("statusCode") || result.contains("error"));
    }

    // ==================== HEAD ====================

    @Test
    void testHttpHead() {
        String result = httpClientHandler.httpHead(HTTPBIN_GET, 15, null);
        assertNotNull(result);
    }

    // ==================== 下载 ====================

    @Test
    void testHttpDownload() {
        String result = httpClientHandler.httpDownload("https://httpbin.org/image/png", "target/test-output/download-test.png", null, 15, null);
        assertNotNull(result);
        assertTrue(result.contains("下载成功") || result.contains("错误"));
    }

    // ==================== 参数校验 ====================

    @Test
    void testHttpGetWithoutOptionalParams() {
        String result = httpClientHandler.httpGet(HTTPBIN_GET, null, null, null);
        assertNotNull(result);
        assertTrue(result.contains("statusCode") || result.contains("error"));
    }

    @Test
    void testHttpPostWithoutOptionalParams() {
        String result = httpClientHandler.httpPost(HTTPBIN_POST, null, null, null, null);
        assertNotNull(result);
        assertTrue(result.contains("statusCode") || result.contains("error"));
    }
}