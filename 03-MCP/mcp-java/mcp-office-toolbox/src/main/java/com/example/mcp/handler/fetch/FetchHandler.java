package com.example.mcp.handler.fetch;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.pojo.fetch.FetchRequest;
import com.example.mcp.util.LogUtil;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Fetch MCP 工具 —— 复刻 @tokenizin/mcp-npx-fetch 的四个工具。
 * 支持 fetch_html / fetch_json / fetch_txt / fetch_markdown。
 * 每个工具均支持可选的自定义请求头。
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@Service
public class FetchHandler extends BaseHandler {

    private final String userAgent;
    private final RestClient restClient;

    public FetchHandler(@Value("${fetch.user-agent}") String userAgent, @Value("${fetch.timeout-seconds}") int timeoutSeconds) {
        this.userAgent = userAgent;
        this.restClient = RestClient.builder()
                                    .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(
                                        java.net.http.HttpClient.newBuilder()
                                                                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                                                                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                                                                .build()))
                                    .build();
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 移除指定 HTML 标签及其内容（如 script、style 等）。
     */
    private static String removeTag(String html, String tagName) {
        return html.replaceAll("(?si)<" + Pattern.quote(tagName) + "[^>]*>.*?</" + Pattern.quote(tagName) + ">", "");
    }

    // ==================== HTML → Markdown 转换 ====================

    /**
     * 将指定 HTML 标签替换为 Markdown 前缀/后缀格式（如 # 标题、段落等）。
     */
    private static String replaceTag(String html, String tagName, String prefix, String suffix) {
        return html.replaceAll("(?si)<" + Pattern.quote(tagName) + "[^>]*>(.*?)</" + Pattern.quote(tagName) + ">", prefix + "$1" + suffix);
    }

    /**
     * 发起 GET 请求并返回响应体字符串。
     *
     * @param request 请求参数，包含 url 和 headers
     * @return 响应体内容
     */
    private String doFetch(FetchRequest request) {
        String url = request.url();
        Map<String, String> headers = request.headers();

        LogUtil.info("Fetching URL: {}", url);

        var reqSpec = restClient.get()
                                .uri(URI.create(url))
                                .header("User-Agent", userAgent);

        // 添加自定义请求头
        if (headers != null) {
            headers.forEach(reqSpec::header);
        }

        ResponseEntity<String> response = reqSpec.retrieve()
                                                 .toEntity(String.class);
        String content = response.getBody();
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Empty response body from " + url);
        }
        return content;
    }

    /**
     * 将 HTML 内容转换为 Markdown 格式。
     * 处理：标题、加粗、斜体、链接、图片、代码块、引用、列表、分割线等。
     */
    private String htmlToMarkdown(String html) {
        String md = html;

        // 1. 移除非内容标签（脚本、样式、注释等）
        md = removeTag(md, "script");
        md = removeTag(md, "style");
        md = removeTag(md, "noscript");
        md = removeTag(md, "head");
        md = md.replaceAll("(?s)<!--.*?-->", "");

        // 2. 标题 h1-h6
        md = replaceTag(md, "h1", "\n\n# ", "\n\n");
        md = replaceTag(md, "h2", "\n\n## ", "\n\n");
        md = replaceTag(md, "h3", "\n\n### ", "\n\n");
        md = replaceTag(md, "h4", "\n\n#### ", "\n\n");
        md = replaceTag(md, "h5", "\n\n##### ", "\n\n");
        md = replaceTag(md, "h6", "\n\n###### ", "\n\n");

        // 3. 加粗 <strong>/<b>
        md = md.replaceAll("(?si)<(?:strong|b)[^>]*>(.*?)</(?:strong|b)>", "**$1**");

        // 4. 斜体 <em>/<i>
        md = md.replaceAll("(?si)<(?:em|i)[^>]*>(.*?)</(?:em|i)>", "*$1*");

        // 5. 链接 <a href="...">text</a>
        md = md.replaceAll("(?si)<a[^>]*href\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>(.*?)</a>", "[$2]($1)");

        // 6. 图片 <img>（多种属性顺序）
        md = md.replaceAll("(?si)<img[^>]*src\\s*=\\s*[\"']([^\"']*)[\"'][^>]*alt\\s*=\\s*[\"']([^\"']*)[\"'][^>]*/?\\s*>", "![$2]($1)");
        md = md.replaceAll("(?si)<img[^>]*alt\\s*=\\s*[\"']([^\"']*)[\"'][^>]*src\\s*=\\s*[\"']([^\"']*)[\"'][^>]*/?\\s*>", "![$1]($2)");
        md = md.replaceAll("(?si)<img[^>]*src\\s*=\\s*[\"']([^\"']*)[\"'][^>]*/?\\s*>", "![]($1)");

        // 7. 代码块 <pre><code>...</code></pre>
        md = md.replaceAll("(?si)<pre[^>]*>\\s*<code[^>]*>(.*?)</code>\\s*</pre>", "\n\n```\n$1\n```\n\n");

        // 8. 行内代码 <code>...</code>
        md = md.replaceAll("(?si)<code[^>]*>(.*?)</code>", "`$1`");

        // 9. 引用 <blockquote>
        md = md.replaceAll("(?si)<blockquote[^>]*>(.*?)</blockquote>", "\n\n> $1\n\n");

        // 10. 分割线 <hr>
        md = md.replaceAll("(?si)<hr[^>]*/?\\s*>", "\n\n---\n\n");

        // 11. 列表 <li>
        md = md.replaceAll("(?si)<li[^>]*>(.*?)</li>", "\n- $1");
        md = md.replaceAll("(?si)</?[ou]l[^>]*>", "\n");

        // 12. 换行 <br> 和段落 <p>/<div>
        md = md.replaceAll("(?si)<br[^>]*/?\\s*>", "\n");
        md = md.replaceAll("(?si)</?p[^>]*>", "\n\n");
        md = md.replaceAll("(?si)</?div[^>]*>", "\n");

        // 13. 删除所有剩余 HTML 标签
        md = md.replaceAll("<[^>]+>", "");

        // 14. 解码 HTML 实体
        md = decodeHtmlEntities(md);

        // 15. 规范化空白字符
        md = md.replaceAll("\\n{3,}", "\n\n");
        md = md.trim();

        return md;
    }

    /**
     * 移除 HTML 中所有标签和脚本，仅保留纯文本内容。
     */
    private String stripHtmlToText(String html) {
        String text = html;
        // 移除 script、style 等非内容标签
        text = removeTag(text, "script");
        text = removeTag(text, "style");
        text = removeTag(text, "noscript");
        text = removeTag(text, "head");
        text = text.replaceAll("(?s)<!--.*?-->", "");
        // 移除所有 HTML 标签
        text = text.replaceAll("<[^>]+>", "");
        // 解码 HTML 实体
        text = decodeHtmlEntities(text);
        // 规范化空白
        text = text.replaceAll("\\s+", " ")
                   .trim();
        return text;
    }

    /**
     * 解码常见 HTML 实体和数字字符引用。
     */
    private String decodeHtmlEntities(String text) {
        text = text.replace("&amp;", "&");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&quot;", "\"");
        text = text.replace("&apos;", "'");
        text = text.replace("&#39;", "'");
        text = text.replace("&nbsp;", " ");

        // 十进制数字实体 &#dddd;
        text = Pattern.compile("&#(\\d+);")
                      .matcher(text)
                      .replaceAll(mr -> {
                          int cp = Integer.parseInt(mr.group(1));
                          return Character.toString(cp);
                      });

        // 十六进制数字实体 &#xhhhh;
        text = Pattern.compile("&#x([0-9a-fA-F]+);")
                      .matcher(text)
                      .replaceAll(mr -> {
                          int cp = Integer.parseInt(mr.group(1), 16);
                          return Character.toString(cp);
                      });

        return text;
    }

    // ==================== MCP 工具方法 ====================

    /**
     * 从指定 URL 抓取原始 HTML 内容。
     *
     * @param request 请求参数，包含 url 和可选 headers
     * @return 原始 HTML 内容
     */
    @Tool(name = "fetch_html", description = "从指定 URL 抓取并返回原始 HTML 内容。")
    public String fetchHtml(@ToolParam(description = "请求参数，包含 url 和可选的 headers") FetchRequest request) {
        return execute("fetch_html", () -> {
            String content = doFetch(request);
            return content;
        });
    }

    /**
     * 从指定 URL 抓取并解析 JSON 数据。
     *
     * @param request 请求参数，包含 url 和可选 headers
     * @return 格式化后的 JSON 字符串
     */
    @Tool(name = "fetch_json", description = "从指定 URL 抓取并解析 JSON 数据。")
    public String fetchJson(@ToolParam(description = "请求参数，包含 url 和可选的 headers") FetchRequest request) {
        return execute("fetch_json", () -> {
            String content = doFetch(request);
            // 尝试格式化 JSON，使其更易读
            try {
                com.alibaba.fastjson2.JSON.parse(content);
                // 使用 fastjson2 格式化输出
                Object parsed = com.alibaba.fastjson2.JSON.parse(content);
                return com.alibaba.fastjson2.JSON.toJSONString(parsed, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);
            } catch (Exception e) {
                // 如果不是有效 JSON，返回原始内容
                return content;
            }
        });
    }

    /**
     * 从指定 URL 抓取并返回纯文本内容（移除 HTML 标签和脚本）。
     *
     * @param request 请求参数，包含 url 和可选 headers
     * @return 纯文本内容
     */
    @Tool(name = "fetch_txt", description = "从指定 URL 抓取并返回纯文本内容，自动移除 HTML 标签和脚本。")
    public String fetchTxt(@ToolParam(description = "请求参数，包含 url 和可选的 headers") FetchRequest request) {
        return execute("fetch_txt", () -> {
            String content = doFetch(request);
            // 如果是 HTML，则转换为纯文本
            return stripHtmlToText(content);
        });
    }

    /**
     * 从指定 URL 抓取内容并转换为格式良好的 Markdown。
     *
     * @param request 请求参数，包含 url 和可选 headers
     * @return Markdown 格式的内容
     */
    @Tool(name = "fetch_markdown", description = "从指定 URL 抓取内容并转换为格式良好的 Markdown。")
    public String fetchMarkdown(@ToolParam(description = "请求参数，包含 url 和可选的 headers") FetchRequest request) {
        return execute("fetch_markdown", () -> {
            String content = doFetch(request);
            // 如果是 HTML，则转换为 Markdown
            return htmlToMarkdown(content);
        });
    }
}
