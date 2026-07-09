package com.example.mcp.handler;

import com.example.mcp.util.FileValidateUtil;
import com.example.mcp.util.PathUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP PDF 工具实现，提供 PDF 文档的读取、解析、元信息获取和文本转换功能。
 * 设计原则：轻量稳定，不依赖重型 PDF 编辑器，无高危编辑功能，定位只读解析。
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@Service
public class PdfHandler {

    private static final Logger log = LoggerFactory.getLogger(PdfHandler.class);

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
    public String readPdfText(
            @ToolParam(description = "Absolute path to the PDF file") String fileAbsolutePath) {
        try {
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
        } catch (Exception e) {
            log.error("readPdfText 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 2. read_pdf_page ---

    /**
     * 按页码单独读取指定页面文本
     */
    @Tool(name = "read_pdf_page", description = "读取 PDF 文件指定页面的文本内容。页码从 1 开始。")
    public String readPdfPage(
            @ToolParam(description = "Absolute path to the PDF file") String fileAbsolutePath,
            @ToolParam(description = "Page number to read (1-based)") int pageNumber) {
        try {
            Path path = validatePdfFile(fileAbsolutePath);
            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                int totalPages = document.getNumberOfPages();
                if (pageNumber < 1 || pageNumber > totalPages) {
                    return String.format("错误：页码 %d 超出范围，PDF 共 %d 页", pageNumber, totalPages);
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
        } catch (Exception e) {
            log.error("readPdfPage 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 3. get_pdf_info ---

    /**
     * 获取 PDF 总页数、文档基础元信息
     */
    @Tool(name = "get_pdf_info", description = "获取 PDF 文档信息，包括总页数、标题、作者、主题、关键词、创建者、生成器、创建日期和修改日期。")
    public String getPdfInfo(
            @ToolParam(description = "Absolute path to the PDF file") String fileAbsolutePath) {
        try {
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
                info.put("creationDate", docInfo.getCreationDate() != null
                        ? docInfo.getCreationDate().getTime().toString()
                        : null);
                info.put("modificationDate", docInfo.getModificationDate() != null
                        ? docInfo.getModificationDate().getTime().toString()
                        : null);

                // 判断是否加密
                info.put("isEncrypted", document.isEncrypted());

                StringBuilder sb = new StringBuilder();
                sb.append("PDF 文件信息：\n");
                for (Map.Entry<String, Object> entry : info.entrySet()) {
                    sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.error("getPdfInfo 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 4. convert_pdf_to_txt ---

    /**
     * PDF 文件批量转换为纯 TXT 文本
     */
    @Tool(name = "convert_pdf_to_txt", description = "将 PDF 文件转换为纯文本 TXT 文件。提取所有文本内容并保存到指定输出路径。")
    public String convertPdfToTxt(
            @ToolParam(description = "Absolute path to the PDF file") String fileAbsolutePath,
            @ToolParam(description = "Output path for the TXT file (optional, defaults to same directory as PDF)", required = false) String outputPath) {
        try {
            Path path = validatePdfFile(fileAbsolutePath);
            Path output;
            if (outputPath != null && !outputPath.isBlank()) {
                output = Paths.get(outputPath);
            } else {
                // 默认输出到 PDF 同目录，同名但扩展名为 .txt
                String pdfName = path.getFileName().toString();
                String txtName = pdfName.substring(0, pdfName.lastIndexOf('.')) + ".txt";
                output = path.getParent().resolve(txtName);
            }

            // 确保输出目录存在
            Path outputParent = output.getParent();
            if (outputParent != null && !Files.exists(outputParent)) {
                Files.createDirectories(outputParent);
            }

            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(document);
                Files.writeString(output, text, StandardCharsets.UTF_8);
            }

            return "PDF 已成功转换为 TXT: " + output;
        } catch (Exception e) {
            log.error("convertPdfToTxt 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }
}