package com.example.mcp.handler.convert;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.util.PathUtil;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
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
public class MarkdownHandler extends BaseHandler {

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
    public String createMdFile(@ToolParam(description = "新 MD 文件的绝对路径") String filePath) {
        return execute("create_md_file", () -> {
            Path path = resolvePath(filePath);
            // 确保父目录存在
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, "", StandardCharsets.UTF_8);
            return "空白 MD 文件已创建: " + path;
        });
    }

    // --- 2. read_md ---

    /**
     * 读取完整 Markdown 原始源码
     */
    @Tool(name = "read_md", description = "读取 .md 文件的完整 Markdown 原始源码内容。")
    public String readMd(@ToolParam(description = "MD 文件的绝对路径") String filePath) {
        return execute("read_md", () -> {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误: 文件不存在 - " + path;
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        });
    }

    // --- 3. append_md ---

    /**
     * 尾部追加内容到 MD 文件
     */
    @Tool(name = "append_md", description = "向 MD 文件末尾追加 Markdown 内容。如果文件不存在则创建。")
    public String appendMd(@ToolParam(description = "MD 文件的绝对路径") String filePath,
        @ToolParam(description = "要追加的 Markdown 内容") String content) {
        return execute("append_md", () -> {
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
        });
    }

    // --- 4. insert_md ---

    /**
     * 指定位置插入 MD 内容
     */
    @Tool(name = "insert_md", description = "在 MD 文件的指定行位置插入 Markdown 内容。行号从 1 开始，内容插入在指定行之前。")
    public String insertMd(@ToolParam(description = "MD 文件的绝对路径") String filePath,
        @ToolParam(description = "插入内容的行号（从 1 开始）") int lineNumber, @ToolParam(description = "要插入的 Markdown 内容") String content) {
        return execute("insert_md", () -> {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误: 文件不存在 - " + path;
            }

            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int ln = lineNumber;
            if (ln < 1) {
                ln = 1;
            }
            if (ln > lines.size()) {
                ln = lines.size() + 1;
            }

            // 在指定行之前插入内容
            lines.add(ln - 1, content);
            // 如果内容不包含换行，确保插入后换行
            if (!content.contains("\n")) {
                // 确保前后都有换行
                if (ln > 1 && !lines.get(ln - 2)
                                    .isEmpty()) {
                    lines.set(ln - 1, "\n" + content);
                }
            }

            Files.writeString(path, String.join("\n", lines), StandardCharsets.UTF_8);
            return "内容已插入到 MD 文件第 " + ln + " 行: " + path;
        });
    }

    // --- 5. md_generate_heading ---

    /**
     * 自动生成 MD 标题
     */
    @Tool(name = "md_generate_heading", description = "生成 Markdown 标题。返回标题文本。用于生成 1-6 级标准 MD 标题。")
    public String mdGenerateHeading(@ToolParam(description = "标题级别（1-6）") int level, @ToolParam(description = "标题文本") String text) {
        if (level < 1 || level > 6) {
            return "错误: 标题级别必须在 1-6 之间";
        }
        return "#".repeat(level) + " " + text;
    }

    // --- 6. md_generate_list ---

    /**
     * 自动生成 MD 有序/无序列表
     */
    @Tool(name = "md_generate_list", description = "根据项目数组生成 Markdown 有序或无序列表。")
    public String mdGenerateList(@ToolParam(description = "列表项，以换行符分隔") String items,
        @ToolParam(description = "是否生成有序列表（true）或无序列表（false，默认）", required = false) Boolean ordered) {
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
    public String mdGenerateCodeBlock(@ToolParam(description = "语法高亮的编程语言（如 java, python, javascript）", required = false) String language,
        @ToolParam(description = "代码内容") String code) {
        String lang = (language != null && !language.isBlank()) ? language : "";
        return "```" + lang + "\n" + code + "\n```";
    }

    // --- 8. md_generate_table ---

    /**
     * 自动生成 MD 数据表格
     */
    @Tool(name = "md_generate_table", description = "生成 Markdown 数据表格。表头以逗号分隔，每行以换行分隔，列以逗号分隔。")
    public String mdGenerateTable(@ToolParam(description = "逗号分隔的表头列名") String headers,
        @ToolParam(description = "表格行，每行换行，列以逗号分隔") String rows) {
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
    public String mdGenerateBlockquote(@ToolParam(description = "引用块文本内容") String text) {
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
    public String modifyMdParagraph(@ToolParam(description = "MD 文件的绝对路径") String filePath,
        @ToolParam(description = "要被替换的文本") String oldText, @ToolParam(description = "替换后的新文本") String newText) {
        return execute("modify_md_paragraph", () -> {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误: 文件不存在 - " + path;
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (!content.contains(oldText)) {
                return "未找到匹配的文本 '" + oldText + "'";
            }

            String modifiedContent = content.replaceFirst(java.util.regex.Pattern.quote(oldText), java.util.regex.Matcher.quoteReplacement(newText));
            Files.writeString(path, modifiedContent, StandardCharsets.UTF_8);
            return "MD 段落修改成功: " + path;
        });
    }

    // --- 11. replace_md_content ---

    /**
     * 全文内容替换
     */
    @Tool(name = "replace_md_content", description = "将 MD 文件中所有匹配的旧文本替换为新文本。")
    public String replaceMdContent(@ToolParam(description = "MD 文件的绝对路径") String filePath,
        @ToolParam(description = "要被替换的文本") String oldText, @ToolParam(description = "替换后的新文本") String newText) {
        return execute("replace_md_content", () -> {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误: 文件不存在 - " + path;
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (!content.contains(oldText)) {
                return "未找到匹配的文本 '" + oldText + "'";
            }

            String modifiedContent = content.replace(oldText, newText);
            Files.writeString(path, modifiedContent, StandardCharsets.UTF_8);
            return "MD 全文替换成功: " + path;
        });
    }

    // --- 12. save_md ---

    /**
     * 保存 MD 文件（覆盖写入）
     */
    @Tool(name = "save_md", description = "将 Markdown 内容保存到文件（覆盖写入）。如果文件不存在则创建。")
    public String saveMd(@ToolParam(description = "MD 文件的绝对路径") String filePath,
        @ToolParam(description = "要保存的 Markdown 内容") String content) {
        return execute("save_md", () -> {
            Path path = resolvePath(filePath);
            // 确保父目录存在
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return "MD 文件已保存: " + path;
        });
    }

    // --- 13. save_md_as ---

    /**
     * 另存为新 MD 文件
     */
    @Tool(name = "save_md_as", description = "将 Markdown 文件另存为新文件。将源文件复制到新位置。")
    public String saveMdAs(@ToolParam(description = "源 MD 文件的绝对路径") String sourcePath,
        @ToolParam(description = "新 MD 文件的绝对路径") String targetPath) {
        return execute("save_md_as", () -> {
            Path source = resolvePath(sourcePath);
            if (!Files.exists(source)) {
                return "错误: 源文件不存在 - " + source;
            }
            Path target = resolvePath(targetPath);
            // 确保目标父目录存在
            Path targetParent = target.getParent();
            if (targetParent != null && !Files.exists(targetParent)) {
                Files.createDirectories(targetParent);
            }
            Files.copy(source, target);
            return "MD 文件已另存为: " + targetPath;
        });
    }
}