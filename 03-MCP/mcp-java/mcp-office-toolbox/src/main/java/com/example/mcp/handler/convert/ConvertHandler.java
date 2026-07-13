package com.example.mcp.handler.convert;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.util.LogUtil;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 文档转换工具实现，提供 HTML 转 PDF、Markdown 转 PDF、Markdown 转 Word 功能。
 * 基于 OpenHTMLToPDF、flexmark 和 Apache POI 实现。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class ConvertHandler extends BaseHandler {

    /**
     * Flexmark Markdown 解析器（线程安全，复用单例）
     */
    private static final Parser MD_PARSER = Parser.builder(new MutableDataSet())
                                                  .build();

    /**
     * Flexmark HTML 渲染器（线程安全，复用单例）
     */
    private static final HtmlRenderer MD_HTML_RENDERER = HtmlRenderer.builder(new MutableDataSet())
                                                                     .build();

    /**
     * 将 Markdown 内容转换为 HTML
     */
    private String markdownToHtml(String markdown) {
        return MD_HTML_RENDERER.render(MD_PARSER.parse(markdown));
    }

    /**
     * 获取 HTML 内容：优先从文件路径读取，否则使用直接传入的内容字符串
     */
    private String resolveHtmlContent(String htmlFilePath, String htmlContent) throws IOException {
        if (htmlFilePath != null && !htmlFilePath.isBlank()) {
            Path path = Paths.get(htmlFilePath);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("HTML 文件不存在: " + htmlFilePath);
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        if (htmlContent != null && !htmlContent.isBlank()) {
            return htmlContent;
        }
        throw new IllegalArgumentException("htmlFilePath 和 htmlContent 不能同时为空");
    }

    /**
     * 获取 Markdown 内容：优先从文件路径读取，否则使用直接传入的内容字符串
     */
    private String resolveMdContent(String mdFilePath, String mdContent) throws IOException {
        if (mdFilePath != null && !mdFilePath.isBlank()) {
            Path path = Paths.get(mdFilePath);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Markdown 文件不存在: " + mdFilePath);
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        if (mdContent != null && !mdContent.isBlank()) {
            return mdContent;
        }
        throw new IllegalArgumentException("mdFilePath 和 mdContent 不能同时为空");
    }

    /**
     * 确保目标文件父目录存在
     */
    private void ensureParentDir(String targetPath) throws IOException {
        Path path = Paths.get(targetPath);
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    /**
     * 使用 OpenHTMLToPDF 将 HTML 内容转换为 PDF
     */
    private void htmlToPdf(String htmlContent, String targetPath) throws IOException {
        ensureParentDir(targetPath);
        try (FileOutputStream os = new FileOutputStream(targetPath)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(os);
            builder.run();
        }
    }

    // --- 1. html_to_pdf ---

    /**
     * 将 HTML 文件或 HTML 内容转换为 PDF 文件。
     * 优先使用 htmlFilePath 参数，若为空则使用 htmlContent。
     *
     * @param htmlFilePath HTML 文件绝对路径（与 htmlContent 二选一，优先使用）
     * @param htmlContent  HTML 内容字符串（与 htmlFilePath 二选一）
     * @param targetPath   输出 PDF 文件的绝对路径
     * @return 转换结果消息
     */
    @Tool(name = "convert_html_to_pdf", description = "将 HTML 文件或 HTML 内容转换为 PDF 文件。支持传入文件路径或直接传入 HTML 内容字符串。")
    public String htmlToPdf(@ToolParam(description = "HTML 文件绝对路径（与 htmlContent 二选一，优先使用）", required = false) String htmlFilePath,
        @ToolParam(description = "HTML 内容字符串（与 htmlFilePath 二选一）", required = false) String htmlContent,
        @ToolParam(description = "输出 PDF 文件的绝对路径") String targetPath) {
        return execute("convert_html_to_pdf", () -> {
            String html = resolveHtmlContent(htmlFilePath, htmlContent);
            htmlToPdf(html, targetPath);
            LogUtil.info("HTML 转 PDF 完成: {}", targetPath);
            return "HTML 已成功转换为 PDF: " + targetPath;
        });
    }

    // --- 2. md_to_pdf ---

    /**
     * 将 Markdown 文件或 Markdown 内容转换为 PDF 文件。
     * 先使用 flexmark 将 Markdown 转为 HTML，再使用 OpenHTMLToPDF 将 HTML 转为 PDF。
     * 优先使用 mdFilePath 参数，若为空则使用 mdContent。
     *
     * @param mdFilePath Markdown 文件绝对路径（与 mdContent 二选一，优先使用）
     * @param mdContent  Markdown 内容字符串（与 mdFilePath 二选一）
     * @param targetPath 输出 PDF 文件的绝对路径
     * @return 转换结果消息
     */
    @Tool(name = "convert_md_to_pdf", description = "将 Markdown 文件或 Markdown 内容转换为 PDF 文件。支持传入文件路径或直接传入 Markdown 内容字符串。")
    public String mdToPdf(@ToolParam(description = "Markdown 文件绝对路径（与 mdContent 二选一，优先使用）", required = false) String mdFilePath,
        @ToolParam(description = "Markdown 内容字符串（与 mdFilePath 二选一）", required = false) String mdContent,
        @ToolParam(description = "输出 PDF 文件的绝对路径") String targetPath) {
        return execute("convert_md_to_pdf", () -> {
            String md = resolveMdContent(mdFilePath, mdContent);
            String html = markdownToHtml(md);
            // 包装完整 HTML 结构，添加中文字体支持
            String fullHtml = wrapHtmlDocument(html);
            htmlToPdf(fullHtml, targetPath);
            LogUtil.info("Markdown 转 PDF 完成: {}", targetPath);
            return "Markdown 已成功转换为 PDF: " + targetPath;
        });
    }

    // --- 3. md_to_docx ---

    /**
     * 将 Markdown 文件或 Markdown 内容转换为 Word (DOCX) 文件。
     * 先使用 flexmark 将 Markdown 转为 HTML，再解析 HTML 分段写入 XWPFDocument。
     * 优先使用 mdFilePath 参数，若为空则使用 mdContent。
     *
     * @param mdFilePath Markdown 文件绝对路径（与 mdContent 二选一，优先使用）
     * @param mdContent  Markdown 内容字符串（与 mdFilePath 二选一）
     * @param targetPath 输出 DOCX 文件的绝对路径
     * @return 转换结果消息
     */
    @Tool(name = "convert_md_to_docx", description = "将 Markdown 文件或 Markdown 内容转换为 Word (DOCX) 文件。支持传入文件路径或直接传入 Markdown 内容字符串。")
    public String mdToDocx(@ToolParam(description = "Markdown 文件绝对路径（与 mdContent 二选一，优先使用）", required = false) String mdFilePath,
        @ToolParam(description = "Markdown 内容字符串（与 mdFilePath 二选一）", required = false) String mdContent,
        @ToolParam(description = "输出 DOCX 文件的绝对路径") String targetPath) {
        return execute("convert_md_to_docx", () -> {
            String md = resolveMdContent(mdFilePath, mdContent);
            String html = markdownToHtml(md);

            ensureParentDir(targetPath);

            // 将 HTML 解析为段落并写入 DOCX
            try (XWPFDocument document = new XWPFDocument(); FileOutputStream fos = new FileOutputStream(targetPath)) {

                String[] blocks = splitHtmlToBlocks(html);
                for (String block : blocks) {
                    String text = block.trim();
                    if (text.isEmpty()) {
                        continue;
                    }
                    XWPFParagraph paragraph = document.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.setText(text);
                    run.setFontFamily("SimSun");
                    run.setFontSize(12);
                }

                document.write(fos);
            }

            LogUtil.info("Markdown 转 DOCX 完成: {}", targetPath);
            return "Markdown 已成功转换为 DOCX: " + targetPath;
        });
    }

    /**
     * 包装完整 HTML 文档结构，添加基本样式和中文字体支持
     */
    private String wrapHtmlDocument(String bodyHtml) {
        return "<!DOCTYPE html>\n" + "<html>\n" + "<head>\n" + "<meta charset=\"UTF-8\"/>\n" + "<style>\n"
            + "  body { font-family: 'SimSun', 'Microsoft YaHei', 'WenQuanYi Micro Hei', sans-serif; "
            + "font-size: 14px; line-height: 1.8; margin: 40px; }\n" + "  h1 { font-size: 24px; }\n" + "  h2 { font-size: 20px; }\n"
            + "  h3 { font-size: 16px; }\n" + "  pre { background-color: #f5f5f5; padding: 10px; white-space: pre-wrap; }\n"
            + "  code { background-color: #f5f5f5; padding: 2px 4px; }\n" + "  table { border-collapse: collapse; width: 100%; }\n"
            + "  th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n" + "</style>\n" + "</head>\n" + "<body>\n" + bodyHtml
            + "\n</body>\n" + "</html>";
    }

    /**
     * 将 HTML 内容按块级元素分割为段落文本数组。
     * 替换块级标签的闭合标签为换行符，然后去除所有 HTML 标签，按换行分割。
     */
    private String[] splitHtmlToBlocks(String html) {
        // 先将块级闭合标签替换为换行符
        String processed = html.replaceAll("(?i)</?(?:br|hr)[^>]*/?>", "\n")
                               .replaceAll("(?i)</(?:p|h[1-6]|li|div|pre|blockquote|section|article|table|tr)[^>]*>", "\n");

        // 去除所有 HTML 标签
        String plainText = processed.replaceAll("<[^>]+>", "");

        // 解码常见 HTML 实体
        plainText = plainText.replace("&amp;", "&")
                             .replace("&lt;", "<")
                             .replace("&gt;", ">")
                             .replace("&quot;", "\"")
                             .replace("&nbsp;", " ")
                             .replace("&#39;", "'");

        // 按换行分割，过滤空行
        return plainText.split("\n");
    }
}