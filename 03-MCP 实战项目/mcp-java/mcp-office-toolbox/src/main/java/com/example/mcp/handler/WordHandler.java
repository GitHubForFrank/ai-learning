package com.example.mcp.handler;

import com.example.mcp.util.FileValidateUtil;
import com.example.mcp.util.PathUtil;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * MCP Word 文档（docx）工具实现，提供 Word 文档的创建、读取、写入、修改和保存功能。
 * 支持插入文字、标题、段落，以及内容修改和关键词批量替换。
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@Service
public class WordHandler {

    private static final Logger log = LoggerFactory.getLogger(WordHandler.class);

    /**
     * 校验 Word 文件路径
     */
    private Path validateDocxFile(String fileAbsolutePath) {
        return FileValidateUtil.validateFile(fileAbsolutePath, ".docx");
    }

    /**
     * 打开 Word 文档
     */
    private XWPFDocument openDocument(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath)) {
            return new XWPFDocument(is);
        }
    }

    /**
     * 保存 Word 文档
     */
    private void saveDocument(XWPFDocument document, Path filePath) throws IOException {
        try (OutputStream os = Files.newOutputStream(filePath)) {
            document.write(os);
        }
    }

    // --- 1. create_word_doc ---

    /**
     * 新建空白 Word 文档
     */
    @Tool(name = "create_word_doc", description = "创建新的空白 Word 文档。如果文件已存在则覆盖。")
    public String createWordDoc(
            @ToolParam(description = "Absolute path for the new Word document") String fileAbsolutePath) {
        try {
            Path path = Paths.get(fileAbsolutePath);
            // 确保父目录存在
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (XWPFDocument document = new XWPFDocument()) {
                saveDocument(document, path);
            }
            return "空白 Word 文档已创建: " + fileAbsolutePath;
        } catch (Exception e) {
            log.error("createWordDoc 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 2. read_word_full ---

    /**
     * 全文读取 Word 文档内容
     */
    @Tool(name = "read_word_full", description = "读取 Word 文档的全文内容。")
    public String readWordFull(
            @ToolParam(description = "Absolute path to the Word document") String fileAbsolutePath) {
        try {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    String text = paragraph.getText();
                    if (text != null && !text.isBlank()) {
                        sb.append(text).append("\n");
                    }
                }
                return sb.toString().trim();
            }
        } catch (Exception e) {
            log.error("readWordFull 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 3. read_word_paragraphs ---

    /**
     * 分段读取 Word 文档段落内容
     */
    @Tool(name = "read_word_paragraphs", description = "逐段读取 Word 文档内容。每个段落返回其索引和样式信息。")
    public String readWordParagraphs(
            @ToolParam(description = "Absolute path to the Word document") String fileAbsolutePath) {
        try {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                StringBuilder sb = new StringBuilder();
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                for (int i = 0; i < paragraphs.size(); i++) {
                    XWPFParagraph paragraph = paragraphs.get(i);
                    String text = paragraph.getText();
                    String style = paragraph.getStyle() != null ? paragraph.getStyle() : "Normal";
                    // 获取段落对齐方式
                    String alignment = paragraph.getAlignment().name();
                    sb.append(String.format("[段落 %d] 样式=%s, 对齐=%s\n", i, style, alignment));
                    sb.append(text).append("\n\n");
                }
                return sb.toString().trim();
            }
        } catch (Exception e) {
            log.error("readWordParagraphs 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 4. insert_word_text ---

    /**
     * 在 Word 文档末尾插入文字
     */
    @Tool(name = "insert_word_text", description = "在 Word 文档末尾插入文字。")
    public String insertWordText(
            @ToolParam(description = "Absolute path to the Word document") String fileAbsolutePath,
            @ToolParam(description = "Text content to insert") String text) {
        try {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(text);
                saveDocument(document, path);
            }
            return "文字已插入到 Word 文档末尾: " + fileAbsolutePath;
        } catch (Exception e) {
            log.error("insertWordText 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 5. insert_word_heading ---

    /**
     * 在 Word 文档末尾插入标题
     */
    @Tool(name = "insert_word_heading", description = "在 Word 文档末尾插入标题。级别 1 为主标题，级别 2 为副标题，以此类推。")
    public String insertWordHeading(
            @ToolParam(description = "Absolute path to the Word document") String fileAbsolutePath,
            @ToolParam(description = "Heading level (1-9)") int level,
            @ToolParam(description = "Heading text") String text) {
        try {
            Path path = validateDocxFile(fileAbsolutePath);
            if (level < 1 || level > 9) {
                return "错误：标题级别必须在 1-9 之间";
            }
            try (XWPFDocument document = openDocument(path)) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.setStyle("Heading" + level);
                XWPFRun run = paragraph.createRun();
                run.setText(text);
                run.setBold(true);
                saveDocument(document, path);
            }
            return "标题（级别 " + level + "）已插入到 Word 文档末尾: " + fileAbsolutePath;
        } catch (Exception e) {
            log.error("insertWordHeading 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 6. insert_word_paragraph ---

    /**
     * 在 Word 文档末尾插入自定义段落
     */
    @Tool(description = "在 Word 文档末尾插入自定义段落。支持可选的粗体、斜体、字号和颜色设置。")
    public String insertWordParagraph(
            @ToolParam(description = "Absolute path to the Word document") String fileAbsolutePath,
            @ToolParam(description = "Paragraph text content") String text,
            @ToolParam(description = "Whether the text is bold", required = false) Boolean bold,
            @ToolParam(description = "Whether the text is italic", required = false) Boolean italic,
            @ToolParam(description = "Font size in points", required = false) Integer fontSize,
            @ToolParam(description = "Font color in hex format (e.g., #FF0000)", required = false) String color) {
        try {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(text);
                if (bold != null && bold)
                    run.setBold(true);
                if (italic != null && italic)
                    run.setItalic(true);
                if (fontSize != null)
                    run.setFontSize(fontSize);
                if (color != null && !color.isBlank()) {
                    run.setColor(color.replace("#", ""));
                }
                saveDocument(document, path);
            }
            return "自定义段落已插入到 Word 文档末尾: " + fileAbsolutePath;
        } catch (Exception e) {
            log.error("insertWordParagraph 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 7. modify_word_content ---

    /**
     * 修改 Word 文档正文内容（关键词替换）
     */
    @Tool(name = "modify_word_content", description = "通过替换文本修改 Word 文档内容。在所有段落中搜索旧文本并替换为新文本。")
    public String modifyWordContent(
            @ToolParam(description = "Absolute path to the Word document") String fileAbsolutePath,
            @ToolParam(description = "Text to be replaced") String oldText,
            @ToolParam(description = "New text to replace with") String newText) {
        try {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                int replaceCount = 0;
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    List<XWPFRun> runs = paragraph.getRuns();
                    if (runs != null) {
                        for (XWPFRun run : runs) {
                            String text = run.getText(0);
                            if (text != null && text.contains(oldText)) {
                                run.setText(text.replace(oldText, newText), 0);
                                replaceCount++;
                            }
                        }
                    }
                }
                saveDocument(document, path);
                return "内容修改完成: " + fileAbsolutePath + "，共替换 " + replaceCount + " 处";
            }
        } catch (Exception e) {
            log.error("modifyWordContent 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 8. replace_word_keywords ---

    /**
     * 关键词批量替换
     */
    @Tool(name = "replace_word_keywords", description = "批量替换 Word 文档中的多个关键词。替换参数为 JSON 格式的键值对映射。")
    public String replaceWordKeywords(
            @ToolParam(description = "Absolute path to the Word document") String fileAbsolutePath,
            @ToolParam(description = "Map of old text to new text replacements, e.g. {\"old1\":\"new1\",\"old2\":\"new2\"}") String replacements) {
        try {
            Path path = validateDocxFile(fileAbsolutePath);
            // 解析替换映射
            Map<String, String> replaceMap = new LinkedHashMap<>();
            String trimmed = replacements.trim();
            if (trimmed.startsWith("{")) {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            String[] pairs = trimmed.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    replaceMap.put(key, value);
                }
            }

            try (XWPFDocument document = openDocument(path)) {
                int totalReplaceCount = 0;
                for (Map.Entry<String, String> entry : replaceMap.entrySet()) {
                    String oldText = entry.getKey();
                    String newText = entry.getValue();
                    for (XWPFParagraph paragraph : document.getParagraphs()) {
                        List<XWPFRun> runs = paragraph.getRuns();
                        if (runs != null) {
                            for (XWPFRun run : runs) {
                                String text = run.getText(0);
                                if (text != null && text.contains(oldText)) {
                                    run.setText(text.replace(oldText, newText), 0);
                                    totalReplaceCount++;
                                }
                            }
                        }
                    }
                }
                saveDocument(document, path);
                return "批量替换完成: " + fileAbsolutePath + "，共替换 " + totalReplaceCount + " 处";
            }
        } catch (Exception e) {
            log.error("replaceWordKeywords 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 9. save_word_as ---

    /**
     * 文档另存为新 docx 文件
     */
    @Tool(name = "save_word_as", description = "将 Word 文档另存为新文件。将原始文档复制到新位置。")
    public String saveWordAs(
            @ToolParam(description = "Absolute path to the source Word document") String sourcePath,
            @ToolParam(description = "Absolute path for the new Word document") String targetPath) {
        try {
            Path source = validateDocxFile(sourcePath);
            Path target = Paths.get(targetPath);
            // 确保目标父目录存在
            Path targetParent = target.getParent();
            if (targetParent != null && !Files.exists(targetParent)) {
                Files.createDirectories(targetParent);
            }
            Files.copy(source, target);
            return "文档已另存为: " + targetPath;
        } catch (Exception e) {
            log.error("saveWordAs 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }
}