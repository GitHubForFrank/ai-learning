package com.example.mcp.handler.fetch;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.mcp.handler.BaseHandler;
import com.example.mcp.util.LogUtil;
import com.example.mcp.util.PathUtil;
import com.example.mcp.util.SslUtil;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * HTTP 客户端处理器，提供完整的 HTTP 请求 MCP 工具。
 * <p>
 * 支持 GET / POST / PUT / DELETE / PATCH / HEAD 六种 HTTP 方法，
 * 以及文件下载功能。支持自定义请求头（如 Authorization 认证头）和 JSON 请求体。
 * 支持通过参数传入 SSL/TLS 证书配置（信任证书库、客户端证书 mTLS、不安全模式）。
 * </p>
 *
 * <h3>SSL 配置参数（sslConfig）</h3>
 * <pre>
 * {
 *   "insecure": true,                              // 跳过证书验证（仅测试环境）
 *   "trustStorePath": "/path/to/truststore.jks",   // 信任证书库路径（JKS/PKCS12）
 *   "trustStorePassword": "changeit",              // 信任证书库密码
 *   "keyStorePath": "/path/to/keystore.jks",       // 客户端证书路径（mTLS）
 *   "keyStorePassword": "changeit"                 // 客户端证书密码
 * }
 * </pre>
 * <p>所有 SSL 字段均为可选，不传 sslConfig 则使用 JDK 默认证书验证。</p>
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class HttpClientHandler extends BaseHandler {

    @Value("${http.timeout-seconds:30}")
    private int timeoutSeconds;

    /**
     * 默认 HttpClient（无自定义 SSL 配置），延迟初始化
     */
    private HttpClient defaultHttpClient;

    /**
     * 获取或创建默认 HttpClient（无 SSL 配置）。
     *
     * @return 默认 HttpClient 实例
     */
    private HttpClient getDefaultHttpClient() {
        if (defaultHttpClient == null) {
            defaultHttpClient = SslUtil.createHttpClient(null, Duration.ofSeconds(timeoutSeconds));
        }
        return defaultHttpClient;
    }

    /**
     * 根据 SSL 配置参数获取 HttpClient。
     * <p>如果 sslConfigJson 为空则返回默认 HttpClient，否则根据参数创建新实例。</p>
     *
     * @param sslConfigJson SSL 配置 JSON 字符串
     * @param timeout       超时时间（秒）
     * @return 配置好的 HttpClient 实例
     * @throws Exception SSL 配置解析失败时抛出
     */
    private HttpClient getHttpClient(String sslConfigJson, int timeout) throws Exception {
        if (sslConfigJson == null || sslConfigJson.isBlank()) {
            return getDefaultHttpClient();
        }

        JSONObject sslConfig = JSON.parseObject(sslConfigJson);
        boolean insecure = sslConfig.getBooleanValue("insecure", false);
        String trustStorePath = sslConfig.getString("trustStorePath");
        String trustStorePassword = sslConfig.getString("trustStorePassword");
        String keyStorePath = sslConfig.getString("keyStorePath");
        String keyStorePassword = sslConfig.getString("keyStorePassword");

        SSLContext sslContext = SslUtil.createSslContext(insecure, trustStorePath, trustStorePassword, keyStorePath, keyStorePassword);
        return SslUtil.createHttpClient(sslContext, Duration.ofSeconds(timeout));
    }

    // ==================== HTTP GET ====================

    /**
     * 发送 HTTP GET 请求，返回响应状态码、响应头和响应体。
     *
     * @param url       请求 URL
     * @param headers   自定义请求头（JSON 格式，如 {"Authorization":"Bearer xxx"}），可选
     * @param timeout   超时时间（秒），不传则使用默认值
     * @param sslConfig SSL 证书配置（JSON 格式），可选，详见类文档
     * @return 包含状态码、响应头和响应体的 JSON 字符串
     */
    @Tool(name = "http_get", description = "发送 HTTP GET 请求，返回状态码、响应头和响应体。支持自定义请求头和 SSL 证书配置。")
    public String httpGet(@ToolParam(description = "请求 URL，例如 https://api.example.com/data") String url,
        @ToolParam(description = "自定义请求头，JSON 格式，如 {\"Authorization\":\"Bearer xxx\"}，可选") String headers,
        @ToolParam(description = "超时时间（秒），默认 30 秒") Integer timeout,
        @ToolParam(description = "SSL 证书配置，JSON 格式，可选。字段: insecure(跳过验证), trustStorePath(信任证书库路径), trustStorePassword(信任证书库密码), keyStorePath(客户端证书路径), keyStorePassword(客户端证书密码)") String sslConfig) {
        return execute("http_get", () -> executeRequest("GET", url, headers, null, timeout, sslConfig));
    }

    // ==================== HTTP POST ====================

    /**
     * 发送 HTTP POST 请求，支持 JSON 请求体，返回响应状态码、响应头和响应体。
     *
     * @param url       请求 URL
     * @param body      请求体（JSON 字符串），可选
     * @param headers   自定义请求头（JSON 格式），可选
     * @param timeout   超时时间（秒），不传则使用默认值
     * @param sslConfig SSL 证书配置（JSON 格式），可选
     * @return 包含状态码、响应头和响应体的 JSON 字符串
     */
    @Tool(name = "http_post", description = "发送 HTTP POST 请求，支持 JSON 请求体、自定义请求头和 SSL 证书配置。返回状态码、响应头和响应体。")
    public String httpPost(@ToolParam(description = "请求 URL，例如 https://api.example.com/create") String url,
        @ToolParam(description = "请求体（JSON 字符串），可选，例如 {\"name\":\"test\"}") String body,
        @ToolParam(description = "自定义请求头，JSON 格式，如 {\"Authorization\":\"Bearer xxx\"}，可选") String headers,
        @ToolParam(description = "超时时间（秒），默认 30 秒") Integer timeout,
        @ToolParam(description = "SSL 证书配置，JSON 格式，可选。字段: insecure, trustStorePath, trustStorePassword, keyStorePath, keyStorePassword") String sslConfig) {
        return execute("http_post", () -> executeRequest("POST", url, headers, body, timeout, sslConfig));
    }

    // ==================== HTTP PUT ====================

    /**
     * 发送 HTTP PUT 请求，用于更新资源，返回响应状态码、响应头和响应体。
     *
     * @param url       请求 URL
     * @param body      请求体（JSON 字符串），可选
     * @param headers   自定义请求头（JSON 格式），可选
     * @param timeout   超时时间（秒），不传则使用默认值
     * @param sslConfig SSL 证书配置（JSON 格式），可选
     * @return 包含状态码、响应头和响应体的 JSON 字符串
     */
    @Tool(name = "http_put", description = "发送 HTTP PUT 请求，用于更新资源。支持 JSON 请求体、自定义请求头和 SSL 证书配置。返回状态码、响应头和响应体。")
    public String httpPut(@ToolParam(description = "请求 URL，例如 https://api.example.com/resource/1") String url,
        @ToolParam(description = "请求体（JSON 字符串），可选，例如 {\"name\":\"updated\"}") String body,
        @ToolParam(description = "自定义请求头，JSON 格式，如 {\"Authorization\":\"Bearer xxx\"}，可选") String headers,
        @ToolParam(description = "超时时间（秒），默认 30 秒") Integer timeout,
        @ToolParam(description = "SSL 证书配置，JSON 格式，可选。字段: insecure, trustStorePath, trustStorePassword, keyStorePath, keyStorePassword") String sslConfig) {
        return execute("http_put", () -> executeRequest("PUT", url, headers, body, timeout, sslConfig));
    }

    // ==================== HTTP DELETE ====================

    /**
     * 发送 HTTP DELETE 请求，用于删除资源，返回响应状态码、响应头和响应体。
     *
     * @param url       请求 URL
     * @param headers   自定义请求头（JSON 格式），可选
     * @param timeout   超时时间（秒），不传则使用默认值
     * @param sslConfig SSL 证书配置（JSON 格式），可选
     * @return 包含状态码、响应头和响应体的 JSON 字符串
     */
    @Tool(name = "http_delete", description = "发送 HTTP DELETE 请求，用于删除资源。支持自定义请求头和 SSL 证书配置。返回状态码、响应头和响应体。")
    public String httpDelete(@ToolParam(description = "请求 URL，例如 https://api.example.com/resource/1") String url,
        @ToolParam(description = "自定义请求头，JSON 格式，如 {\"Authorization\":\"Bearer xxx\"}，可选") String headers,
        @ToolParam(description = "超时时间（秒），默认 30 秒") Integer timeout,
        @ToolParam(description = "SSL 证书配置，JSON 格式，可选。字段: insecure, trustStorePath, trustStorePassword, keyStorePath, keyStorePassword") String sslConfig) {
        return execute("http_delete", () -> executeRequest("DELETE", url, headers, null, timeout, sslConfig));
    }

    // ==================== HTTP PATCH ====================

    /**
     * 发送 HTTP PATCH 请求，用于部分更新资源，返回响应状态码、响应头和响应体。
     *
     * @param url       请求 URL
     * @param body      请求体（JSON 字符串），可选
     * @param headers   自定义请求头（JSON 格式），可选
     * @param timeout   超时时间（秒），不传则使用默认值
     * @param sslConfig SSL 证书配置（JSON 格式），可选
     * @return 包含状态码、响应头和响应体的 JSON 字符串
     */
    @Tool(name = "http_patch", description = "发送 HTTP PATCH 请求，用于部分更新资源。支持 JSON 请求体、自定义请求头和 SSL 证书配置。返回状态码、响应头和响应体。")
    public String httpPatch(@ToolParam(description = "请求 URL，例如 https://api.example.com/resource/1") String url,
        @ToolParam(description = "请求体（JSON 字符串），可选，例如 {\"status\":\"active\"}") String body,
        @ToolParam(description = "自定义请求头，JSON 格式，如 {\"Authorization\":\"Bearer xxx\"}，可选") String headers,
        @ToolParam(description = "超时时间（秒），默认 30 秒") Integer timeout,
        @ToolParam(description = "SSL 证书配置，JSON 格式，可选。字段: insecure, trustStorePath, trustStorePassword, keyStorePath, keyStorePassword") String sslConfig) {
        return execute("http_patch", () -> executeRequest("PATCH", url, headers, body, timeout, sslConfig));
    }

    // ==================== HTTP HEAD ====================

    /**
     * 发送 HTTP HEAD 请求，仅返回响应状态码和响应头（无响应体）。
     *
     * @param url       请求 URL
     * @param timeout   超时时间（秒），不传则使用默认值
     * @param sslConfig SSL 证书配置（JSON 格式），可选
     * @return 包含状态码和响应头的 JSON 字符串
     */
    @Tool(name = "http_head", description = "发送 HTTP HEAD 请求，仅返回响应状态码和响应头，不返回响应体。支持 SSL 证书配置。")
    public String httpHead(@ToolParam(description = "请求 URL，例如 https://example.com") String url,
        @ToolParam(description = "超时时间（秒），默认 30 秒") Integer timeout,
        @ToolParam(description = "SSL 证书配置，JSON 格式，可选。字段: insecure, trustStorePath, trustStorePassword, keyStorePath, keyStorePassword") String sslConfig) {
        return execute("http_head", () -> executeRequest("HEAD", url, null, null, timeout, sslConfig));
    }

    // ==================== HTTP 下载 ====================

    /**
     * 下载文件到本地磁盘，支持大文件下载，返回下载结果描述。
     *
     * @param url        文件下载 URL
     * @param targetPath 目标文件保存路径
     * @param headers    自定义请求头（JSON 格式），可选
     * @param timeout    超时时间（秒），不传则使用默认值
     * @param sslConfig  SSL 证书配置（JSON 格式），可选
     * @return 下载结果描述（文件路径和大小）
     */
    @Tool(name = "http_download", description = "下载文件到本地磁盘，支持大文件下载、自定义请求头和 SSL 证书配置。返回保存路径和文件大小。")
    public String httpDownload(@ToolParam(description = "文件下载 URL，例如 https://example.com/file.zip") String url,
        @ToolParam(description = "目标文件保存路径，例如 /downloads/file.zip") String targetPath,
        @ToolParam(description = "自定义请求头，JSON 格式，如 {\"Authorization\":\"Bearer xxx\"}，可选") String headers,
        @ToolParam(description = "超时时间（秒），默认 30 秒") Integer timeout,
        @ToolParam(description = "SSL 证书配置，JSON 格式，可选。字段: insecure, trustStorePath, trustStorePassword, keyStorePath, keyStorePassword") String sslConfig) {
        return execute("http_download", () -> {
            Path path = PathUtil.resolvePath(targetPath);
            PathUtil.ensureParentDirectory(path);

            int effectiveTimeout = (timeout != null && timeout > 0) ? timeout : timeoutSeconds;
            HttpClient client = getHttpClient(sslConfig, effectiveTimeout);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                                                            .uri(URI.create(url))
                                                            .timeout(Duration.ofSeconds(effectiveTimeout))
                                                            .GET();

            if (headers != null && !headers.isBlank()) {
                JSONObject headerObj = JSON.parseObject(headers);
                for (String key : headerObj.keySet()) {
                    requestBuilder.header(key, String.valueOf(headerObj.get(key)));
                }
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorMsg = String.format("下载失败: HTTP %d - %s", response.statusCode(), new String(response.body()));
                LogUtil.warn("http_download 失败: url={}, statusCode={}", url, response.statusCode());
                return "错误: " + errorMsg;
            }

            Files.write(path, response.body());
            long fileSize = Files.size(path);
            LogUtil.info("http_download 成功: url={}, targetPath={}, size={} 字节", url, targetPath, fileSize);
            return String.format("下载成功: %s (%d 字节)", path.toAbsolutePath(), fileSize);
        });
    }

    // ==================== 核心请求执行方法 ====================

    /**
     * 执行 HTTP 请求的核心方法，统一处理 GET / POST / PUT / DELETE / PATCH / HEAD 六种方法。
     *
     * @param method        HTTP 方法（GET/POST/PUT/DELETE/PATCH/HEAD）
     * @param url           请求 URL
     * @param headersJson   自定义请求头（JSON 格式）
     * @param body          请求体，仅 POST/PUT/PATCH 使用
     * @param timeout       超时时间（秒）
     * @param sslConfigJson SSL 配置 JSON 字符串
     * @return 格式化的响应 JSON 字符串
     * @throws Exception 网络异常或 SSL 配置异常
     */
    private String executeRequest(String method, String url, String headersJson, String body, Integer timeout, String sslConfigJson)
        throws Exception {

        int effectiveTimeout = (timeout != null && timeout > 0) ? timeout : timeoutSeconds;
        HttpClient client = getHttpClient(sslConfigJson, effectiveTimeout);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                                                        .uri(URI.create(url))
                                                        .timeout(Duration.ofSeconds(effectiveTimeout));

        // 设置请求方法和请求体
        switch (method) {
            case "GET" -> requestBuilder.GET();
            case "DELETE" -> requestBuilder.DELETE();
            case "HEAD" -> requestBuilder.method("HEAD", HttpRequest.BodyPublishers.noBody());
            case "POST" -> requestBuilder.method("POST", buildBodyPublisher(body));
            case "PUT" -> requestBuilder.method("PUT", buildBodyPublisher(body));
            case "PATCH" -> requestBuilder.method("PATCH", buildBodyPublisher(body));
            default -> throw new IllegalArgumentException("不支持的 HTTP 方法: " + method);
        }

        // 添加自定义请求头
        if (headersJson != null && !headersJson.isBlank()) {
            JSONObject headerObj = JSON.parseObject(headersJson);
            for (String key : headerObj.keySet()) {
                requestBuilder.header(key, String.valueOf(headerObj.get(key)));
            }
        }

        // 如果请求体不为空且未设置 Content-Type，默认设为 application/json
        if (body != null && !body.isBlank() && !hasContentTypeHeader(headersJson)) {
            requestBuilder.header("Content-Type", "application/json");
        }

        HttpRequest request = requestBuilder.build();
        LogUtil.info("http_{} 请求: url={}, timeout={}s", method.toLowerCase(), url, effectiveTimeout);

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject result = new JSONObject();
        result.put("statusCode", response.statusCode());
        result.put("statusText", getStatusText(response.statusCode()));

        // 序列化响应头
        JSONObject respHeaders = new JSONObject();
        response.headers()
                .map()
                .forEach((key, values) -> {
                    respHeaders.put(key, String.join(", ", values));
                });
        result.put("headers", respHeaders);

        // HEAD 请求不返回响应体
        if (!"HEAD".equals(method)) {
            result.put("body", response.body());
        }

        LogUtil.info("http_{} 响应: url={}, statusCode={}", method.toLowerCase(), url, response.statusCode());
        return result.toJSONString();
    }

    /**
     * 构建请求体发布器，将 JSON 字符串转为 BodyPublisher。
     *
     * @param body 请求体 JSON 字符串
     * @return BodyPublisher，若 body 为空则返回 noBody
     */
    private HttpRequest.BodyPublisher buildBodyPublisher(String body) {
        if (body == null || body.isBlank()) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofString(body);
    }

    /**
     * 检查自定义请求头中是否已包含 Content-Type。
     *
     * @param headersJson 自定义请求头 JSON 字符串
     * @return true 如果已包含 Content-Type
     */
    private boolean hasContentTypeHeader(String headersJson) {
        if (headersJson == null || headersJson.isBlank()) {
            return false;
        }
        try {
            JSONObject obj = JSON.parseObject(headersJson);
            return obj.keySet()
                      .stream()
                      .anyMatch(k -> k.equalsIgnoreCase("Content-Type"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 根据 HTTP 状态码返回状态描述文本。
     *
     * @param statusCode HTTP 状态码
     * @return 状态描述文本
     */
    private String getStatusText(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> statusCode >= 200 && statusCode < 300 ? "OK" : "Error";
        };
    }
}