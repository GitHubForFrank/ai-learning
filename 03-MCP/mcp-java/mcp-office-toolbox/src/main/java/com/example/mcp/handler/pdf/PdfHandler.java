package com.example.mcp.handler.pdf;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.util.FileValidateUtil;
import com.example.mcp.util.LogUtil;
import com.example.mcp.util.PathUtil;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * MCP PDF 工具实现，提供 PDF 文档的读取、解析、元信息获取和文本转换功能。
 * 设计原则：轻量稳定，不依赖重型 PDF 编辑器，无高危编辑功能，定位只读解析。
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@Service
public class PdfHandler extends BaseHandler {

    /**
     * 自定义中文字体文件路径，逗号分隔，优先级高于内置默认路径。
     * 可通过 application.properties 中的 pdf.chinese-font-paths 配置。
     */
    @Value("${pdf.chinese-font-paths:}")
    private String configuredFontPaths;

    /**
     * 校验 PDF 文件路径
     */
    private Path validatePdfFile(String fileAbsolutePath) {
        return FileValidateUtil.validateFile(fileAbsolutePath, ".pdf");
    }

    // --- 1. read_pdf_text ---

    /**
     * 读取 PDF 全部文字内容
     */
    @Tool(name = "read_pdf_text", description = "读取 PDF 文件的全部文本内容。返回从所有页面提取的完整文本。")
    public String readPdfText(@ToolParam(description = "PDF 文件的绝对路径") String fileAbsolutePath) {
        return execute("read_pdf_text", () -> {
            Path path = validatePdfFile(fileAbsolutePath);
            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(document);
                if (text == null || text.isBlank()) {
                    return "PDF 文件未包含可提取的文字内容: " + fileAbsolutePath;
                }
                return text;
            }
        });
    }

    // --- 2. read_pdf_page ---

    /**
     * 按页码单独读取指定页面文本
     */
    @Tool(name = "read_pdf_page", description = "读取 PDF 文件指定页面的文本内容。页码从 1 开始。")
    public String readPdfPage(@ToolParam(description = "PDF 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "要读取的页码（从1开始）") int pageNumber) {
        return execute("read_pdf_page", () -> {
            Path path = validatePdfFile(fileAbsolutePath);
            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                int totalPages = document.getNumberOfPages();
                if (pageNumber < 1 || pageNumber > totalPages) {
                    return String.format("错误: 页码 %d 超出范围，PDF 共 %d 页", pageNumber, totalPages);
                }

                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String text = stripper.getText(document);
                if (text == null || text.isBlank()) {
                    return String.format("第 %d 页未包含可提取的文字内容", pageNumber);
                }
                return text;
            }
        });
    }

    // --- 3. get_pdf_info ---

    /**
     * 获取 PDF 总页数、文档基础元信息
     */
    @Tool(name = "get_pdf_info", description = "获取 PDF 文档信息，包括总页数、标题、作者、主题、关键词、创建者、生成器、创建日期和修改日期。")
    public String getPdfInfo(@ToolParam(description = "PDF 文件的绝对路径") String fileAbsolutePath) {
        return execute("get_pdf_info", () -> {
            Path path = validatePdfFile(fileAbsolutePath);
            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("filePath", fileAbsolutePath);
                info.put("totalPages", document.getNumberOfPages());
                info.put("fileSize", Files.size(path));

                PDDocumentInformation docInfo = document.getDocumentInformation();
                info.put("title", docInfo.getTitle());
                info.put("author", docInfo.getAuthor());
                info.put("subject", docInfo.getSubject());
                info.put("keywords", docInfo.getKeywords());
                info.put("creator", docInfo.getCreator());
                info.put("producer", docInfo.getProducer());
                info.put("creationDate", docInfo.getCreationDate() != null ? docInfo.getCreationDate()
                                                                                    .getTime()
                                                                                    .toString() : null);
                info.put("modificationDate", docInfo.getModificationDate() != null ? docInfo.getModificationDate()
                                                                                            .getTime()
                                                                                            .toString() : null);

                // 判断是否加密
                info.put("isEncrypted", document.isEncrypted());

                StringBuilder sb = new StringBuilder();
                sb.append("PDF 文件信息：\n");
                for (Map.Entry<String, Object> entry : info.entrySet()) {
                    sb.append("  ")
                      .append(entry.getKey())
                      .append(": ")
                      .append(entry.getValue())
                      .append("\n");
                }
                return sb.toString();
            }
        });
    }

    // --- 4. convert_pdf_to_txt ---

    /**
     * PDF 文件批量转换为纯 TXT 文本
     */
    @Tool(name = "convert_pdf_to_txt", description = "将 PDF 文件转换为纯文本 TXT 文件。提取所有文本内容并保存到指定输出路径。")
    public String convertPdfToTxt(@ToolParam(description = "PDF 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "TXT 文件的输出路径（可选，默认与 PDF 同目录）", required = false) String outputPath) {
        return execute("convert_pdf_to_txt", () -> {
            Path path = validatePdfFile(fileAbsolutePath);
            Path output;
            if (outputPath != null && !outputPath.isBlank()) {
                output = Paths.get(outputPath);
            } else {
                // 默认输出到 PDF 同目录，同名但扩展名为 .txt
                String pdfName = path.getFileName()
                                     .toString();
                String txtName = pdfName.substring(0, pdfName.lastIndexOf('.')) + ".txt";
                output = path.getParent()
                             .resolve(txtName);
            }

            PathUtil.ensureParentDirectory(output);

            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(document);
                Files.writeString(output, text, StandardCharsets.UTF_8);
            }

            return "PDF 已成功转换为 TXT: " + output;
        });
    }

    // --- 5. pdf_merge ---

    /**
     * 合并多个PDF文件为一个PDF文件。
     *
     * @param sourceFilePaths 逗号分隔的PDF文件绝对路径列表
     * @param targetFilePath  合并后的输出文件路径
     * @return 成功消息或错误信息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "pdf_merge", description = "合并多个PDF文件。将多个PDF文件按顺序合并为一个新的PDF文件。")
    public String pdfMerge(@ToolParam(description = "逗号分隔的PDF文件绝对路径列表") String sourceFilePaths,
        @ToolParam(description = "合并后的输出文件路径") String targetFilePath) {
        return execute("pdf_merge", () -> {
            String[] paths = sourceFilePaths.split(",");
            if (paths.length < 2) {
                return "错误: 至少需要两个PDF文件路径才能合并";
            }

            PDFMergerUtility merger = new PDFMergerUtility();
            for (String p : paths) {
                String trimmed = p.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                Path sourcePath = validatePdfFile(trimmed);
                merger.addSource(sourcePath.toFile());
            }

            Path targetPath = Paths.get(targetFilePath);
            PathUtil.ensureParentDirectory(targetPath);
            merger.setDestinationFileName(targetPath.toString());
            merger.mergeDocuments(null);

            LogUtil.info("pdfMerge 成功，合并到: {}", targetFilePath);
            return "PDF 合并成功: " + targetFilePath;
        });
    }

    // --- 6. pdf_split ---

    /**
     * 拆分PDF文件，按页码范围拆分为多个文件。
     *
     * @param fileAbsolutePath PDF文件绝对路径
     * @param pageRanges       页码范围，如 "1-3,5,7-9"
     * @param outputDir        输出目录
     * @return 成功消息或错误信息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "pdf_split", description = "拆分PDF文件。按指定页码范围将PDF拆分为多个文件，如\"1-3,5,7-9\"。")
    public String pdfSplit(@ToolParam(description = "PDF文件绝对路径") String fileAbsolutePath,
        @ToolParam(description = "页码范围，如\"1-3,5,7-9\"") String pageRanges, @ToolParam(description = "输出目录") String outputDir) {
        return execute("pdf_split", () -> {
            Path path = validatePdfFile(fileAbsolutePath);
            Path dir = Paths.get(outputDir);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                int totalPages = document.getNumberOfPages();
                List<int[]> ranges = parsePageRanges(pageRanges, totalPages);

                List<String> outputFiles = new ArrayList<>();
                for (int[] range : ranges) {
                    int start = range[0];
                    int end = range[1];

                    String rangeName = start == end ? String.valueOf(start) : start + "-" + end;
                    String outputFileName = "split_" + rangeName + ".pdf";
                    Path outputPath = dir.resolve(outputFileName);

                    try (PDDocument subDoc = new PDDocument()) {
                        for (int i = start - 1; i < end; i++) {
                            subDoc.addPage(document.getPage(i));
                        }
                        subDoc.save(outputPath.toFile());
                    }
                    outputFiles.add(outputPath.toString());
                }

                LogUtil.info("pdfSplit 成功，生成 {} 个文件到: {}", outputFiles.size(), outputDir);
                return "PDF 拆分成功，生成 " + outputFiles.size() + " 个文件:\n" + String.join("\n", outputFiles);
            }
        });
    }

    // --- 7. pdf_create_from_text ---

    /**
     * 从文本内容生成PDF文件，支持中文文本。
     *
     * @param textContent    文本内容
     * @param targetFilePath 输出PDF文件路径
     * @param title          文档标题（可选）
     * @param fontSize       字体大小，默认12
     * @return 成功消息或错误信息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "pdf_create_from_text", description = "从文本内容生成PDF文件。支持中文文本，可指定标题和字体大小。")
    public String pdfCreateFromText(@ToolParam(description = "文本内容") String textContent,
        @ToolParam(description = "输出PDF文件路径") String targetFilePath, @ToolParam(description = "文档标题（可选）", required = false) String title,
        @ToolParam(description = "字体大小，默认12", required = false) Integer fontSize) {
        return execute("pdf_create_from_text", () -> {
            if (textContent == null || textContent.isBlank()) {
                throw new IllegalArgumentException("textContent 参数不能为空");
            }
            int fs = (fontSize != null && fontSize > 0) ? fontSize : 12;
            Path targetPath = Paths.get(targetFilePath);
            PathUtil.ensureParentDirectory(targetPath);

            try (PDDocument document = new PDDocument()) {
                PDFont font = loadChineseFont(document);
                if (font == null) {
                    LogUtil.warn("pdfCreateFromText 未找到中文字体，使用 HELVETICA 作为回退字体（中文文本将显示为乱码或空白）");
                    font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                }
                float margin = 50;
                float pageWidth = PDRectangle.A4.getWidth() - 2 * margin;
                float lineHeight = fs * 1.5f;

                // 预处理所有文本行
                String[] lines = textContent.split("\n");
                List<String> allLines = new ArrayList<>();
                for (String line : lines) {
                    allLines.addAll(wrapText(line, font, fs, pageWidth));
                }

                PDPage page = null;
                PDPageContentStream cs = null;
                float y = 0;
                boolean firstPage = true;

                for (int i = 0; i < allLines.size(); i++) {
                    if (page == null || y < margin + lineHeight) {
                        // 关闭上一个内容流
                        if (cs != null) {
                            cs.close();
                        }
                        // 创建新页面
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        cs = new PDPageContentStream(document, page);
                        y = PDRectangle.A4.getHeight() - margin;

                        // 首页绘制标题
                        if (firstPage && title != null && !title.isBlank()) {
                            cs.beginText();
                            cs.setFont(font, fs + 4);
                            cs.newLineAtOffset(margin, y);
                            cs.showText(title);
                            cs.endText();
                            y -= (fs + 4) * 1.5f;
                            firstPage = false;
                        }
                    }

                    cs.beginText();
                    cs.setFont(font, fs);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(allLines.get(i));
                    cs.endText();
                    y -= lineHeight;
                }

                if (cs != null) {
                    cs.close();
                }

                document.save(targetPath.toFile());
            }

            LogUtil.info("pdfCreateFromText 成功，输出到: {}", targetFilePath);
            return "PDF 创建成功: " + targetFilePath;
        });
    }

    // --- 8. pdf_extract_pages ---

    /**
     * 从PDF中提取指定页面，生成新的PDF文件。
     *
     * @param fileAbsolutePath PDF文件绝对路径
     * @param pageNumbers      页码，如 "1,3,5-7"
     * @param targetFilePath   输出文件路径
     * @return 成功消息或错误信息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "pdf_extract_pages", description = "提取PDF中的指定页面。从源PDF中提取指定页码的页面，生成新的PDF文件。")
    public String pdfExtractPages(@ToolParam(description = "PDF文件绝对路径") String fileAbsolutePath,
        @ToolParam(description = "页码，如\"1,3,5-7\"") String pageNumbers, @ToolParam(description = "输出文件路径") String targetFilePath) {
        return execute("pdf_extract_pages", () -> {
            Path path = validatePdfFile(fileAbsolutePath);
            Path targetPath = Paths.get(targetFilePath);
            PathUtil.ensureParentDirectory(targetPath);

            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                int totalPages = document.getNumberOfPages();
                List<int[]> ranges = parsePageRanges(pageNumbers, totalPages);

                try (PDDocument newDoc = new PDDocument()) {
                    for (int[] range : ranges) {
                        for (int i = range[0] - 1; i < range[1]; i++) {
                            newDoc.addPage(document.getPage(i));
                        }
                    }
                    newDoc.save(targetPath.toFile());
                }

                int pageCount = ranges.stream()
                                      .mapToInt(r -> r[1] - r[0] + 1)
                                      .sum();
                LogUtil.info("pdfExtractPages 成功，提取 {} 页到: {}", pageCount, targetFilePath);
                return "PDF 页面提取成功，共提取 " + pageCount + " 页: " + targetFilePath;
            }
        });
    }

    // --- 9. pdf_add_watermark ---

    /**
     * 为PDF文件添加水印文字。
     *
     * @param fileAbsolutePath PDF文件绝对路径
     * @param watermarkText    水印文字
     * @param targetFilePath   输出文件路径（可选，默认覆盖原文件）
     * @param fontSize         水印字体大小，默认48
     * @param opacity          透明度，默认0.3f
     * @param rotation         旋转角度，默认45度
     * @return 成功消息或错误信息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "pdf_add_watermark", description = "为PDF文件添加水印文字。支持自定义字体大小、透明度和旋转角度。")
    public String pdfAddWatermark(@ToolParam(description = "PDF文件绝对路径") String fileAbsolutePath,
        @ToolParam(description = "水印文字") String watermarkText,
        @ToolParam(description = "输出文件路径（可选，默认覆盖原文件）", required = false) String targetFilePath,
        @ToolParam(description = "水印字体大小，默认48", required = false) Integer fontSize,
        @ToolParam(description = "透明度(0~1)，默认0.3", required = false) Float opacity,
        @ToolParam(description = "旋转角度，默认45度", required = false) Integer rotation) {
        return execute("pdf_add_watermark", () -> {
            Path path = validatePdfFile(fileAbsolutePath);
            int fs = (fontSize != null && fontSize > 0) ? fontSize : 48;
            float alpha = (opacity != null) ? Math.max(0, Math.min(1, opacity)) : 0.3f;
            int angle = (rotation != null) ? rotation : 45;

            String outputPath = (targetFilePath != null && !targetFilePath.isBlank()) ? targetFilePath : fileAbsolutePath;
            Path targetPath = Paths.get(outputPath);
            PathUtil.ensureParentDirectory(targetPath);

            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                PDFont font = loadChineseFont(document);
                if (font == null) {
                    LogUtil.warn("pdfAddWatermark 未找到中文字体，使用 HELVETICA 作为回退字体（中文文本将显示为乱码或空白）");
                    font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                }

                for (PDPage page : document.getPages()) {
                    float pageWidth = page.getMediaBox()
                                          .getWidth();
                    float pageHeight = page.getMediaBox()
                                           .getHeight();
                    float centerX = pageWidth / 2;
                    float centerY = pageHeight / 2;

                    try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                        // 设置透明度
                        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                        gs.setNonStrokingAlphaConstant(alpha);
                        cs.setGraphicsStateParameters(gs);

                        // 设置水印颜色（灰色）
                        cs.setNonStrokingColor(new Color(0.5f, 0.5f, 0.5f));

                        cs.beginText();
                        cs.setFont(font, fs);
                        cs.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(angle), centerX, centerY));
                        cs.showText(watermarkText);
                        cs.endText();
                    }
                }

                document.save(targetPath.toFile());
            }

            LogUtil.info("pdfAddWatermark 成功，输出到: {}", outputPath);
            return "PDF 水印添加成功: " + outputPath;
        });
    }

    // --- 10. pdf_to_images ---

    /**
     * 将PDF页面转换为图片文件。
     *
     * @param fileAbsolutePath PDF文件绝对路径
     * @param pageNumbers      页码（可选，如"1,3-5"，默认全部页面）
     * @param outputDir        输出目录
     * @param imageFormat      图片格式，默认"png"，支持png/jpg
     * @param resolution       图片分辨率(DPI)，默认150
     * @return 成功消息或错误信息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "pdf_to_images", description = "将PDF页面转换为图片。支持指定页码、图片格式和分辨率。")
    public String pdfToImages(@ToolParam(description = "PDF文件绝对路径") String fileAbsolutePath,
        @ToolParam(description = "页码，如\"1,3-5\"（可选，默认全部页面）", required = false) String pageNumbers,
        @ToolParam(description = "输出目录") String outputDir,
        @ToolParam(description = "图片格式，默认\"png\"，支持png/jpg", required = false) String imageFormat,
        @ToolParam(description = "图片分辨率(DPI)，默认150", required = false) Integer resolution) {
        return execute("pdf_to_images", () -> {
            Path path = validatePdfFile(fileAbsolutePath);
            Path dir = Paths.get(outputDir);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String format = (imageFormat != null && !imageFormat.isBlank()) ? imageFormat.toLowerCase() : "png";
            if (!format.equals("png") && !format.equals("jpg") && !format.equals("jpeg")) {
                return "错误: 不支持的图片格式 '" + format + "'，仅支持 png 和 jpg";
            }
            // 统一 jpg/jpeg 为 jpg
            if (format.equals("jpeg")) {
                format = "jpg";
            }

            int dpi = (resolution != null && resolution > 0) ? resolution : 150;

            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                int totalPages = document.getNumberOfPages();
                List<int[]> ranges;
                if (pageNumbers != null && !pageNumbers.isBlank()) {
                    ranges = parsePageRanges(pageNumbers, totalPages);
                } else {
                    ranges = new ArrayList<>();
                    ranges.add(new int[]{1, totalPages});
                }

                PDFRenderer renderer = new PDFRenderer(document);
                List<String> outputFiles = new ArrayList<>();

                for (int[] range : ranges) {
                    for (int i = range[0] - 1; i < range[1]; i++) {
                        int pageNum = i + 1;
                        BufferedImage image = renderer.renderImageWithDPI(i, dpi);

                        String outputFileName = String.format("page_%d.%s", pageNum, format);
                        Path outputPath = dir.resolve(outputFileName);

                        ImageIO.write(image, format, outputPath.toFile());
                        outputFiles.add(outputPath.toString());
                    }
                }

                LogUtil.info("pdfToImages 成功，生成 {} 张图片到: {}", outputFiles.size(), outputDir);
                return "PDF 转图片成功，生成 " + outputFiles.size() + " 张图片:\n" + String.join("\n", outputFiles);
            }
        });
    }

    // ======================== 辅助方法 ========================

    /**
     * 解析页码范围字符串，如 "1-3,5,7-9"。
     *
     * @param pageRanges 页码范围字符串
     * @param totalPages 总页数，用于校验范围
     * @return 页码范围列表，每个元素为 [startPage, endPage]
     * @throws IllegalArgumentException 页码超出范围时抛出
     */
    private List<int[]> parsePageRanges(String pageRanges, int totalPages) {
        List<int[]> ranges = new ArrayList<>();
        String[] parts = pageRanges.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.contains("-")) {
                String[] rangeParts = trimmed.split("-");
                int start = Integer.parseInt(rangeParts[0].trim());
                int end = Integer.parseInt(rangeParts[1].trim());
                if (start < 1 || end > totalPages || start > end) {
                    throw new IllegalArgumentException(String.format("页码范围 %d-%d 无效，PDF 共 %d 页", start, end, totalPages));
                }
                ranges.add(new int[]{start, end});
            } else {
                int page = Integer.parseInt(trimmed);
                if (page < 1 || page > totalPages) {
                    throw new IllegalArgumentException(String.format("页码 %d 无效，PDF 共 %d 页", page, totalPages));
                }
                ranges.add(new int[]{page, page});
            }
        }
        return ranges;
    }

    /**
     * 文本换行，根据字体和最大宽度计算需要换行的行列表。
     *
     * @param text     原始文本
     * @param font     字体
     * @param fontSize 字体大小
     * @param maxWidth 最大宽度
     * @return 换行后的文本行列表
     * @throws IOException 字体字符串宽度计算失败时抛出
     */
    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            result.add("");
            return result;
        }

        StringBuilder currentLine = new StringBuilder();
        for (char c : text.toCharArray()) {
            String testLine = currentLine.toString() + c;
            float width = font.getStringWidth(testLine) / 1000 * fontSize;
            if (width > maxWidth && currentLine.length() > 0) {
                result.add(currentLine.toString());
                currentLine = new StringBuilder();
            }
            currentLine.append(c);
        }
        if (currentLine.length() > 0) {
            result.add(currentLine.toString());
        }
        return result;
    }

    /**
     * 加载中文字体，优先尝试系统常见中文字体路径，再扫描字体目录，最后使用 GraphicsEnvironment 兜底。
     *
     * @param document PDF文档对象
     * @return 加载到的字体，如果找不到中文字体则返回 null
     */
    private PDFont loadChineseFont(PDDocument document) {
        // 0. 优先尝试用户配置的自定义字体路径
        if (configuredFontPaths != null && !configuredFontPaths.isBlank()) {
            String[] customPaths = configuredFontPaths.split(",");
            for (String fontPath : customPaths) {
                String trimmed = fontPath.trim();
                if (!trimmed.isEmpty()) {
                    File fontFile = new File(trimmed);
                    if (fontFile.exists()) {
                        try {
                            LogUtil.info("使用自定义字体路径: {}", trimmed);
                            return PDType0Font.load(document, fontFile);
                        } catch (IOException e) {
                            LogUtil.warn("自定义字体加载失败: {}", trimmed);
                        }
                    } else {
                        LogUtil.warn("自定义字体文件不存在: {}", trimmed);
                    }
                }
            }
        }

        // 1. 尝试常见中文字体路径（跨平台）
        String[] specificFontPaths = {
            // Windows
            "C:/Windows/Fonts/simsun.ttc", "C:/Windows/Fonts/msyh.ttc", "C:/Windows/Fonts/simhei.ttf", "C:/Windows/Fonts/msyhbd.ttc",
            "C:/Windows/Fonts/msyh.ttf", "C:/Windows/Fonts/simsun.ttf",
            // Linux
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc", "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf", "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc", "/usr/share/fonts/truetype/noto/NotoSerifCJK-Regular.ttc",
            // macOS
            "/System/Library/Fonts/PingFang.ttc", "/System/Library/Fonts/STHeiti Light.ttc", "/System/Library/Fonts/STHeiti Medium.ttc",
            "/Library/Fonts/Arial Unicode.ttf",};

        for (String fontPath : specificFontPaths) {
            File fontFile = new File(fontPath);
            if (fontFile.exists()) {
                try {
                    return PDType0Font.load(document, fontFile);
                } catch (IOException e) {
                    // 尝试下一个字体
                }
            }
        }

        // 2. 扫描常见字体目录，查找中文字体文件（Linux/macOS）
        String[] searchDirs = {"/usr/share/fonts/", "/usr/local/share/fonts/", System.getProperty("user.home") + "/.fonts/", "/System/Library/Fonts/",
            "/Library/Fonts/",};

        for (String dirPath : searchDirs) {
            File dir = new File(dirPath);
            if (dir.exists() && dir.isDirectory()) {
                PDFont font = scanFontDirectory(document, dir, 0, 3);
                if (font != null) {
                    return font;
                }
            }
        }

        // 3. 使用 GraphicsEnvironment 查找系统可用中文字体作为兜底
        PDFont geFont = findChineseFontViaGraphicsEnvironment(document);
        if (geFont != null) {
            return geFont;
        }

        // 4. 所有字体都找不到，返回 null 由调用方做降级处理
        LogUtil.warn("未找到中文字体文件，中文文本将无法正常显示，请安装中文字体（如 WenQuanYi Micro Hei 或 Noto Sans CJK）");
        return null;
    }

    /**
     * 递归扫描字体目录，查找中文字体文件。
     *
     * @param document PDF文档对象
     * @param dir      当前扫描目录
     * @param depth    当前递归深度
     * @param maxDepth 最大递归深度
     * @return 找到的字体，未找到返回 null
     */
    private PDFont scanFontDirectory(PDDocument document, File dir, int depth, int maxDepth) {
        if (depth > maxDepth || !dir.isDirectory()) {
            return null;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                PDFont font = scanFontDirectory(document, file, depth + 1, maxDepth);
                if (font != null) {
                    return font;
                }
            } else {
                String name = file.getName()
                                  .toLowerCase();
                if (name.endsWith(".ttf") || name.endsWith(".ttc")) {
                    try {
                        return PDType0Font.load(document, file);
                    } catch (IOException e) {
                        // 继续尝试下一个
                    }
                }
            }
        }
        return null;
    }

    /**
     * 使用 GraphicsEnvironment 查找系统注册的中文字体，并尝试加载对应的字体文件。
     *
     * @param document PDF文档对象
     * @return 找到的字体，未找到返回 null
     */
    private PDFont findChineseFontViaGraphicsEnvironment(PDDocument document) {
        try {
            String[] fontFamilyNames = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                                          .getAvailableFontFamilyNames();

            String[] chineseKeywords = {"SimSun", "Microsoft YaHei", "SimHei", "PingFang", "STHeiti", "WenQuanYi", "Noto Sans CJK", "Noto Serif CJK",
                "Source Han Sans", "Source Han Serif", "Droid Sans Fallback", "AR PL UMing", "AR PL UKai", "WenQuanYi Micro Hei",
                "WenQuanYi Zen Hei"};

            // 检查系统中是否有中文字体注册
            boolean hasChineseFont = false;
            String matchedFontName = null;
            for (String name : fontFamilyNames) {
                for (String kw : chineseKeywords) {
                    if (name.contains(kw) || name.equalsIgnoreCase(kw)) {
                        hasChineseFont = true;
                        matchedFontName = name;
                        break;
                    }
                }
                if (hasChineseFont) {
                    break;
                }
            }

            if (hasChineseFont) {
                LogUtil.info("系统已注册中文字体: {}", matchedFontName);
                // 在常见字体目录中查找匹配的字体文件
                String[] searchDirs = {"/usr/share/fonts/", "/usr/local/share/fonts/", System.getProperty("user.home") + "/.fonts/",
                    "/System/Library/Fonts/", "/Library/Fonts/", "C:/Windows/Fonts/",};
                for (String dirPath : searchDirs) {
                    File dir = new File(dirPath);
                    if (dir.exists() && dir.isDirectory()) {
                        PDFont font = scanFontDirectory(document, dir, 0, 3);
                        if (font != null) {
                            return font;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // GraphicsEnvironment 不可用时忽略
        }
        return null;
    }

    // ==================== P2 工具 ====================

    // --- 11. pdf_rotate_page ---

    /**
     * 旋转PDF指定页面。
     *
     * @param fileAbsolutePath PDF文件的绝对路径
     * @param pageNumber       要旋转的页码（从1开始）
     * @param rotation         旋转角度，必须是90/180/270
     * @param targetFilePath   输出文件路径（可选，默认覆盖原文件）
     * @return 操作结果消息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "pdf_rotate_page", description = "旋转PDF指定页面。支持90度、180度、270度旋转，默认覆盖原文件。")
    public String pdfRotatePage(@ToolParam(description = "PDF文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "要旋转的页码（从1开始）") int pageNumber, @ToolParam(description = "旋转角度，必须是90/180/270") int rotation,
        @ToolParam(description = "输出文件路径（可选，默认覆盖原文件）", required = false) String targetFilePath) {
        return execute("pdf_rotate_page", () -> {
            if (rotation != 90 && rotation != 180 && rotation != 270) {
                return "错误: 旋转角度必须是 90、180 或 270，当前值: " + rotation;
            }

            Path path = validatePdfFile(fileAbsolutePath);
            String outputPath = (targetFilePath != null && !targetFilePath.isBlank()) ? targetFilePath : fileAbsolutePath;
            Path targetPath = Paths.get(outputPath);
            PathUtil.ensureParentDirectory(targetPath);

            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                int totalPages = document.getNumberOfPages();
                if (pageNumber < 1 || pageNumber > totalPages) {
                    return String.format("错误: 页码 %d 超出范围，PDF 共 %d 页", pageNumber, totalPages);
                }

                PDPage page = document.getPage(pageNumber - 1);
                page.setRotation(rotation);
                document.save(targetPath.toFile());
            }

            LogUtil.info("pdfRotatePage 成功，页码: {}, 旋转: {}°, 输出: {}", pageNumber, rotation, outputPath);
            return String.format("PDF 第 %d 页已旋转 %d°，输出: %s", pageNumber, rotation, outputPath);
        });
    }

    // --- 12. pdf_extract_images ---

    /**
     * 提取PDF中嵌入的图片，保存为PNG文件到指定目录。
     *
     * @param fileAbsolutePath PDF文件的绝对路径
     * @param outputDir        图片输出目录
     * @param pageNumber       指定页码（从1开始，可选，不传则提取所有页面）
     * @return 操作结果消息，包含提取的图片数量和文件列表
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "pdf_extract_images", description = "提取PDF中嵌入的图片，保存为PNG文件。可指定页码，不传则提取所有页面。")
    public String pdfExtractImages(@ToolParam(description = "PDF文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "图片输出目录") String outputDir,
        @ToolParam(description = "指定页码（从1开始，可选，不传则提取所有页面）", required = false) Integer pageNumber) {
        return execute("pdf_extract_images", () -> {
            Path path = validatePdfFile(fileAbsolutePath);
            Path dir = Paths.get(outputDir);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                int totalPages = document.getNumberOfPages();
                List<String> outputFiles = new ArrayList<>();
                int count = 0;

                List<PDPage> pagesToProcess = new ArrayList<>();
                if (pageNumber != null) {
                    if (pageNumber < 1 || pageNumber > totalPages) {
                        return String.format("错误: 页码 %d 超出范围，PDF 共 %d 页", pageNumber, totalPages);
                    }
                    pagesToProcess.add(document.getPage(pageNumber - 1));
                } else {
                    for (PDPage page : document.getPages()) {
                        pagesToProcess.add(page);
                    }
                }

                for (int i = 0; i < pagesToProcess.size(); i++) {
                    PDPage page = pagesToProcess.get(i);
                    int pageIdx = (pageNumber != null) ? pageNumber : (i + 1);
                    PDResources resources = page.getResources();
                    if (resources == null) {
                        continue;
                    }
                    for (COSName name : resources.getXObjectNames()) {
                        PDXObject xobj = resources.getXObject(name);
                        if (xobj instanceof PDImageXObject image) {
                            count++;
                            BufferedImage bimg = image.getImage();
                            String fileName = String.format("page_%d_image_%d.png", pageIdx, count);
                            File outputFile = new File(dir.toFile(), fileName);
                            ImageIO.write(bimg, "PNG", outputFile);
                            outputFiles.add(outputFile.getAbsolutePath());
                        }
                    }
                }

                if (count == 0) {
                    return "未在PDF中找到嵌入的图片";
                }

                LogUtil.info("pdfExtractImages 成功，提取 {} 张图片到: {}", count, outputDir);
                StringBuilder sb = new StringBuilder();
                sb.append("成功提取 ")
                  .append(count)
                  .append(" 张图片:\n");
                for (String file : outputFiles) {
                    sb.append("  ")
                      .append(file)
                      .append("\n");
                }
                return sb.toString()
                         .trim();
            }
        });
    }
}