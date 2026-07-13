package com.example.mcp.handler.word;

import com.example.mcp.util.LogUtil;
import com.example.mcp.util.PathUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHighlightColor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Word 文档高级操作工具，提供模板创建、关键词搜索高亮、目录提取和默认字体设置功能。
 * 基于 Apache POI 库操作 docx 格式文档。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class WordAdvancedHandler extends WordBaseHandler {

    // --- 1. word_create_from_template ---

    /**
     * 基于模板文档创建新文档，将模板中的占位符 {{key}} 替换为实际值。
     * 支持表格和文本框中的占位符替换。
     *
     * @param templatePath 模板文档的绝对路径（.docx）
     * @param targetPath   输出文档的绝对路径（.docx）
     * @param placeholders 占位符键值对，格式为 "key1=value1,key2=value2"，将 {{key1}} 替换为 value1
     * @return 操作结果消息
     */
    @Tool(name = "word_create_from_template", description = "基于模板文档创建新文档。将模板中的占位符 {{key}} 替换为实际值。")
    public String wordCreateFromTemplate(@ToolParam(description = "模板文档的绝对路径（.docx）") String templatePath,
        @ToolParam(description = "输出文档的绝对路径（.docx）") String targetPath,
        @ToolParam(description = "占位符键值对，格式为 \"key1=value1,key2=value2\"，将 {{key1}} 替换为 value1") String placeholders) {
        return execute("word_create_from_template", () -> {
            Path srcPath = validateDocxFile(templatePath);
            Path tgtPath = Paths.get(targetPath);
            PathUtil.ensureParentDirectory(tgtPath);

            // 解析占位符映射
            Map<String, String> placeholderMap = parsePlaceholders(placeholders);
            if (placeholderMap.isEmpty()) {
                return "错误: 占位符不能为空";
            }

            // 复制模板到目标文件
            Files.copy(srcPath, tgtPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            try (XWPFDocument document = openDocument(tgtPath)) {
                int[] totalReplacements = {0};
                for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
                    String placeholder = "{{" + entry.getKey() + "}}";
                    String value = entry.getValue();
                    forEachRun(document, (run, text) -> {
                        if (text != null && text.contains(placeholder)) {
                            run.setText(text.replace(placeholder, value), 0);
                            totalReplacements[0]++;
                        }
                    });
                }
                saveDocument(document, tgtPath);
                LogUtil.info("wordCreateFromTemplate 完成，共替换 {} 处占位符，输出: {}", totalReplacements[0], targetPath);
                return "模板文档已生成: " + targetPath + "，共替换 " + totalReplacements[0] + " 处占位符";
            }
        });
    }

    /**
     * 解析占位符字符串为 Map
     */
    private Map<String, String> parsePlaceholders(String placeholders) {
        Map<String, String> map = new LinkedHashMap<>();
        if (placeholders == null || placeholders.isBlank()) {
            return map;
        }
        String[] pairs = placeholders.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    // --- 2. word_find_and_highlight ---

    /**
     * 在文档中搜索关键词并对匹配文本设置高亮背景色。
     * 遍历所有段落和表格，对包含关键词的文本设置黄色背景高亮。
     *
     * @param fileAbsolutePath Word 文档的绝对路径
     * @param keyword          要搜索并高亮的关键词
     * @param highlightColor   高亮颜色，支持: yellow(默认), green, cyan, red, blue, magenta, darkYellow, darkGreen, darkCyan, darkRed, darkBlue, darkMagenta, black, gray, white
     * @return 操作结果消息
     */
    @Tool(name = "word_find_and_highlight", description = "在文档中搜索关键词并对匹配文本高亮（设置背景色）。支持多种高亮颜色。")
    public String wordFindAndHighlight(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "要搜索并高亮的关键词") String keyword,
        @ToolParam(description = "高亮颜色，支持: yellow(默认), green, cyan, red, blue, magenta, darkYellow 等", required = false) String highlightColor) {
        return execute("word_find_and_highlight", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            String color = (highlightColor != null && !highlightColor.isBlank()) ? highlightColor.toUpperCase() : "YELLOW";
            STHighlightColor.Enum hlColor = parseHighlightColor(color);

            try (XWPFDocument document = openDocument(path)) {
                int[] totalHighlights = {0};

                forEachRun(document, (run, text) -> {
                    if (text != null && text.contains(keyword)) {
                        setHighlightOnRun(run, hlColor);
                        totalHighlights[0]++;
                    }
                });

                saveDocument(document, path);
                LogUtil.info("wordFindAndHighlight 完成，关键词 '{}'，高亮 {} 处: {}", keyword, totalHighlights[0], fileAbsolutePath);
                return "已在文档中找到关键词 '" + keyword + "' 并高亮 " + totalHighlights[0] + " 处（颜色: " + color + "）: " + fileAbsolutePath;
            }
        });
    }

    /**
     * 设置 run 的高亮颜色
     */
    private void setHighlightOnRun(XWPFRun run, STHighlightColor.Enum color) {
        CTRPr rpr = run.getCTR()
                       .isSetRPr() ? run.getCTR()
                                        .getRPr() : run.getCTR()
                                                       .addNewRPr();
        rpr.addNewHighlight()
           .setVal(color);
    }

    /**
     * 解析高亮颜色字符串
     */
    private STHighlightColor.Enum parseHighlightColor(String color) {
        return switch (color) {
            case "YELLOW" -> STHighlightColor.YELLOW;
            case "GREEN" -> STHighlightColor.GREEN;
            case "CYAN" -> STHighlightColor.CYAN;
            case "RED" -> STHighlightColor.RED;
            case "BLUE" -> STHighlightColor.BLUE;
            case "MAGENTA" -> STHighlightColor.MAGENTA;
            case "DARK_YELLOW", "DARKYELLOW" -> STHighlightColor.DARK_YELLOW;
            case "DARK_GREEN", "DARKGREEN" -> STHighlightColor.DARK_GREEN;
            case "DARK_CYAN", "DARKCYAN" -> STHighlightColor.DARK_CYAN;
            case "DARK_RED", "DARKRED" -> STHighlightColor.DARK_RED;
            case "DARK_BLUE", "DARKBLUE" -> STHighlightColor.DARK_BLUE;
            case "DARK_MAGENTA", "DARKMAGENTA" -> STHighlightColor.DARK_MAGENTA;
            case "BLACK" -> STHighlightColor.BLACK;
            case "GRAY" -> STHighlightColor.DARK_GRAY;
            case "WHITE" -> STHighlightColor.WHITE;
            default -> STHighlightColor.YELLOW;
        };
    }

    // --- 3. word_extract_headings ---

    /**
     * 提取文档中所有标题及其层级，返回文档的目录结构。
     * 遍历所有段落，识别样式为 Heading1~Heading9 的段落，按顺序输出层级目录。
     *
     * @param fileAbsolutePath Word 文档的绝对路径
     * @return 文档的目录结构，格式为每行 "[层级] 标题文本"
     */
    @Tool(name = "word_extract_headings", description = "提取文档中所有标题及其层级，返回文档的目录结构。")
    public String wordExtractHeadings(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath) {
        return execute("word_extract_headings", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                StringBuilder sb = new StringBuilder();
                sb.append("文档目录结构: ")
                  .append(fileAbsolutePath)
                  .append("\n");
                sb.append("========================================\n");

                List<XWPFParagraph> paragraphs = document.getParagraphs();
                int headingCount = 0;
                for (int i = 0; i < paragraphs.size(); i++) {
                    XWPFParagraph paragraph = paragraphs.get(i);
                    String style = paragraph.getStyle();
                    if (style != null && style.startsWith("Heading")) {
                        try {
                            int level = Integer.parseInt(style.replace("Heading", "")
                                                              .trim());
                            if (level < 1) {
                                level = 1;
                            }
                            if (level > 9) {
                                level = 9;
                            }
                            String text = paragraph.getText();
                            if (text != null && !text.isBlank()) {
                                headingCount++;
                                // 构建缩进
                                String indent = "  ".repeat(level - 1);
                                String prefix = "#".repeat(level);
                                sb.append(indent)
                                  .append(prefix)
                                  .append(" ")
                                  .append(text.trim())
                                  .append("\n");
                            }
                        } catch (NumberFormatException e) {
                            // 忽略无法解析层级的标题样式
                        }
                    }
                }

                if (headingCount == 0) {
                    sb.append("（文档中没有找到标题）\n");
                } else {
                    sb.append("========================================\n");
                    sb.append("共提取 ")
                      .append(headingCount)
                      .append(" 个标题\n");
                }

                LogUtil.info("wordExtractHeadings 完成，共提取 {} 个标题: {}", headingCount, fileAbsolutePath);
                return sb.toString()
                         .trim();
            }
        });
    }

    // --- 4. word_set_default_font ---

    /**
     * 设置文档默认字体，用于全文统一字体名称和大小。
     * 支持指定字体名称（如宋体、微软雅黑、Calibri 等）和字体大小，
     * 可选择性应用于全部段落、仅正文或仅标题。
     *
     * @param fileAbsolutePath Word 文档的绝对路径
     * @param fontName         字体名称，如 "宋体"、"微软雅黑"、"Calibri"、"Arial"
     * @param fontSize         字体大小（磅），默认 12
     * @param scope            应用范围: "all"(全部,默认), "body"(仅正文段落), "headings"(仅标题段落)
     * @return 操作结果消息
     */
    @Tool(name = "word_set_default_font", description = "设置文档默认字体（用于全文统一字体，如宋体/微软雅黑/Calibri）。支持指定应用范围。")
    public String wordSetDefaultFont(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "字体名称，如 \"宋体\"、\"微软雅黑\"、\"Calibri\"、\"Arial\"") String fontName,
        @ToolParam(description = "字体大小（磅），默认 12", required = false) Integer fontSize,
        @ToolParam(description = "应用范围: all(全部,默认), body(仅正文段落), headings(仅标题段落)", required = false) String scope) {
        return execute("word_set_default_font", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            int size = (fontSize != null && fontSize > 0) ? fontSize : 12;
            String applyScope = (scope != null && !scope.isBlank()) ? scope.toLowerCase() : "all";

            if (!"all".equals(applyScope) && !"body".equals(applyScope) && !"headings".equals(applyScope)) {
                return "错误: 不支持的应用范围 '" + scope + "'，仅支持 all / body / headings";
            }

            try (XWPFDocument document = openDocument(path)) {
                int updatedParagraphs = 0;
                int updatedRuns = 0;

                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    String style = paragraph.getStyle();
                    boolean isHeading = style != null && style.startsWith("Heading");

                    boolean shouldApply = switch (applyScope) {
                        case "all" -> true;
                        case "body" -> !isHeading;
                        case "headings" -> isHeading;
                        default -> true;
                    };

                    if (shouldApply) {
                        List<XWPFRun> runs = paragraph.getRuns();
                        if (runs != null) {
                            boolean paraUpdated = false;
                            for (XWPFRun run : runs) {
                                String text = run.getText(0);
                                if (text != null && !text.isBlank()) {
                                    run.setFontFamily(fontName);
                                    run.setFontSize(size);
                                    updatedRuns++;
                                    paraUpdated = true;
                                }
                            }
                            if (paraUpdated) {
                                updatedParagraphs++;
                            }
                        }
                    }
                }

                // 也处理表格中的文本
                for (var table : document.getTables()) {
                    for (var row : table.getRows()) {
                        for (var cell : row.getTableCells()) {
                            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                                List<XWPFRun> runs = paragraph.getRuns();
                                if (runs != null) {
                                    for (XWPFRun run : runs) {
                                        String text = run.getText(0);
                                        if (text != null && !text.isBlank()) {
                                            run.setFontFamily(fontName);
                                            run.setFontSize(size);
                                            updatedRuns++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                saveDocument(document, path);
                String scopeDesc = switch (applyScope) {
                    case "all" -> "全部";
                    case "body" -> "正文";
                    case "headings" -> "标题";
                    default -> "全部";
                };
                LogUtil.info("wordSetDefaultFont 完成，范围: {}，字体: {} {}pt，更新段落: {}，更新文本块: {}", applyScope, fontName, size,
                             updatedParagraphs, updatedRuns);
                return "已设置" + scopeDesc + "段落的默认字体为 " + fontName + " " + size + "pt（更新 " + updatedParagraphs + " 个段落，" + updatedRuns
                    + " 个文本块）: " + fileAbsolutePath;
            }
        });
    }
}
