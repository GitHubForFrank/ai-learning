package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.pdf.PdfHandler;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * PdfHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@SpringBootTest
class PdfHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private PdfHandler pdfHandler;

    @Test
    void testReadPdfTextFileNotFound() {
        String result = pdfHandler.readPdfText(tempDir.resolve("nonexistent.pdf")
                                                      .toString());
        assertTrue(result.startsWith("错误: 文件不存在"));
    }

    @Test
    void testGetPdfInfoFileNotFound() {
        String result = pdfHandler.getPdfInfo(tempDir.resolve("nonexistent.pdf")
                                                     .toString());
        assertTrue(result.startsWith("错误: 文件不存在"));
    }

    @Test
    void testReadPdfPageFileNotFound() {
        String result = pdfHandler.readPdfPage(tempDir.resolve("nonexistent.pdf")
                                                      .toString(), 1);
        assertTrue(result.startsWith("错误: 文件不存在"));
    }

    @Test
    void testConvertPdfToTxtFileNotFound() {
        String result = pdfHandler.convertPdfToTxt(tempDir.resolve("nonexistent.pdf")
                                                          .toString(), null);
        assertTrue(result.startsWith("错误: 文件不存在"));
    }

    /**
     * 测试正常创建 PDF 并读取文本内容
     */
    @Test
    void testCreatePdfAndReadText() {
        Path pdfPath = tempDir.resolve("test_read.pdf");
        pdfHandler.pdfCreateFromText("Hello World 测试PDF内容", pdfPath.toString(), "Test Title", 12);
        String result = pdfHandler.readPdfText(pdfPath.toString());
        assertNotNull(result);
        assertTrue(result.contains("Hello World"));
    }

    /**
     * 测试正常读取 PDF 页数信息
     */
    @Test
    void testGetPdfInfoSuccess() {
        Path pdfPath = tempDir.resolve("test_info.pdf");
        pdfHandler.pdfCreateFromText("Page content for info test", pdfPath.toString(), null, null);
        String result = pdfHandler.getPdfInfo(pdfPath.toString());
        assertNotNull(result);
        assertTrue(result.contains("totalPages"));
    }

    /**
     * 测试正常读取 PDF 指定页面文本
     */
    @Test
    void testReadPdfPageSuccess() {
        Path pdfPath = tempDir.resolve("test_page.pdf");
        pdfHandler.pdfCreateFromText("First page text content", pdfPath.toString(), null, null);
        String result = pdfHandler.readPdfPage(pdfPath.toString(), 1);
        assertNotNull(result);
        assertTrue(result.contains("First page"));
    }
}