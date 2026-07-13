package com.example.mcp.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpClient;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;

/**
 * SslUtil 单元测试
 * <p>
 * 测试 SSLContext 创建的各种模式：默认模式、不安全模式、自定义 truststore 模式。
 * </p>
 *
 * @author Frank Kang
 * @since 2026-07-13
 */
class SslUtilTest {

    // ==================== 默认模式 ====================

    @Test
    void testDefaultSslContext() throws Exception {
        SSLContext sslContext = SslUtil.createSslContext(false, null, null, null, null);
        assertNotNull(sslContext);
        assertNotNull(sslContext.getSocketFactory());
    }

    @Test
    void testDefaultHttpClient() {
        HttpClient client = SslUtil.createHttpClient(null, Duration.ofSeconds(10));
        assertNotNull(client);
        assertTrue(client.followRedirects() == HttpClient.Redirect.NORMAL);
    }

    // ==================== 不安全模式 ====================

    @Test
    void testInsecureSslContext() throws Exception {
        SSLContext sslContext = SslUtil.createSslContext(true, null, null, null, null);
        assertNotNull(sslContext);
        assertNotNull(sslContext.getSocketFactory());
    }

    @Test
    void testInsecureHttpClient() throws Exception {
        SSLContext sslContext = SslUtil.createSslContext(true, null, null, null, null);
        HttpClient client = SslUtil.createHttpClient(sslContext, Duration.ofSeconds(10));
        assertNotNull(client);
    }

    // ==================== 自定义 SSLContext ====================

    @Test
    void testCustomSslContextHttpClient() throws Exception {
        SSLContext sslContext = SslUtil.createSslContext(false, null, null, null, null);
        HttpClient client = SslUtil.createHttpClient(sslContext, Duration.ofSeconds(30));
        assertNotNull(client);
    }

    // ==================== 异常情况 ====================

    @Test
    void testNonExistentTrustStore() {
        assertThrows(IllegalArgumentException.class, () -> {
            SslUtil.createSslContext(false, "/non/existent/truststore.jks", "password", null, null);
        });
    }

    @Test
    void testNonExistentKeyStore() {
        assertThrows(IllegalArgumentException.class, () -> {
            SslUtil.createSslContext(false, null, null, "/non/existent/keystore.jks", "password");
        });
    }

    // ==================== 超时配置 ====================

    @Test
    void testTimeoutConfiguration() {
        HttpClient client = SslUtil.createHttpClient(null, Duration.ofSeconds(60));
        assertNotNull(client);
    }
}