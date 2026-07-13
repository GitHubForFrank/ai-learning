package com.example.mcp.handler.word;

import com.alibaba.fastjson2.JSON;
import com.example.mcp.util.LogUtil;
import com.example.mcp.util.PathUtil;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFldChar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Word 文档（docx）工具实现，提供 Word 文档的创建、读取、写入、修改和保存功能。
 * 支持插入文字、标题、段落，以及内容修改和关键词批量替换。
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@Service
public class WordHandler extends WordBaseHandler {

    // --- 1. create_word_doc ---

    /**
     * 新建空白 Word 文档
     */
    @Tool(name = "create_word_doc", description = "创建新的空白 Word 文档。如果文件已存在则覆盖。")
    public String createWordDoc(@ToolParam(description = "新 Word 文档的绝对路径") String fileAbsolutePath) {
        return execute("create_word_doc", () -> {
            Path path = Paths.get(fileAbsolutePath);
            PathUtil.ensureParentDirectory(path);
            try (XWPFDocument document = new XWPFDocument()) {
                saveDocument(document, path);
            }
            return "空白 Word 文档已创建: " + fileAbsolutePath;
        });
    }

    // --- 2. read_word_full ---

    /**
     * 全文读取 Word 文档内容
     */
    @Tool(name = "read_word_full", description = "读取 Word 文档的全文内容。")
    public String readWordFull(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath) {
        return execute("read_word_full", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    String text = paragraph.getText();
                    if (text != null && !text.isBlank()) {
                        sb.append(text)
                          .append("\n");
                    }
                }
                return sb.toString()
                         .trim();
            }
        });
    }

    // --- 3. read_word_paragraphs ---

    /**
     * 分段读取 Word 文档段落内容
     */
    @Tool(name = "read_word_paragraphs", description = "逐段读取 Word 文档内容。每个段落返回其索引和样式信息。")
    public String readWordParagraphs(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath) {
        return execute("read_word_paragraphs", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                StringBuilder sb = new StringBuilder();
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                for (int i = 0; i < paragraphs.size(); i++) {
                    XWPFParagraph paragraph = paragraphs.get(i);
                    String text = paragraph.getText();
                    String style = paragraph.getStyle() != null ? paragraph.getStyle() : "Normal";
                    // 获取段落对齐方式
                    String alignment = paragraph.getAlignment() != null ? paragraph.getAlignment()
                                                                                   .name() : "LEFT";
                    sb.append(String.format("[段落 %d] 样式=%s, 对齐=%s\n", i, style, alignment));
                    sb.append(text)
                      .append("\n\n");
                }
                return sb.toString()
                         .trim();
            }
        });
    }

    // --- 4. insert_word_text ---

    /**
     * 在 Word 文档末尾插入文字
     */
    @Tool(name = "insert_word_text", description = "在 Word 文档末尾插入文字。")
    public String insertWordText(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "要插入的文本内容") String text) {
        return execute("insert_word_text", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(text);
                saveDocument(document, path);
            }
            return "文字已插入到 Word 文档末尾: " + fileAbsolutePath;
        });
    }

    // --- 5. insert_word_heading ---

    /**
     * 在 Word 文档末尾插入标题
     */
    @Tool(name = "insert_word_heading", description = "在 Word 文档末尾插入标题。级别 1 为主标题，级别 2 为副标题，以此类推。")
    public String insertWordHeading(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "标题级别（1-9）") int level, @ToolParam(description = "标题文本") String text) {
        return execute("insert_word_heading", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            if (level < 1 || level > 9) {
                return "错误: 标题级别必须在 1-9 之间";
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
        });
    }

    // --- 6. insert_word_paragraph ---

    /**
     * 在 Word 文档末尾插入自定义段落
     */
    @Tool(name = "insert_word_paragraph", description = "在 Word 文档末尾插入自定义段落。支持可选的粗体、斜体、字号和颜色设置。")
    public String insertWordParagraph(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "段落文本内容") String text, @ToolParam(description = "是否加粗", required = false) Boolean bold,
        @ToolParam(description = "是否斜体", required = false) Boolean italic,
        @ToolParam(description = "字号（磅）", required = false) Integer fontSize,
        @ToolParam(description = "字体颜色，十六进制格式（如 #FF0000）", required = false) String color) {
        return execute("insert_word_paragraph", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(text);
                if (bold != null && bold) {
                    run.setBold(true);
                }
                if (italic != null && italic) {
                    run.setItalic(true);
                }
                if (fontSize != null) {
                    run.setFontSize(fontSize);
                }
                if (color != null && !color.isBlank()) {
                    run.setColor(color.replace("#", ""));
                }
                saveDocument(document, path);
            }
            return "自定义段落已插入到 Word 文档末尾: " + fileAbsolutePath;
        });
    }

    // --- 7. modify_word_content ---

    /**
     * 修改 Word 文档正文内容（关键词替换）
     */
    @Tool(name = "modify_word_content", description = "通过替换文本修改 Word 文档内容。在所有段落中搜索旧文本并替换为新文本。")
    public String modifyWordContent(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "要被替换的文本") String oldText, @ToolParam(description = "替换文本") String newText) {
        return execute("modify_word_content", () -> {
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
        });
    }

    // --- 8. replace_word_keywords ---

    /**
     * 关键词批量替换
     */
    @Tool(name = "replace_word_keywords", description = "批量替换 Word 文档中的多个关键词。替换参数为 JSON 格式的键值对映射。")
    public String replaceWordKeywords(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "替换映射，JSON 格式，如 {\"旧文本1\":\"新文本1\",\"旧文本2\":\"新文本2\"}") String replacements) {
        return execute("replace_word_keywords", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            // 解析替换映射
            Map<String, String> replaceMap = new LinkedHashMap<>();
            com.alibaba.fastjson2.JSONObject jsonObj = JSON.parseObject(replacements);
            for (String key : jsonObj.keySet()) {
                replaceMap.put(key, jsonObj.getString(key));
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
        });
    }

    // --- 9. save_word_as ---

    /**
     * 文档另存为新 docx 文件
     */
    @Tool(name = "save_word_as", description = "将 Word 文档另存为新文件。将原始文档复制到新位置。")
    public String saveWordAs(@ToolParam(description = "源 Word 文档的绝对路径") String sourcePath,
        @ToolParam(description = "新 Word 文档的绝对路径") String targetPath) {
        return execute("save_word_as", () -> {
            Path source = validateDocxFile(sourcePath);
            Path target = Paths.get(targetPath);
            PathUtil.ensureParentDirectory(target);
            Files.copy(source, target);
            return "文档已另存为: " + targetPath;
        });
    }

    // ==================== P0: 基础操作补齐 ====================

    // --- 10. insert_word_table ---

    @Tool(name = "insert_word_table", description = "在 Word 文档末尾插入表格。rows 为表格数据，第一行作为表头，每行数据用逗号分隔。")
    public String insertWordTable(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "表格数据，每行用换行分隔，列用逗号分隔。第一行为表头。") String rows) {
        return execute("insert_word_table", () -> {
            if (rows == null || rows.isEmpty()) {
                throw new IllegalArgumentException("rows 参数不能为空，请提供表格数据");
            }
            Path path = validateDocxFile(fileAbsolutePath);
            String[] rowLines = rows.split("\n");
            if (rowLines.length == 0) {
                return "错误: 表格数据不能为空";
            }
            String[] headerCols = rowLines[0].split(",");
            try (XWPFDocument document = openDocument(path)) {
                XWPFTable table = document.createTable(rowLines.length, headerCols.length);
                XWPFTableRow headerRow = table.getRow(0);
                for (int c = 0; c < headerCols.length; c++) {
                    XWPFTableCell cell = headerRow.getCell(c);
                    cell.setText(headerCols[c].trim());
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        for (XWPFRun run : p.getRuns()) {
                            run.setBold(true);
                        }
                    }
                }
                for (int r = 1; r < rowLines.length; r++) {
                    String[] cells = rowLines[r].split(",", -1);
                    XWPFTableRow row = table.getRow(r);
                    for (int c = 0; c < headerCols.length && c < cells.length; c++) {
                        XWPFTableCell cell = row.getCell(c);
                        cell.setText(cells[c].trim());
                    }
                }
                saveDocument(document, path);
                return "表格（" + (rowLines.length - 1) + " 行 × " + headerCols.length + " 列）已插入到 Word 文档末尾: " + fileAbsolutePath;
            }
        });
    }

    // --- 11. read_word_tables ---

    @Tool(name = "read_word_tables", description = "读取 Word 文档中所有表格的数据。")
    public String readWordTables(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath) {
        return execute("read_word_tables", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                List<XWPFTable> tables = document.getTables();
                if (tables.isEmpty()) {
                    return "文档中没有表格";
                }
                StringBuilder sb = new StringBuilder();
                for (int t = 0; t < tables.size(); t++) {
                    XWPFTable table = tables.get(t);
                    sb.append("[表格 ")
                      .append(t)
                      .append("]\n");
                    for (XWPFTableRow row : table.getRows()) {
                        List<XWPFTableCell> cells = row.getTableCells();
                        StringBuilder rowSb = new StringBuilder();
                        for (int c = 0; c < cells.size(); c++) {
                            if (c > 0) {
                                rowSb.append(" | ");
                            }
                            rowSb.append(cells.get(c)
                                              .getText());
                        }
                        sb.append(rowSb.toString()
                                       .trim())
                          .append("\n");
                    }
                    sb.append("\n");
                }
                return sb.toString()
                         .trim();
            }
        });
    }

    // --- 12. insert_word_image ---

    @Tool(name = "insert_word_image", description = "在 Word 文档末尾插入图片。支持 PNG、JPG、GIF 等常见格式。")
    public String insertWordImage(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "图片文件的绝对路径") String imagePath, @ToolParam(description = "图片宽度（像素）", required = false) Integer width,
        @ToolParam(description = "图片高度（像素）", required = false) Integer height) {
        return execute("insert_word_image", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            Path imgPath = Paths.get(imagePath);
            if (!Files.exists(imgPath)) {
                return "错误: 图片文件不存在: " + imagePath;
            }
            String fileName = imgPath.getFileName()
                                     .toString();
            int dotIndex = fileName.lastIndexOf('.');
            String ext = dotIndex > 0 ? fileName.substring(dotIndex + 1)
                                                .toLowerCase() : "";
            if (!Set.of("png", "jpg", "jpeg", "gif", "bmp")
                    .contains(ext)) {
                throw new IllegalArgumentException("不支持的图片格式: " + ext + "，支持: png/jpg/jpeg/gif/bmp");
            }
            try (XWPFDocument document = openDocument(path); InputStream imgStream = Files.newInputStream(imgPath)) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                int format = switch (ext) {
                    case "png" -> XWPFDocument.PICTURE_TYPE_PNG;
                    case "jpg", "jpeg" -> XWPFDocument.PICTURE_TYPE_JPEG;
                    case "gif" -> XWPFDocument.PICTURE_TYPE_GIF;
                    case "bmp" -> XWPFDocument.PICTURE_TYPE_BMP;
                    default -> XWPFDocument.PICTURE_TYPE_PNG;
                };
                byte[] imgBytes = imgStream.readAllBytes();
                int w = width != null ? width : 200;
                int h = height != null ? height : 150;
                run.addPicture(new java.io.ByteArrayInputStream(imgBytes), format, fileName, w * 9525, h * 9525);
                saveDocument(document, path);
                return "图片 '" + fileName + "' 已插入到 Word 文档末尾: " + fileAbsolutePath;
            }
        });
    }

    // --- 13. word_add_page_break ---

    @Tool(name = "word_add_page_break", description = "在 Word 文档末尾添加分页符，后续内容将在新页面开始。")
    public String wordAddPageBreak(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath) {
        return execute("word_add_page_break", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.addBreak(org.apache.poi.xwpf.usermodel.BreakType.PAGE);
                saveDocument(document, path);
                return "分页符已添加: " + fileAbsolutePath;
            }
        });
    }

    // ==================== P1: 排版与格式控制 ====================

    // --- 14. word_page_setup ---

    @Tool(name = "word_page_setup", description = "设置 Word 文档的页面参数：纸张大小、方向、页边距。")
    public String wordPageSetup(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "纸张大小：A4、A3、LETTER", required = false) String paperSize,
        @ToolParam(description = "方向：portrait（纵向）、landscape（横向）", required = false) String orientation,
        @ToolParam(description = "上边距（磅，1pt≈0.35mm）", required = false) Integer marginTop,
        @ToolParam(description = "下边距（磅）", required = false) Integer marginBottom,
        @ToolParam(description = "左边距（磅）", required = false) Integer marginLeft,
        @ToolParam(description = "右边距（磅）", required = false) Integer marginRight) {
        return execute("word_page_setup", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                CTSectPr sectPr = document.getDocument()
                                          .getBody()
                                          .isSetSectPr() ? document.getDocument()
                                                                   .getBody()
                                                                   .getSectPr() : document.getDocument()
                                                                                          .getBody()
                                                                                          .addNewSectPr();
                if (paperSize != null || orientation != null) {
                    CTPageSz pageSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
                    if (paperSize != null) {
                        switch (paperSize.toLowerCase()) {
                            case "a4" -> {
                                pageSz.setW(BigInteger.valueOf(11906));
                                pageSz.setH(BigInteger.valueOf(16838));
                            }
                            case "a3" -> {
                                pageSz.setW(BigInteger.valueOf(16838));
                                pageSz.setH(BigInteger.valueOf(23814));
                            }
                            case "letter" -> {
                                pageSz.setW(BigInteger.valueOf(12240));
                                pageSz.setH(BigInteger.valueOf(15840));
                            }
                        }
                    }
                    if ("landscape".equalsIgnoreCase(orientation)) {
                        BigInteger w = (BigInteger) pageSz.getW();
                        BigInteger h = (BigInteger) pageSz.getH();
                        pageSz.setW(h);
                        pageSz.setH(w);
                    }
                }
                if (marginTop != null || marginBottom != null || marginLeft != null || marginRight != null) {
                    CTPageMar pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
                    if (marginTop != null) {
                        pageMar.setTop(BigInteger.valueOf(marginTop));
                    }
                    if (marginBottom != null) {
                        pageMar.setBottom(BigInteger.valueOf(marginBottom));
                    }
                    if (marginLeft != null) {
                        pageMar.setLeft(BigInteger.valueOf(marginLeft));
                    }
                    if (marginRight != null) {
                        pageMar.setRight(BigInteger.valueOf(marginRight));
                    }
                }
                saveDocument(document, path);
                return "页面设置已更新: " + fileAbsolutePath;
            }
        });
    }

    // --- 15. word_set_paragraph_alignment ---

    @Tool(name = "word_set_paragraph_alignment", description = "设置 Word 文档中指定段落的对齐方式。")
    public String wordSetParagraphAlignment(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "段落索引（从0开始）") int paragraphIndex,
        @ToolParam(description = "对齐方式：LEFT（左对齐）、CENTER（居中）、RIGHT（右对齐）、BOTH（两端对齐）") String alignment) {
        return execute("word_set_paragraph_alignment", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                if (paragraphIndex < 0 || paragraphIndex >= paragraphs.size()) {
                    return "错误: 段落索引 " + paragraphIndex + " 超出范围（0~" + (paragraphs.size() - 1) + "）";
                }
                XWPFParagraph paragraph = paragraphs.get(paragraphIndex);
                ParagraphAlignment align = switch (alignment.toUpperCase()) {
                    case "LEFT" -> ParagraphAlignment.LEFT;
                    case "CENTER" -> ParagraphAlignment.CENTER;
                    case "RIGHT" -> ParagraphAlignment.RIGHT;
                    case "BOTH" -> ParagraphAlignment.BOTH;
                    default -> throw new IllegalArgumentException("不支持的对齐方式: " + alignment);
                };
                paragraph.setAlignment(align);
                saveDocument(document, path);
                return "段落 " + paragraphIndex + " 对齐方式已设置为 " + alignment + ": " + fileAbsolutePath;
            }
        });
    }

    // --- 16. word_set_line_spacing ---

    @Tool(name = "word_set_line_spacing", description = "设置 Word 文档中指定段落的行间距（倍数）。")
    public String wordSetLineSpacing(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "段落索引（从0开始），-1 表示应用到所有段落", required = false) Integer paragraphIndex,
        @ToolParam(description = "行间距倍数，如 1.0、1.5、2.0") double lineSpacing) {
        return execute("word_set_line_spacing", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                if (paragraphIndex != null && paragraphIndex >= 0) {
                    if (paragraphIndex >= paragraphs.size()) {
                        return "错误: 段落索引超出范围";
                    }
                    setParagraphLineSpacing(paragraphs.get(paragraphIndex), lineSpacing);
                } else {
                    for (XWPFParagraph p : paragraphs) {
                        setParagraphLineSpacing(p, lineSpacing);
                    }
                }
                saveDocument(document, path);
                String target = paragraphIndex != null ? "段落 " + paragraphIndex : "所有段落";
                return target + " 行间距已设置为 " + lineSpacing + " 倍: " + fileAbsolutePath;
            }
        });
    }

    private void setParagraphLineSpacing(XWPFParagraph paragraph, double lineSpacing) {
        CTPPr ppr = paragraph.getCTP()
                             .isSetPPr() ? paragraph.getCTP()
                                                    .getPPr() : paragraph.getCTP()
                                                                         .addNewPPr();
        CTSpacing spacing = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
        spacing.setLine(BigInteger.valueOf(Math.round(lineSpacing * 240)));
        spacing.setLineRule(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.AUTO);
    }

    // --- 17. word_add_header_footer ---

    @Tool(name = "word_add_header_footer", description = "为 Word 文档添加页眉和页脚。")
    public String wordAddHeaderFooter(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "页眉文本（可选，留空则跳过）", required = false) String headerText,
        @ToolParam(description = "页脚文本（可选，留空则跳过）", required = false) String footerText) {
        return execute("word_add_header_footer", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                XWPFHeaderFooterPolicy policy = document.getHeaderFooterPolicy();
                if (policy == null) {
                    policy = document.createHeaderFooterPolicy();
                }
                if (headerText != null && !headerText.isBlank()) {
                    XWPFHeader header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
                    XWPFParagraph paragraph = header.createParagraph();
                    paragraph.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun run = paragraph.createRun();
                    run.setText(headerText);
                    run.setFontSize(9);
                    run.setColor("808080");
                }
                if (footerText != null && !footerText.isBlank()) {
                    XWPFFooter footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
                    XWPFParagraph paragraph = footer.createParagraph();
                    paragraph.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun run = paragraph.createRun();
                    run.setText(footerText);
                    run.setFontSize(9);
                    run.setColor("808080");
                }
                saveDocument(document, path);
                return "页眉页脚已设置: " + fileAbsolutePath;
            }
        });
    }

    // --- 18. word_add_page_number ---

    @Tool(name = "word_add_page_number", description = "为 Word 文档添加页码。页码默认添加在页脚居中位置。")
    public String wordAddPageNumber(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath) {
        return execute("word_add_page_number", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                XWPFHeaderFooterPolicy policy = document.getHeaderFooterPolicy();
                if (policy == null) {
                    policy = document.createHeaderFooterPolicy();
                }
                XWPFFooter footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
                XWPFParagraph paragraph = footer.createParagraph();
                paragraph.setAlignment(ParagraphAlignment.CENTER);
                CTPPr ppr = paragraph.getCTP()
                                     .addNewPPr();
                CTJc jc = ppr.addNewJc();
                jc.setVal(STJc.CENTER);
                CTR ctr = paragraph.getCTP()
                                   .addNewR();
                CTText ctText = ctr.addNewT();
                ctText.setStringValue("第 ");
                CTR pageRun = paragraph.getCTP()
                                       .addNewR();
                CTFldChar fldBegin = pageRun.addNewFldChar();
                fldBegin.setFldCharType(STFldCharType.BEGIN);
                CTR pageRun2 = paragraph.getCTP()
                                        .addNewR();
                CTText instrText = pageRun2.addNewInstrText();
                instrText.setStringValue(" PAGE ");
                CTR pageRun3 = paragraph.getCTP()
                                        .addNewR();
                CTFldChar fldEnd = pageRun3.addNewFldChar();
                fldEnd.setFldCharType(STFldCharType.END);
                CTR ctr3 = paragraph.getCTP()
                                    .addNewR();
                CTText ctText3 = ctr3.addNewT();
                ctText3.setStringValue(" 页");
                saveDocument(document, path);
                return "页码已添加: " + fileAbsolutePath;
            }
        });
    }

    // ==================== P2: 文档元素增强 ====================

    // --- 19. word_add_bullet_list ---

    @Tool(name = "word_add_bullet_list", description = "在 Word 文档末尾添加项目符号或编号列表。")
    public String wordAddBulletList(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "列表项，每项一行") String items,
        @ToolParam(description = "true 表示编号列表，false 表示项目符号列表（默认）") boolean numbered) {
        return execute("word_add_bullet_list", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            String[] itemArray = items.split("\n");
            try (XWPFDocument document = openDocument(path)) {
                for (int i = 0; i < itemArray.length; i++) {
                    XWPFParagraph paragraph = document.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    String prefix = numbered ? (i + 1) + ". " : "\u2022 ";
                    run.setText(prefix + itemArray[i].trim());
                }
                saveDocument(document, path);
                String type = numbered ? "编号" : "项目符号";
                return type + "列表已插入到 Word 文档末尾: " + fileAbsolutePath;
            }
        });
    }

    // --- 20. word_add_hyperlink ---

    @Tool(name = "word_add_hyperlink", description = "在 Word 文档末尾添加超链接。")
    public String wordAddHyperlink(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "超链接的显示文本") String text, @ToolParam(description = "超链接的 URL") String url) {
        return execute("word_add_hyperlink", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                XWPFParagraph paragraph = document.createParagraph();
                String linkId = document.getPackagePart()
                                        .addExternalRelationship(url, org.apache.poi.openxml4j.opc.PackageRelationshipTypes.HYPERLINK_PART)
                                        .getId();
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink ctHyperlink = paragraph.getCTP()
                                                                                                          .addNewHyperlink();
                ctHyperlink.setId(linkId);
                CTR ctr = ctHyperlink.addNewR();
                CTText ctText = ctr.addNewT();
                ctText.setStringValue(text);
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr rpr = ctr.addNewRPr();
                rpr.addNewColor()
                   .setVal("0000FF");
                rpr.addNewU()
                   .setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STUnderline.SINGLE);
                saveDocument(document, path);
                return "超链接 '" + text + "' 已插入到 Word 文档末尾: " + fileAbsolutePath;
            }
        });
    }

    // --- 21. word_add_table_of_contents ---

    @Tool(name = "word_add_table_of_contents", description = "在 Word 文档开头插入目录域。需要在 Word 中右键刷新以更新目录。")
    public String wordAddTableOfContents(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath) {
        return execute("word_add_table_of_contents", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                XWPFParagraph tocParagraph = document.createParagraph();
                XWPFRun titleRun = tocParagraph.createRun();
                titleRun.setText("目录");
                titleRun.setBold(true);
                titleRun.setFontSize(16);
                XWPFParagraph tocField = document.createParagraph();
                CTR ctr = tocField.getCTP()
                                  .addNewR();
                CTFldChar fldBegin = ctr.addNewFldChar();
                fldBegin.setFldCharType(STFldCharType.BEGIN);
                CTR ctr2 = tocField.getCTP()
                                   .addNewR();
                CTText instrText = ctr2.addNewInstrText();
                instrText.setStringValue(" TOC \\o \"1-3\" \\h \\z \\u ");
                CTR ctr3 = tocField.getCTP()
                                   .addNewR();
                CTFldChar fldSeparate = ctr3.addNewFldChar();
                fldSeparate.setFldCharType(STFldCharType.SEPARATE);
                CTR ctr4 = tocField.getCTP()
                                   .addNewR();
                CTText placeholder = ctr4.addNewT();
                placeholder.setStringValue("（请右键点击此处，选择「更新域」以生成目录）");
                CTR ctr5 = tocField.getCTP()
                                   .addNewR();
                CTFldChar fldEnd = ctr5.addNewFldChar();
                fldEnd.setFldCharType(STFldCharType.END);
                CTBody body = document.getDocument()
                                      .getBody();
                body.getDomNode()
                    .insertBefore(tocParagraph.getCTP()
                                              .getDomNode(), body.getDomNode()
                                                                 .getFirstChild());
                body.getDomNode()
                    .insertBefore(tocField.getCTP()
                                          .getDomNode(), body.getDomNode()
                                                             .getFirstChild());
                saveDocument(document, path);
                return "目录已插入到文档开头: " + fileAbsolutePath + "（需在 Word 中右键刷新）";
            }
        });
    }

    // --- 22. word_add_watermark ---

    @Tool(name = "word_add_watermark", description = "为 Word 文档添加水印文字。水印以灰色斜体显示在页面中央。")
    public String wordAddWatermark(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "水印文本，如\"机密\"、\"草稿\"、\"内部资料\"") String watermarkText) {
        return execute("word_add_watermark", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                XWPFHeaderFooterPolicy policy = document.getHeaderFooterPolicy();
                if (policy == null) {
                    policy = document.createHeaderFooterPolicy();
                }
                XWPFHeader header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
                XWPFParagraph paragraph = header.createParagraph();
                paragraph.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun run = paragraph.createRun();
                run.setText(watermarkText);
                run.setColor("C0C0C0");
                run.setFontSize(48);
                run.setBold(true);
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr rpr = run.getCTR()
                                                                                      .isSetRPr() ? run.getCTR()
                                                                                                       .getRPr() : run.getCTR()
                                                                                                                      .addNewRPr();
                rpr.addNewHighlight()
                   .setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STHighlightColor.NONE);
                saveDocument(document, path);
                return "水印 '" + watermarkText + "' 已添加: " + fileAbsolutePath;
            }
        });
    }

    // ==================== P3: 文档转换与高级操作 ====================

    // --- 23. word_convert_to_pdf ---

    @Tool(name = "word_convert_to_pdf", description = "将 Word 文档转换为 PDF 文件。注意：转换基于文本内容，复杂格式可能丢失。")
    public String wordConvertToPdf(@ToolParam(description = "源 Word 文档的绝对路径") String sourcePath,
        @ToolParam(description = "目标 PDF 文件的绝对路径") String targetPath) {
        return execute("word_convert_to_pdf", () -> {
            Path srcPath = validateDocxFile(sourcePath);
            Path tgtPath = Paths.get(targetPath);
            PathUtil.ensureParentDirectory(tgtPath);
            try (XWPFDocument document = openDocument(srcPath); PDDocument pdfDoc = new PDDocument()) {
                float margin = 50;
                float fontSize = 11;
                float lineHeight = fontSize * 1.5f;
                PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                java.util.List<Object[]> contentLines = new java.util.ArrayList<>();
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    String text = paragraph.getText();
                    String style = paragraph.getStyle();
                    boolean isHeading = style != null && style.startsWith("Heading");
                    float textFontSize = isHeading ? fontSize + 4 : fontSize;
                    if (text == null || text.isBlank()) {
                        contentLines.add(new Object[]{"", textFontSize, false});
                    } else {
                        float maxWidth = 595 - 2 * margin;
                        for (String line : wrapText(text, textFontSize, maxWidth)) {
                            contentLines.add(new Object[]{line, textFontSize, isHeading});
                        }
                    }
                }
                java.util.Iterator<Object[]> lineIter = contentLines.iterator();
                while (lineIter.hasNext()) {
                    PDPage page = new PDPage();
                    pdfDoc.addPage(page);
                    float yPosition = page.getMediaBox()
                                          .getHeight() - margin;
                    try (PDPageContentStream cs = new PDPageContentStream(pdfDoc, page)) {
                        cs.beginText();
                        cs.newLineAtOffset(margin, yPosition);
                        while (lineIter.hasNext() && yPosition >= margin) {
                            Object[] lineInfo = lineIter.next();
                            String line = (String) lineInfo[0];
                            float lineFontSize = (Float) lineInfo[1];
                            boolean bold = (Boolean) lineInfo[2];
                            cs.setFont(bold ? boldFont : regularFont, lineFontSize);
                            if (!line.isEmpty()) {
                                cs.showText(line);
                            }
                            yPosition -= lineHeight;
                            cs.newLineAtOffset(0, -lineHeight);
                        }
                        cs.endText();
                    }
                }
                pdfDoc.save(tgtPath.toFile());
                return "Word 文档已转换为 PDF: " + targetPath;
            }
        });
    }

    private List<String> wrapText(String text, float fontSize, float maxWidth) {
        List<String> lines = new ArrayList<>();
        float charWidth = fontSize * 0.6f;
        int maxChars = (int) (maxWidth / charWidth);
        if (text.length() <= maxChars) {
            lines.add(text);
        } else {
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + maxChars, text.length());
                lines.add(text.substring(start, end));
                start = end;
            }
        }
        return lines;
    }

    // --- 24. word_merge_documents ---

    @Tool(name = "word_merge_documents", description = "将多个 Word 文档合并为一个文档。按顺序追加所有源文档的段落和表格。")
    public String wordMergeDocuments(@ToolParam(description = "合并输出的 Word 文档的绝对路径") String targetPath,
        @ToolParam(description = "源 Word 文档的绝对路径，逗号分隔") String sourceFilePaths) {
        return execute("word_merge_documents", () -> {
            String[] sources = sourceFilePaths.split(",");
            Path tgtPath = Paths.get(targetPath);
            PathUtil.ensureParentDirectory(tgtPath);
            try (XWPFDocument mergedDoc = new XWPFDocument()) {
                for (String src : sources) {
                    Path srcFile = Paths.get(src.trim());
                    if (!Files.exists(srcFile)) {
                        return "错误: 源文件不存在: " + src.trim();
                    }
                    try (XWPFDocument srcDoc = openDocument(srcFile)) {
                        for (XWPFParagraph srcPara : srcDoc.getParagraphs()) {
                            XWPFParagraph newPara = mergedDoc.createParagraph();
                            newPara.getCTP()
                                   .set(srcPara.getCTP()
                                               .copy());
                        }
                        for (XWPFTable srcTable : srcDoc.getTables()) {
                            XWPFTable newTable = mergedDoc.createTable();
                            newTable.getCTTbl()
                                    .set(srcTable.getCTTbl()
                                                 .copy());
                        }
                    }
                }
                saveDocument(mergedDoc, tgtPath);
                return "已合并 " + sources.length + " 个文档到: " + targetPath;
            }
        });
    }

    // --- 25. word_extract_images ---

    @Tool(name = "word_extract_images", description = "提取 Word 文档中的所有嵌入图片并保存到指定目录。")
    public String wordExtractImages(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "提取图片的输出目录的绝对路径") String outputDir) {
        return execute("word_extract_images", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            Path outDir = Paths.get(outputDir);
            if (!Files.exists(outDir)) {
                Files.createDirectories(outDir);
            }
            try (XWPFDocument document = openDocument(path)) {
                List<XWPFPictureData> pictures = document.getAllPictures();
                if (pictures.isEmpty()) {
                    return "文档中没有嵌入图片";
                }
                int count = 0;
                for (XWPFPictureData picture : pictures) {
                    count++;
                    String ext = picture.suggestFileExtension();
                    String fileName = "image_" + count + "." + ext;
                    Path outFile = outDir.resolve(fileName);
                    Files.write(outFile, picture.getData());
                }
                return "已提取 " + count + " 张图片到: " + outputDir;
            }
        });
    }

    // --- 26. word_add_comment ---

    @Tool(name = "word_add_comment", description = "为 Word 文档指定段落添加批注。")
    public String wordAddComment(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "要添加批注的段落索引（从0开始）") int paragraphIndex, @ToolParam(description = "批注文本") String commentText,
        @ToolParam(description = "作者名称", required = false) String author) {
        return execute("word_add_comment", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                if (paragraphIndex < 0 || paragraphIndex >= paragraphs.size()) {
                    return "错误: 段落索引超出范围";
                }
                XWPFParagraph paragraph = paragraphs.get(paragraphIndex);
                String authorName = (author != null && !author.isBlank()) ? author : "Reviewer";
                XWPFRun commentRun = paragraph.createRun();
                commentRun.setText(" [批注@" + authorName + ": " + commentText + "]");
                commentRun.setItalic(true);
                commentRun.setColor("808080");
                commentRun.setFontSize(9);
                saveDocument(document, path);
                return "批注已添加到段落 " + paragraphIndex + ": " + fileAbsolutePath + "（作者: " + authorName + "）";
            }
        });
    }

    // --- 27. word_count ---

    @Tool(name = "word_count", description = "统计 Word 文档的字数、段落数、表格数和图片数。")
    public String wordCount(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath) {
        return execute("word_count", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                int paragraphCount = 0;
                int characterCount = 0;
                int wordCount = 0;
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    String text = paragraph.getText();
                    if (text != null && !text.isBlank()) {
                        paragraphCount++;
                        characterCount += text.length();
                        wordCount += text.split("\\s+").length;
                    }
                }
                int tableCount = document.getTables()
                                         .size();
                int imageCount = document.getAllPictures()
                                         .size();
                return "文档统计: " + fileAbsolutePath + "\n" + "  段落数: " + paragraphCount + "\n" + "  字数（英文）: " + wordCount + "\n" + "  字符数: "
                    + characterCount + "\n" + "  表格数: " + tableCount + "\n" + "  图片数: " + imageCount;
            }
        });
    }

    // ==================== Word→MD: 结构化读取 ====================

    // --- 28. read_word_structured ---

    @Tool(name = "read_word_structured", description = "以结构化格式读取 Word 文档完整内容，保留标题层级、段落格式、表格、列表和图片信息。适用于 AI 转换为 Markdown 等场景。")
    public String readWordStructured(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath) {
        return execute("read_word_structured", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                StringBuilder sb = new StringBuilder();
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                for (int i = 0; i < paragraphs.size(); i++) {
                    XWPFParagraph paragraph = paragraphs.get(i);
                    String text = paragraph.getText();
                    if (text == null || text.isBlank()) {
                        continue;
                    }
                    String style = paragraph.getStyle() != null ? paragraph.getStyle() : "Normal";
                    if (style.startsWith("Heading")) {
                        try {
                            int level = Integer.parseInt(style.replace("Heading", ""));
                            sb.append("[HEADING:")
                              .append(level)
                              .append("] ")
                              .append(text)
                              .append("\n");
                        } catch (NumberFormatException e) {
                            sb.append("[HEADING:1] ")
                              .append(text)
                              .append("\n");
                        }
                    } else if (paragraph.getNumID() != null) {
                        sb.append("[LIST_ITEM] ")
                          .append(text)
                          .append("\n");
                    } else {
                        sb.append("[PARAGRAPH:ALIGN=")
                          .append(paragraph.getAlignment()
                                           .name())
                          .append("]\n");
                        List<XWPFRun> runs = paragraph.getRuns();
                        if (runs != null) {
                            for (XWPFRun run : runs) {
                                String runText = run.getText(0);
                                if (runText == null || runText.isBlank()) {
                                    continue;
                                }
                                sb.append("[RUNS:bold=")
                                  .append(run.isBold())
                                  .append(",italic=")
                                  .append(run.isItalic())
                                  .append(",size=")
                                  .append(run.getFontSizeAsDouble());
                                String color = run.getColor();
                                if (color != null) {
                                    sb.append(",color=")
                                      .append(color);
                                }
                                if (run.getUnderline() != UnderlinePatterns.NONE) {
                                    sb.append(",underline=true");
                                }
                                sb.append("] ")
                                  .append(runText)
                                  .append("\n");
                            }
                        }
                        sb.append("\n");
                    }
                }
                List<XWPFTable> tables = document.getTables();
                for (int t = 0; t < tables.size(); t++) {
                    XWPFTable table = tables.get(t);
                    sb.append("[TABLE:")
                      .append(t)
                      .append("]\n");
                    List<XWPFTableRow> tableRows = table.getRows();
                    for (int r = 0; r < tableRows.size(); r++) {
                        XWPFTableRow row = tableRows.get(r);
                        List<XWPFTableCell> cells = row.getTableCells();
                        StringBuilder rowSb = new StringBuilder();
                        for (int c = 0; c < cells.size(); c++) {
                            if (c > 0) {
                                rowSb.append(" | ");
                            }
                            rowSb.append(cells.get(c)
                                              .getText()
                                              .replace("\n", " "));
                        }
                        String rowType = r == 0 ? "HEADER" : "DATA";
                        sb.append("[ROW:")
                          .append(rowType)
                          .append("] ")
                          .append(rowSb.toString()
                                       .trim())
                          .append("\n");
                    }
                    sb.append("\n");
                }
                List<XWPFPictureData> pictures = document.getAllPictures();
                for (int p = 0; p < pictures.size(); p++) {
                    XWPFPictureData picture = pictures.get(p);
                    sb.append("[IMAGE:")
                      .append(p)
                      .append("] ")
                      .append(picture.getFileName())
                      .append(" (")
                      .append(picture.getData().length)
                      .append(" bytes)\n");
                }
                return sb.toString()
                         .trim();
            }
        });
    }

    // ==================== P0: Word 转 Markdown ====================

    // --- 29. word_convert_to_markdown ---

    /**
     * 将 Word 文档转换为 Markdown 格式。
     * 读取 Word 文档的所有段落、标题、表格，转换为 Markdown 格式。
     * 标题根据样式层级转为 # ## ### 等，表格转为 Markdown 表格，段落保持原样。
     * 如果指定了 targetFilePath 则保存为 .md 文件，否则直接返回 Markdown 内容字符串。
     *
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "word_convert_to_markdown", description = "将 Word 文档转换为 Markdown 格式。支持标题、段落、表格和内联格式（粗体、斜体）。")
    public String wordConvertToMarkdown(@ToolParam(description = "源 Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "目标 Markdown 文件的绝对路径（可选，不指定则直接返回 Markdown 内容）", required = false) String targetFilePath) {
        return execute("word_convert_to_markdown", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                StringBuilder md = new StringBuilder();

                // 处理段落
                for (XWPFParagraph para : document.getParagraphs()) {
                    String style = para.getStyle();
                    String text = para.getText();

                    if (text == null || text.trim()
                                            .isEmpty()) {
                        md.append("\n");
                        continue;
                    }

                    // 判断是否是标题
                    if (style != null && style.startsWith("Heading")) {
                        int level = 1;
                        try {
                            level = Integer.parseInt(style.replace("Heading", "")
                                                          .trim());
                        } catch (NumberFormatException e) {
                            level = 1;
                        }
                        if (level > 6) {
                            level = 6;
                        }
                        String prefix = "#".repeat(level);
                        md.append(prefix)
                          .append(" ")
                          .append(text.trim())
                          .append("\n\n");
                        continue;
                    }

                    // 处理段落中的内联格式（粗体、斜体）
                    List<XWPFRun> runs = para.getRuns();
                    if (runs != null && !runs.isEmpty()) {
                        for (XWPFRun run : runs) {
                            String runText = run.getText(0);
                            if (runText == null) {
                                continue;
                            }
                            if (run.isBold() && run.isItalic()) {
                                md.append("***")
                                  .append(runText)
                                  .append("***");
                            } else if (run.isBold()) {
                                md.append("**")
                                  .append(runText)
                                  .append("**");
                            } else if (run.isItalic()) {
                                md.append("*")
                                  .append(runText)
                                  .append("*");
                            } else {
                                md.append(runText);
                            }
                        }
                    } else {
                        md.append(text.trim());
                    }
                    md.append("\n\n");
                }

                // 处理表格
                List<XWPFTable> tables = document.getTables();
                if (!tables.isEmpty()) {
                    for (XWPFTable table : tables) {
                        List<XWPFTableRow> rows = table.getRows();
                        if (rows.isEmpty()) {
                            continue;
                        }
                        for (int i = 0; i < rows.size(); i++) {
                            XWPFTableRow row = rows.get(i);
                            md.append("|");
                            for (XWPFTableCell cell : row.getTableCells()) {
                                md.append(" ")
                                  .append(cell.getText()
                                              .replace("\n", " "))
                                  .append(" |");
                            }
                            md.append("\n");
                            // 表头分隔线
                            if (i == 0) {
                                md.append("|");
                                for (int j = 0; j < row.getTableCells()
                                                       .size(); j++) {
                                    md.append(" --- |");
                                }
                                md.append("\n");
                            }
                        }
                        md.append("\n");
                    }
                }

                String markdownContent = md.toString()
                                           .trim();
                if (targetFilePath != null && !targetFilePath.isBlank()) {
                    Path targetPath = Paths.get(targetFilePath);
                    PathUtil.ensureParentDirectory(targetPath);
                    Files.writeString(targetPath, markdownContent);
                    LogUtil.info("wordConvertToMarkdown 完成，已保存到: {}", targetFilePath);
                    return "Word 文档已转换为 Markdown 并保存到: " + targetFilePath;
                }

                LogUtil.info("wordConvertToMarkdown 完成，返回 Markdown 内容");
                return markdownContent;
            }
        });
    }

    // ==================== P1: 排版与格式控制 ====================

    // --- 30. word_delete_paragraph ---

    /**
     * 删除 Word 文档中指定索引的段落。
     * 通过段落所在的 XML 位置来删除，段落索引从 0 开始。
     *
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "word_delete_paragraph", description = "删除 Word 文档中指定索引的段落。段落索引从 0 开始。")
    public String wordDeleteParagraph(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Paragraph index (0-based)") int paragraphIndex) {
        return execute("word_delete_paragraph", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            try (XWPFDocument document = openDocument(path)) {
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                if (paragraphIndex < 0 || paragraphIndex >= paragraphs.size()) {
                    return "错误: 段落索引 " + paragraphIndex + " 超出范围（0~" + (paragraphs.size() - 1) + "）";
                }
                XWPFParagraph para = paragraphs.get(paragraphIndex);
                int pos = document.getPosOfParagraph(para);
                document.removeBodyElement(pos);
                saveDocument(document, path);
                LogUtil.info("wordDeleteParagraph 删除段落索引 {}: {}", paragraphIndex, fileAbsolutePath);
                return "段落 " + paragraphIndex + " 已删除: " + fileAbsolutePath;
            }
        });
    }

    // --- 31. word_find ---

    /**
     * 在 Word 文档中搜索关键词，返回包含关键词的段落索引和内容摘要。
     * 支持区分大小写搜索，返回 JSON 格式的结果。
     *
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "word_find", description = "在 Word 文档中搜索关键词，返回包含关键词的段落索引、内容和匹配详情。")
    public String wordFind(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "要搜索的关键词") String keyword,
        @ToolParam(description = "是否区分大小写（默认 false）", required = false) Boolean caseSensitive) {
        return execute("word_find", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            boolean caseSensitiveSearch = caseSensitive != null && caseSensitive;
            try (XWPFDocument document = openDocument(path)) {
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                List<Map<String, Object>> resultList = new ArrayList<>();
                String searchKeyword = caseSensitiveSearch ? keyword : keyword.toLowerCase();
                for (int i = 0; i < paragraphs.size(); i++) {
                    XWPFParagraph para = paragraphs.get(i);
                    String text = para.getText();
                    if (text == null || text.isBlank()) {
                        continue;
                    }
                    String searchText = caseSensitiveSearch ? text : text.toLowerCase();
                    if (searchText.contains(searchKeyword)) {
                        // 收集所有匹配
                        List<String> matches = new ArrayList<>();
                        int idx = 0;
                        String originalText = text;
                        String compareText = searchText;
                        while ((idx = compareText.indexOf(searchKeyword, idx)) != -1) {
                            int end = Math.min(idx + keyword.length(), originalText.length());
                            matches.add(originalText.substring(idx, end));
                            idx += keyword.length();
                        }
                        // 截取摘要
                        String summary = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("paragraphIndex", i);
                        item.put("text", summary);
                        item.put("matches", matches);
                        resultList.add(item);
                    }
                }
                LogUtil.info("wordFind 搜索关键词 '{}' 完成，共找到匹配段落", keyword);
                return JSON.toJSONString(resultList);
            }
        });
    }

    // --- 32. word_set_font ---

    /**
     * 统一设置 Word 文档中段落的字体名称和大小。
     * 支持设置全部段落、仅正文段落或仅标题段落。
     *
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "word_set_font", description = "统一设置 Word 文档中段落的字体名称和大小。支持设置全部、仅正文或仅标题。")
    public String wordSetFont(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "字体名称，如\"宋体\"、\"微软雅黑\"、\"Arial\"") String fontName,
        @ToolParam(description = "字号（磅，默认 12）", required = false) Integer fontSize,
        @ToolParam(description = "范围：all（全部，默认）、body（仅正文段落）、headings（仅标题段落）", required = false) String scope) {
        return execute("word_set_font", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            int size = fontSize != null ? fontSize : 12;
            String applyScope = scope != null && !scope.isBlank() ? scope.toLowerCase() : "all";
            try (XWPFDocument document = openDocument(path)) {
                int updatedCount = 0;
                for (XWPFParagraph para : document.getParagraphs()) {
                    String style = para.getStyle();
                    boolean isHeading = style != null && style.startsWith("Heading");
                    boolean shouldApply = switch (applyScope) {
                        case "all" -> true;
                        case "body" -> !isHeading;
                        case "headings" -> isHeading;
                        default -> true;
                    };
                    if (shouldApply) {
                        List<XWPFRun> runs = para.getRuns();
                        if (runs != null) {
                            for (XWPFRun run : runs) {
                                run.setFontFamily(fontName);
                                run.setFontSize(size);
                            }
                            updatedCount++;
                        }
                    }
                }
                saveDocument(document, path);
                LogUtil.info("wordSetFont 完成，已更新 {} 个段落的字体为 {} {}pt: {}", updatedCount, fontName, size, fileAbsolutePath);
                return "已更新 " + updatedCount + " 个段落的字体为 " + fontName + " " + size + "pt: " + fileAbsolutePath;
            }
        });
    }

    // --- 33. word_set_paragraph_spacing ---

    /**
     * 设置 Word 文档中段落的段前段后间距和行距。
     * 支持指定单个段落或所有段落。
     *
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "word_set_paragraph_spacing", description = "设置 Word 文档中段落的段前间距、段后间距和行距。")
    public String wordSetParagraphSpacing(@ToolParam(description = "Word 文档的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "段落索引（从0开始），-1 表示应用到所有段落", required = false) Integer paragraphIndex,
        @ToolParam(description = "段前间距（磅，默认 0）", required = false) Integer beforeSpacing,
        @ToolParam(description = "段后间距（磅，默认 0）", required = false) Integer afterSpacing,
        @ToolParam(description = "行距倍数（默认 1.0）", required = false) Double lineSpacing) {
        return execute("word_set_paragraph_spacing", () -> {
            Path path = validateDocxFile(fileAbsolutePath);
            int before = beforeSpacing != null ? beforeSpacing : 0;
            int after = afterSpacing != null ? afterSpacing : 0;
            double line = lineSpacing != null ? lineSpacing : 1.0;
            int paraIdx = paragraphIndex != null ? paragraphIndex : -1;
            try (XWPFDocument document = openDocument(path)) {
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                if (paraIdx >= 0) {
                    if (paraIdx >= paragraphs.size()) {
                        return "错误: 段落索引 " + paraIdx + " 超出范围（0~" + (paragraphs.size() - 1) + "）";
                    }
                    XWPFParagraph para = paragraphs.get(paraIdx);
                    para.setSpacingBefore(before);
                    para.setSpacingAfter(after);
                    para.setSpacingBetween(line);
                    saveDocument(document, path);
                    LogUtil.info("wordSetParagraphSpacing 完成，段落 {} 间距已设置: {}", paraIdx, fileAbsolutePath);
                    return "段落 " + paraIdx + " 的段前间距=" + before + "pt, 段后间距=" + after + "pt, 行距=" + line + "倍: " + fileAbsolutePath;
                } else {
                    int updatedCount = 0;
                    for (XWPFParagraph para : paragraphs) {
                        String text = para.getText();
                        if (text == null || text.isBlank()) {
                            continue;
                        }
                        para.setSpacingBefore(before);
                        para.setSpacingAfter(after);
                        para.setSpacingBetween(line);
                        updatedCount++;
                    }
                    saveDocument(document, path);
                    LogUtil.info("wordSetParagraphSpacing 完成，已更新 {} 个段落: {}", updatedCount, fileAbsolutePath);
                    return "已更新 " + updatedCount + " 个段落的段前间距=" + before + "pt, 段后间距=" + after + "pt, 行距=" + line + "倍: "
                        + fileAbsolutePath;
                }
            }
        });
    }
}