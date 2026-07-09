package com.example.mcp.handler;

import com.example.mcp.util.PathUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Markdown 文件专属操作工具实现。
 * 提供 MD 文件的创建、读取、追加、插入、组件生成、修改和保存功能。
 * 场景适配：AI 文档撰写、笔记记录、日志归档。
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@Service
public class MarkdownHandler {

    private static final Logger log = LoggerFactory.getLogger(MarkdownHandler.class);

    /**
     * 解析文件路径
     */
    private Path resolvePath(String filePath) {
        return PathUtil.resolvePath(filePath);
    }

    // --- 1. create_md_file ---

    /**
     * 新建空白 MD 文件
     */
    @Tool(name = "create_md_file", description = "创建新的空白 Markdown 文件。如果文件已存在则覆盖为空文件。")
    public String createMdFile(@ToolParam(description = "Absolute path for the new MD file") String filePath) {
        try {
            Path path = resolvePath(filePath);
            // 确保父目录存在
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, "", StandardCharsets.UTF_8);
            return "空白 MD 文件已创建: " + path;
        } catch (Exception e) {
            log.error("createMdFile 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 2. read_md ---

    /**
     * 读取完整 Markdown 原始源码
     */
    @Tool(name = "read_md", description = "读取 .md 文件的完整 Markdown 原始源码内容。")
    public String readMd(@ToolParam(description = "Absolute path to the MD file") String filePath) {
        try {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + path;
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("readMd 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 3. append_md ---

    /**
     * 尾部追加内容到 MD 文件
     */
    @Tool(name = "append_md", description = "向 MD 文件末尾追加 Markdown 内容。如果文件不存在则创建。")
    public String appendMd(@ToolParam(description = "Absolute path to the MD file") String filePath,
        @ToolParam(description = "Markdown content to append") String content) {
        try {
            Path path = resolvePath(filePath);
            // 确保父目录存在
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            // 确保内容前面有换行
            String contentToAppend = content;
            if (Files.exists(path) && Files.size(path) > 0) {
                String existing = Files.readString(path, StandardCharsets.UTF_8);
                if (!existing.endsWith("\n")) {
                    contentToAppend = "\n" + content;
                }
            }

            if (Files.exists(path)) {
                Files.writeString(path, contentToAppend, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } else {
                Files.writeString(path, contentToAppend, StandardCharsets.UTF_8);
            }
            return "内容已追加到 MD 文件: " + path;
        } catch (Exception e) {
            log.error("appendMd 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 4. insert_md ---

    /**
     * 指定位置插入 MD 内容
     */
    @Tool(name = "insert_md", description = "在 MD 文件的指定行位置插入 Markdown 内容。行号从 1 开始，内容插入在指定行之前。")
    public String insertMd(@ToolParam(description = "Absolute path to the MD file") String filePath,
        @ToolParam(description = "Line number where to insert content (1-based)") int lineNumber,
        @ToolParam(description = "Markdown content to insert") String content) {
        try {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + path;
            }

            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (lineNumber < 1) {
                lineNumber = 1;
            }
            if (lineNumber > lines.size()) {
                lineNumber = lines.size() + 1;
            }

            // 在指定行之前插入内容
            lines.add(lineNumber - 1, content);
            // 如果内容不包含换行，确保插入后换行
            if (!content.contains("\n")) {
                // 确保前后都有换行
                if (lineNumber > 1 && !lines.get(lineNumber - 2)
                                            .isEmpty()) {
                    lines.set(lineNumber - 1, "\n" + content);
                }
            }

            Files.writeString(path, String.join("\n", lines), StandardCharsets.UTF_8);
            return "内容已插入到 MD 文件第 " + lineNumber + " 行: " + path;
        } catch (Exception e) {
            log.error("insertMd 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 5. md_generate_heading ---

    /**
     * 自动生成 MD 标题
     */
    @Tool(name = "md_generate_heading", description = "生成 Markdown 标题。返回标题文本。用于生成 1-6 级标准 MD 标题。")
    public String mdGenerateHeading(@ToolParam(description = "Heading level (1-6)") int level, @ToolParam(description = "Heading text") String text) {
        if (level < 1 || level > 6) {
            return "错误：标题级别必须在 1-6 之间";
        }
        return "#".repeat(level) + " " + text;
    }

    // --- 6. md_generate_list ---

    /**
     * 自动生成 MD 有序/无序列表
     */
    @Tool(name = "md_generate_list", description = "根据项目数组生成 Markdown 有序或无序列表。")
    public String mdGenerateList(@ToolParam(description = "List items, separated by newline") String items,
        @ToolParam(description = "Whether to generate an ordered list (true) or unordered list (false, default)", required = false) Boolean ordered) {
        String[] itemArray = items.split("\n");
        StringBuilder sb = new StringBuilder();
        boolean isOrdered = ordered != null && ordered;
        for (int i = 0; i < itemArray.length; i++) {
            String item = itemArray[i].trim();
            if (item.isEmpty()) {
                continue;
            }
            if (isOrdered) {
                sb.append((i + 1))
                  .append(". ")
                  .append(item)
                  .append("\n");
            } else {
                sb.append("- ")
                  .append(item)
                  .append("\n");
            }
        }
        return sb.toString()
                 .trim();
    }

    // --- 7. md_generate_code_block ---

    /**
     * 自动生成 MD 代码块
     */
    @Tool(name = "md_generate_code_block", description = "生成 Markdown 代码块，可选指定语言类型。")
    public String mdGenerateCodeBlock(
        @ToolParam(description = "Programming language for syntax highlighting (e.g., java, python, javascript)", required = false) String language,
        @ToolParam(description = "Code content") String code) {
        String lang = (language != null && !language.isBlank()) ? language : "";
        return "```" + lang + "\n" + code + "\n```";
    }

    // --- 8. md_generate_table ---

    /**
     * 自动生成 MD 数据表格
     */
    @Tool(name = "md_generate_table", description = "生成 Markdown 数据表格。表头以逗号分隔，每行以换行分隔，列以逗号分隔。")
    public String mdGenerateTable(@ToolParam(description = "Comma-separated header column names") String headers,
        @ToolParam(description = "Table rows, each row on a new line, columns separated by commas") String rows) {
        String[] headerCols = headers.split(",");
        StringBuilder sb = new StringBuilder();

        // 表头
        sb.append("|");
        for (String col : headerCols) {
            sb.append(" ")
              .append(col.trim())
              .append(" |");
        }
        sb.append("\n");

        // 分隔行
        sb.append("|");
        for (int i = 0; i < headerCols.length; i++) {
            sb.append(" --- |");
        }
        sb.append("\n");

        // 数据行
        String[] rowArray = rows.split("\n");
        for (String row : rowArray) {
            String[] cols = row.split(",");
            sb.append("|");
            for (String col : cols) {
                sb.append(" ")
                  .append(col.trim())
                  .append(" |");
            }
            sb.append("\n");
        }

        return sb.toString()
                 .trim();
    }

    // --- 9. md_generate_blockquote ---

    /**
     * 自动生成 MD 引用块
     */
    @Tool(name = "md_generate_blockquote", description = "生成 Markdown 引用块。")
    public String mdGenerateBlockquote(@ToolParam(description = "Text content for the blockquote") String text) {
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append("> ")
              .append(line)
              .append("\n");
        }
        return sb.toString()
                 .trim();
    }

    // --- 10. modify_md_paragraph ---

    /**
     * 指定段落修改
     */
    @Tool(name = "modify_md_paragraph", description = "替换 MD 文件中指定段落的文本。仅替换第一个匹配到的旧文本。")
    public String modifyMdParagraph(@ToolParam(description = "Absolute path to the MD file") String filePath,
        @ToolParam(description = "Text to be replaced") String oldText, @ToolParam(description = "New text to replace with") String newText) {
        try {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + path;
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (!content.contains(oldText)) {
                return "未找到匹配的文本 '" + oldText + "'";
            }

            String modifiedContent = content.replaceFirst(java.util.regex.Pattern.quote(oldText), java.util.regex.Matcher.quoteReplacement(newText));
            Files.writeString(path, modifiedContent, StandardCharsets.UTF_8);
            return "MD 段落修改成功: " + path;
        } catch (Exception e) {
            log.error("modifyMdParagraph 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 11. replace_md_content ---

    /**
     * 全文内容替换
     */
    @Tool(name = "replace_md_content", description = "将 MD 文件中所有匹配的旧文本替换为新文本。")
    public String replaceMdContent(@ToolParam(description = "Absolute path to the MD file") String filePath,
        @ToolParam(description = "Text to be replaced") String oldText, @ToolParam(description = "New text to replace with") String newText) {
        try {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + path;
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (!content.contains(oldText)) {
                return "未找到匹配的文本 '" + oldText + "'";
            }

            String modifiedContent = content.replace(oldText, newText);
            Files.writeString(path, modifiedContent, StandardCharsets.UTF_8);
            return "MD 全文替换成功: " + path;
        } catch (Exception e) {
            log.error("replaceMdContent 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 12. save_md ---

    /**
     * 保存 MD 文件（覆盖写入）
     */
    @Tool(name = "save_md", description = "将 Markdown 内容保存到文件（覆盖写入）。如果文件不存在则创建。")
    public String saveMd(@ToolParam(description = "Absolute path to the MD file") String filePath,
        @ToolParam(description = "Markdown content to save") String content) {
        try {
            Path path = resolvePath(filePath);
            // 确保父目录存在
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return "MD 文件已保存: " + path;
        } catch (Exception e) {
            log.error("saveMd 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 13. save_md_as ---

    /**
     * 另存为新 MD 文件
     */
    @Tool(name = "save_md_as", description = "将 Markdown 文件另存为新文件。将源文件复制到新位置。")
    public String saveMdAs(@ToolParam(description = "Absolute path to the source MD file") String sourcePath,
        @ToolParam(description = "Absolute path for the new MD file") String targetPath) {
        try {
            Path source = resolvePath(sourcePath);
            if (!Files.exists(source)) {
                return "错误：源文件不存在 - " + source;
            }
            Path target = resolvePath(targetPath);
            // 确保目标父目录存在
            Path targetParent = target.getParent();
            if (targetParent != null && !Files.exists(targetParent)) {
                Files.createDirectories(targetParent);
            }
            Files.copy(source, target);
            return "MD 文件已另存为: " + targetPath;
        } catch (Exception e) {
            log.error("saveMdAs 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }
}