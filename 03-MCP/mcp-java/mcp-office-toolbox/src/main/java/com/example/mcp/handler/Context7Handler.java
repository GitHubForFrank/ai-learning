package com.example.mcp.handler;

import com.example.mcp.pojo.context7.QueryDocsRequest;
import com.example.mcp.pojo.context7.ResolveLibraryRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.example.mcp.util.LogUtil;
import org.springframework.ai.tool.annotation.Tool;
import com.example.mcp.util.LogUtil;
import org.springframework.ai.tool.annotation.ToolParam;
import com.example.mcp.util.LogUtil;
import org.springframework.http.ResponseEntity;
import com.example.mcp.util.LogUtil;
import org.springframework.stereotype.Service;
import com.example.mcp.util.LogUtil;
import org.springframework.web.client.RestClient;

/**
 * Context7 MCP 工具 —— 复刻 @upstash/context7-mcp 的两个工具。
 * 提供实时库文档查询能力，解决 LLM 训练数据时效性问题。
 * 支持 resolve-library-id（库 ID 解析）和 query-docs（文档查询）。
 *
 * @author Frank Kang
 * @since 2026-07-10
 */
@Service
public class Context7Handler {

    /**
     * Context7 API 基础地址
     */
    private static final String API_BASE_URL = "https://context7.com/api";

    /**
     * 默认超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final RestClient restClient;

    public Context7Handler() {
        this.restClient = RestClient.builder()
                                    .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(
                                        java.net.http.HttpClient.newBuilder()
                                                                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                                                                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                                                                .build()))
                                    .build();
    }

    // ==================== MCP 工具方法 ====================

    /**
     * 将用户输入的库名称转换为 Context7 标准库 ID。
     * 例如将 "next.js" 解析为 "/vercel/next.js"。
     *
     * @param request 请求参数，包含 query 和 libraryName
     * @return 匹配结果列表，包含库 ID、名称、描述、版本等信息
     */
    @Tool(name = "resolve-library-id", description = "将库名称解析为 Context7 标准库 ID。输入库名称，返回匹配的库 ID 列表及元数据。")
    public String resolveLibraryId(@ToolParam(description = "请求参数，包含 query（用户问题）和 libraryName（库名称）") ResolveLibraryRequest request) {
        try {
            LogUtil.info("resolve-library-id: libraryName={}, query={}", request.libraryName(), request.query());

            ResponseEntity<Map> response = restClient.get()
                                                     .uri(URI.create(
                                                         API_BASE_URL + "/v2/libs/search?libraryName=" + encode(request.libraryName()) + "&query="
                                                             + encode(request.query())))
                                                     .retrieve()
                                                     .toEntity(Map.class);

            Map body = response.getBody();
            if (body == null) {
                return "错误：API 返回空响应";
            }

            List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
            if (results == null || results.isEmpty()) {
                return "未找到匹配的库: " + request.libraryName();
            }

            // 格式化输出结果
            StringBuilder sb = new StringBuilder();
            sb.append("找到 ")
              .append(results.size())
              .append(" 个匹配的库：\n\n");

            for (int i = 0; i < Math.min(results.size(), 5); i++) {
                Map<String, Object> result = results.get(i);
                String id = (String) result.get("id");
                String title = (String) result.get("title");
                String description = (String) result.get("description");
                Number totalSnippets = (Number) result.get("totalSnippets");
                Number trustScore = (Number) result.get("trustScore");
                List<String> versions = (List<String>) result.get("versions");

                sb.append(i + 1)
                  .append(". ")
                  .append(title)
                  .append("\n");
                sb.append("   ID: ")
                  .append(id)
                  .append("\n");
                if (description != null) {
                    sb.append("   描述: ")
                      .append(description)
                      .append("\n");
                }
                if (totalSnippets != null) {
                    sb.append("   代码片段数: ")
                      .append(totalSnippets)
                      .append("\n");
                }
                if (trustScore != null) {
                    sb.append("   可信度评分: ")
                      .append(String.format("%.1f/10", trustScore.doubleValue()))
                      .append("\n");
                }
                if (versions != null && !versions.isEmpty()) {
                    sb.append("   可用版本: ")
                      .append(String.join(", ", versions.subList(0, Math.min(versions.size(), 5))));
                    if (versions.size() > 5) {
                        sb.append(" 等")
                          .append(versions.size())
                          .append("个版本");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }

            return sb.toString()
                     .trim();
        } catch (Exception e) {
            LogUtil.error("resolve-library-id 失败: {}", e.getMessage(), e);
            return "错误：解析库 ID 失败 - " + e.getMessage();
        }
    }

    /**
     * 根据 Context7 标准库 ID 获取文档内容。
     * 支持指定查询主题和 token 数量限制。
     *
     * @param request 请求参数，包含 libraryId、query 和 tokens
     * @return 文档片段和代码示例
     */
    @Tool(name = "query-docs", description = "根据库 ID 查询最新文档。返回指定库的代码示例和文档片段。")
    public String queryDocs(
        @ToolParam(description = "请求参数，包含 libraryId（库 ID）、query（查询主题）和 tokens（最大 token 数）") QueryDocsRequest request) {
        try {
            LogUtil.info("query-docs: libraryId={}, query={}, tokens={}", request.libraryId(), request.query(), request.tokens());

            String url = API_BASE_URL + "/v2/context?libraryId=" + encode(request.libraryId()) + "&query=" + encode(request.query()) + "&tokens="
                + request.tokens() + "&type=json";

            ResponseEntity<Map> response = restClient.get()
                                                     .uri(URI.create(url))
                                                     .retrieve()
                                                     .toEntity(Map.class);

            Map body = response.getBody();
            if (body == null) {
                return "错误：API 返回空响应";
            }

            // 检查是否有错误
            String error = (String) body.get("error");
            if (error != null) {
                String message = (String) body.get("message");
                return "错误：" + error + " - " + message;
            }

            // 解析并格式化文档内容
            StringBuilder sb = new StringBuilder();
            sb.append("库 ID: ")
              .append(request.libraryId())
              .append("\n");
            sb.append("查询: ")
              .append(request.query())
              .append("\n\n");

            // 处理代码片段
            List<Map<String, Object>> codeSnippets = (List<Map<String, Object>>) body.get("codeSnippets");
            if (codeSnippets != null && !codeSnippets.isEmpty()) {
                sb.append("=== 代码示例 ===\n\n");
                for (Map<String, Object> snippet : codeSnippets) {
                    String title = (String) snippet.get("codeTitle");
                    if (title != null) {
                        sb.append("【")
                          .append(title)
                          .append("】\n");
                    }
                    List<Map<String, Object>> codeList = (List<Map<String, Object>>) snippet.get("codeList");
                    if (codeList != null) {
                        for (Map<String, Object> codeItem : codeList) {
                            String code = (String) codeItem.get("code");
                            if (code != null) {
                                sb.append("```")
                                  .append("\n")
                                  .append(code)
                                  .append("\n```\n\n");
                            }
                        }
                    }
                }
            }

            // 处理信息片段
            List<Map<String, Object>> infoSnippets = (List<Map<String, Object>>) body.get("infoSnippets");
            if (infoSnippets != null && !infoSnippets.isEmpty()) {
                sb.append("=== 文档内容 ===\n\n");
                for (Map<String, Object> info : infoSnippets) {
                    String content = (String) info.get("content");
                    if (content != null) {
                        sb.append(content)
                          .append("\n\n");
                    }
                }
            }

            if (codeSnippets == null && infoSnippets == null) {
                // 如果没有结构化数据，返回原始内容
                return body.toString();
            }

            return sb.toString()
                     .trim();
        } catch (Exception e) {
            LogUtil.error("query-docs 失败: {}", e.getMessage(), e);
            return "错误：查询文档失败 - " + e.getMessage();
        }
    }

    // ==================== 内部辅助方法 ====================

    /**
     * URL 编码
     */
    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
