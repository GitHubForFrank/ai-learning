package com.example.mcp.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PdfHandler 单元测试
 *
 * @author FrankKang
 * @since 2026-07-09
 */
@SpringBootTest
class PdfHandlerTest {

    @Autowired
    private PdfHandler pdfHandler;

    @TempDir
    Path tempDir;

    @Test
    void testReadPdfTextFileNotFound() {
        String result = pdfHandler.readPdfText(tempDir.resolve("nonexistent.pdf").toString());
        assertTrue(result.contains("错误") || result.contains("文件不存在"));
    }

    @Test
    void testGetPdfInfoFileNotFound() {
        String result = pdfHandler.getPdfInfo(tempDir.resolve("nonexistent.pdf").toString());
        assertTrue(result.contains("错误") || result.contains("文件不存在"));
    }

    @Test
    void testReadPdfPageFileNotFound() {
        String result = pdfHandler.readPdfPage(tempDir.resolve("nonexistent.pdf").toString(), 1);
        assertTrue(result.contains("错误") || result.contains("文件不存在"));
    }

    @Test
    void testConvertPdfToTxtFileNotFound() {
        String result = pdfHandler.convertPdfToTxt(tempDir.resolve("nonexistent.pdf").toString(), null);
        assertTrue(result.contains("错误") || result.contains("文件不存在"));
    }
}