package com.example.mcp.handler;

import com.example.mcp.util.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * MCP TXT 纯文本文件专属操作工具实现。
 * 提供 TXT 文件的创建、读取（全文/按行/指定行）、写入（覆盖/追加）、清空、查找和替换功能。
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@Service
public class TxtHandler {

    private static final Logger log = LoggerFactory.getLogger(TxtHandler.class);

    /**
     * 解析文件路径
     */
    private Path resolvePath(String filePath) {
        return PathUtil.resolvePath(filePath);
    }

    // --- 1. create_txt_file ---

    /**
     * 新建空白 TXT 文件
     */
    @Tool(name = "create_txt_file", description = "创建新的空白 TXT 文件。如果文件已存在则覆盖为空文件。")
    public String createTxtFile(
            @ToolParam(description = "Absolute path for the new TXT file") String filePath) {
        try {
            Path path = resolvePath(filePath);
            // 确保父目录存在
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, "", StandardCharsets.UTF_8);
            return "空白 TXT 文件已创建: " + path;
        } catch (Exception e) {
            log.error("createTxtFile 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 2. read_txt_full ---

    /**
     * 全文读取 TXT 文件
     */
    @Tool(name = "read_txt_full", description = "读取 TXT 文件的全文内容。")
    public String readTxtFull(
            @ToolParam(description = "Absolute path to the TXT file") String filePath) {
        try {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + path;
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("readTxtFull 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 3. read_txt_lines ---

    /**
     * 按行读取 TXT 文件
     */
    @Tool(name = "read_txt_lines", description = "按行读取 TXT 文件。返回所有行或指定范围的行。")
    public String readTxtLines(
            @ToolParam(description = "Absolute path to the TXT file") String filePath,
            @ToolParam(description = "Start line number (1-based, inclusive). Default: 1", required = false) Integer startLine,
            @ToolParam(description = "End line number (1-based, inclusive). Default: last line", required = false) Integer endLine) {
        try {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + path;
            }

            List<String> allLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            int start = (startLine != null && startLine > 0) ? startLine - 1 : 0;
            int end = (endLine != null && endLine > 0) ? Math.min(endLine, allLines.size()) : allLines.size();

            if (start >= allLines.size()) {
                return "错误：起始行 " + (start + 1) + " 超出文件总行数 " + allLines.size();
            }

            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                sb.append(String.format("第 %d 行: ", i + 1)).append(allLines.get(i)).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("readTxtLines 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 4. read_txt_specific_lines ---

    /**
     * 读取指定行号列表的内容
     */
    @Tool(name = "read_txt_specific_lines", description = "从 TXT 文件中读取指定行号的内容。行号从 1 开始。")
    public String readTxtSpecificLines(
            @ToolParam(description = "Absolute path to the TXT file") String filePath,
            @ToolParam(description = "Comma-separated line numbers to read, e.g., \"1,3,5\"") String lineNumbers) {
        try {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + path;
            }

            List<String> allLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String[] parts = lineNumbers.split(",");
            List<Integer> lineNums = new ArrayList<>();
            for (String part : parts) {
                try {
                    lineNums.add(Integer.parseInt(part.trim()));
                } catch (NumberFormatException ignored) {
                }
            }

            StringBuilder sb = new StringBuilder();
            for (int lineNum : lineNums) {
                if (lineNum >= 1 && lineNum <= allLines.size()) {
                    sb.append(String.format("第 %d 行: ", lineNum)).append(allLines.get(lineNum - 1)).append("\n");
                } else {
                    sb.append(String.format("第 %d 行: [超出范围]\n", lineNum));
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("readTxtSpecificLines 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 5. write_txt ---

    /**
     * 覆盖写入 TXT 文件
     */
    @Tool(name = "write_txt", description = "将文本内容写入 TXT 文件（覆盖写入）。如果文件不存在则创建。")
    public String writeTxt(
            @ToolParam(description = "Absolute path to the TXT file") String filePath,
            @ToolParam(description = "Text content to write") String content) {
        try {
            Path path = resolvePath(filePath);
            // 确保父目录存在
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return "TXT 文件写入成功: " + path;
        } catch (Exception e) {
            log.error("writeTxt 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 6. append_txt ---

    /**
     * 尾部追加写入 TXT 文件
     */
    @Tool(name = "append_txt", description = "向 TXT 文件末尾追加文本内容。如果文件不存在则创建。")
    public String appendTxt(
            @ToolParam(description = "Absolute path to the TXT file") String filePath,
            @ToolParam(description = "Text content to append") String content) {
        try {
            Path path = resolvePath(filePath);
            // 确保父目录存在
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            if (Files.exists(path)) {
                Files.writeString(path, content, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } else {
                Files.writeString(path, content, StandardCharsets.UTF_8);
            }
            return "内容已追加到 TXT 文件: " + path;
        } catch (Exception e) {
            log.error("appendTxt 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 7. clear_txt ---

    /**
     * 一键清空 TXT 文件全部内容
     */
    @Tool(name = "clear_txt", description = "清空 TXT 文件的全部内容。")
    public String clearTxt(
            @ToolParam(description = "Absolute path to the TXT file") String filePath) {
        try {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + path;
            }
            Files.writeString(path, "", StandardCharsets.UTF_8);
            return "TXT 文件内容已清空: " + path;
        } catch (Exception e) {
            log.error("clearTxt 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 8. search_txt ---

    /**
     * 文本内容关键词查找
     */
    @Tool(name = "search_txt", description = "在 TXT 文件中搜索关键词。返回匹配行及其行号。")
    public String searchTxt(
            @ToolParam(description = "Absolute path to the TXT file") String filePath,
            @ToolParam(description = "Keyword to search for") String keyword,
            @ToolParam(description = "Case insensitive search", required = false) Boolean caseInsensitive) {
        try {
            Path path = resolvePath(filePath);
            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + path;
            }

            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            boolean ci = caseInsensitive != null && caseInsensitive;
            String searchKeyword = ci ? keyword.toLowerCase() : keyword;

            StringBuilder sb = new StringBuilder();
            sb.append("搜索关键词 '").append(keyword).append("' 在文件 ").append(path).append(" 中的结果：\n");
            int matchCount = 0;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String compareLine = ci ? line.toLowerCase() : line;
                if (compareLine.contains(searchKeyword)) {
                    sb.append("  第 ").append(i + 1).append(" 行: ").append(line).append("\n");
                    matchCount++;
                }
            }
            if (matchCount == 0) {
                sb.append("  未找到匹配项\n");
            } else {
                sb.append("共找到 ").append(matchCount).append(" 处匹配\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("searchTxt 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 9. replace_txt ---

    /**
     * 简单文本替换
     */
    @Tool(name = "replace_txt", description = "将 TXT 文件中所有匹配的旧文本替换为新文本。")
    public String replaceTxt(
            @ToolParam(description = "Absolute path to the TXT file") String filePath,
            @ToolParam(description = "Text to be replaced") String oldText,
            @ToolParam(description = "New text to replace with") String newText) {
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
            return "TXT 文件替换成功: " + path;
        } catch (Exception e) {
            log.error("replaceTxt 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }
}