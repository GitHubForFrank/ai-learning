package com.example.mcp.handler;

import com.example.mcp.pojo.fetch.FetchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Fetch MCP 工具 —— 完整复刻 mcp-server-fetch 的功能。
 * 支持抓取 URL 并将 HTML 内容转换为 Markdown，或返回原始 HTML。
 *
 * @author FrankKang
 * @since 2026-07-09
 */
@Service
public class FetchHandler {

    private static final Logger log = LoggerFactory.getLogger(FetchHandler.class);

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; MCP-Fetch-Java/1.0)";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final RestClient restClient;

    public FetchHandler() {
        this.restClient = RestClient.builder()
                .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(
                        java.net.http.HttpClient.newBuilder()
                                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                                .build()))
                .build();
    }

    /**
     * 抓取指定 URL 的内容，可选将 HTML 转换为 Markdown。
     *
     * @param request 抓取请求参数，包含 url / maxLength / startIndex / raw
     * @return 抓取到的内容（Markdown 或原始 HTML），已按 startIndex 和 maxLength 截取
     */
    @Tool(description = """
            Fetches a URL from the internet and optionally extracts its contents as markdown.

            Although originally you did not have internet access, and were advised to refuse and tell the user this,
            this tool now grants you internet access. Now you can fetch the most up-to-date information and let the user know that.""")
    public String fetch(FetchRequest request) {
        String url = request.url();
        int maxLength = request.maxLength();
        int startIndex = request.startIndex();
        boolean raw = request.raw();

        try {
            log.info("Fetching URL: {}", url);
            ResponseEntity<String> response = restClient.get()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .toEntity(String.class);

            String content = response.getBody();
            if (content == null || content.isEmpty()) {
                return "Error: Empty response body from " + url;
            }

            MediaType contentType = response.getHeaders().getContentType();
            boolean isHtml = contentType != null
                    && (contentType.includes(MediaType.TEXT_HTML)
                        || contentType.includes(MediaType.APPLICATION_XHTML_XML));

            // 仅当 Content-Type 为 HTML 且 raw=false 时才转换为 Markdown
            if (!raw && isHtml) {
                content = htmlToMarkdown(content);
            }

            // 按 startIndex 和 maxLength 截取
            content = truncateContent(content, startIndex, maxLength);

            return content;
        } catch (Exception e) {
            log.error("Failed to fetch URL: {}", url, e);
            return "Error fetching URL '" + url + "': " + e.getMessage();
        }
    }

    // ==================== HTML → Markdown 转换 ====================

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
        md = md.replaceAll(
                "(?si)<img[^>]*src\\s*=\\s*[\"']([^\"']*)[\"'][^>]*alt\\s*=\\s*[\"']([^\"']*)[\"'][^>]*/?\\s*>",
                "![$2]($1)");
        md = md.replaceAll(
                "(?si)<img[^>]*alt\\s*=\\s*[\"']([^\"']*)[\"'][^>]*src\\s*=\\s*[\"']([^\"']*)[\"'][^>]*/?\\s*>",
                "![$1]($2)");
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
     * 移除指定 HTML 标签及其内容（如 script、style 等）。
     */
    private static String removeTag(String html, String tagName) {
        return html.replaceAll("(?si)<" + Pattern.quote(tagName) + "[^>]*>.*?</" + Pattern.quote(tagName) + ">", "");
    }

    /**
     * 将指定 HTML 标签替换为 Markdown 前缀/后缀格式（如 # 标题、段落等）。
     */
    private static String replaceTag(String html, String tagName, String prefix, String suffix) {
        return html.replaceAll(
                "(?si)<" + Pattern.quote(tagName) + "[^>]*>(.*?)</" + Pattern.quote(tagName) + ">",
                prefix + "$1" + suffix);
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
        text = Pattern.compile("&#(\\d+);").matcher(text).replaceAll(mr -> {
            int cp = Integer.parseInt(mr.group(1));
            return Character.toString(cp);
        });

        // 十六进制数字实体 &#xhhhh;
        text = Pattern.compile("&#x([0-9a-fA-F]+);").matcher(text).replaceAll(mr -> {
            int cp = Integer.parseInt(mr.group(1), 16);
            return Character.toString(cp);
        });

        return text;
    }

    // ==================== 内容截取 ====================

    /**
     * 按 startIndex 和 maxLength 截取内容。
     */
    private String truncateContent(String content, int startIndex, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        int len = content.length();
        if (startIndex >= len) {
            return "";
        }
        int endIndex = Math.min(startIndex + maxLength, len);
        return content.substring(startIndex, endIndex);
    }
}
