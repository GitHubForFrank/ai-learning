package com.example.mcp.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * SSL 工具类，提供自定义 SSLContext 和 HttpClient 的构建能力。
 * <p>
 * 支持三种模式：
 * <ul>
 *   <li><b>默认模式</b>：使用 JDK 默认 truststore（cacerts）</li>
 *   <li><b>自定义信任证书</b>：通过 trustStore 路径指定自定义信任证书库</li>
 *   <li><b>双向认证（mTLS）</b>：通过 keyStore 路径指定客户端证书</li>
 *   <li><b>不安全模式</b>：跳过 SSL 证书验证（仅用于测试环境）</li>
 * </ul>
 * </p>
 *
 * @author Frank Kang
 * @since 2026-07-13
 */
public final class SslUtil {

    private SslUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 根据配置创建 SSLContext。
     *
     * @param insecure           是否跳过证书验证（不安全模式）
     * @param trustStorePath     信任证书库路径，null 表示使用默认
     * @param trustStorePassword 信任证书库密码
     * @param keyStorePath       客户端密钥库路径（mTLS），null 表示不使用
     * @param keyStorePassword   客户端密钥库密码
     * @return 配置好的 SSLContext
     * @throws Exception 证书加载失败时抛出
     */
    public static SSLContext createSslContext(boolean insecure, String trustStorePath, String trustStorePassword, String keyStorePath,
        String keyStorePassword) throws Exception {
        // 不安全模式：信任所有证书
        if (insecure) {
            return createInsecureSslContext();
        }

        // 构建 TrustManager
        TrustManager[] trustManagers = buildTrustManagers(trustStorePath, trustStorePassword);

        // 构建 KeyManager（mTLS 客户端证书）
        KeyManagerFactory kmf = null;
        if (keyStorePath != null && !keyStorePath.isBlank()) {
            kmf = buildKeyManagerFactory(keyStorePath, keyStorePassword);
        }

        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf != null ? kmf.getKeyManagers() : null, trustManagers, new SecureRandom());
        return sslContext;
    }

    /**
     * 创建跳过证书验证的 SSLContext（不安全模式，仅用于测试环境）。
     *
     * @return 信任所有证书的 SSLContext
     * @throws Exception 创建失败时抛出
     */
    private static SSLContext createInsecureSslContext() throws Exception {
        TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                // 信任所有客户端证书
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                // 信任所有服务端证书
            }
        }};
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(null, trustAll, new SecureRandom());
        return sslContext;
    }

    /**
     * 构建 TrustManager 数组。
     *
     * @param trustStorePath     信任证书库路径，null 表示使用默认
     * @param trustStorePassword 信任证书库密码
     * @return TrustManager 数组
     * @throws Exception 加载失败时抛出
     */
    private static TrustManager[] buildTrustManagers(String trustStorePath, String trustStorePassword) throws Exception {
        if (trustStorePath == null || trustStorePath.isBlank()) {
            // 使用 JDK 默认 truststore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            return tmf.getTrustManagers();
        }

        Path path = Path.of(trustStorePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("信任证书库文件不存在: " + trustStorePath);
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream is = new FileInputStream(path.toFile())) {
            trustStore.load(is, trustStorePassword != null ? trustStorePassword.toCharArray() : null);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf.getTrustManagers();
    }

    /**
     * 构建客户端密钥的 KeyManagerFactory（用于 mTLS 双向认证）。
     *
     * @param keyStorePath     客户端密钥库路径
     * @param keyStorePassword 客户端密钥库密码
     * @return KeyManagerFactory
     * @throws Exception 加载失败时抛出
     */
    private static KeyManagerFactory buildKeyManagerFactory(String keyStorePath, String keyStorePassword) throws Exception {
        Path path = Path.of(keyStorePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("客户端密钥库文件不存在: " + keyStorePath);
        }

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream is = new FileInputStream(path.toFile())) {
            keyStore.load(is, keyStorePassword != null ? keyStorePassword.toCharArray() : null);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyStorePassword != null ? keyStorePassword.toCharArray() : null);
        return kmf;
    }

    /**
     * 使用自定义 SSLContext 创建 JDK HttpClient。
     *
     * @param sslContext SSL 上下文，null 表示使用默认
     * @param timeout    连接超时时间
     * @return 配置好的 HttpClient
     */
    public static HttpClient createHttpClient(SSLContext sslContext, Duration timeout) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                                               .followRedirects(HttpClient.Redirect.NORMAL)
                                               .connectTimeout(timeout);

        if (sslContext != null) {
            builder.sslContext(sslContext);
        }

        return builder.build();
    }
}