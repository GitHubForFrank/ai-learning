package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.tool.QrCodeHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * QrCodeHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class QrCodeHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private QrCodeHandler qrCodeHandler;

    // ==================== qrCodeGenerate 测试 ====================

    @Test
    void testQrCodeGenerateSuccess() {
        Path targetPath = tempDir.resolve("test_qr.png");
        String content = "https://example.com/hello";
        String result = qrCodeHandler.qrCodeGenerate(content, targetPath.toString(), null, null, null);
        assertTrue(result.contains("二维码生成成功"));
        assertTrue(result.contains(content));
        assertTrue(Files.exists(targetPath));
    }

    @Test
    void testQrCodeGenerateNullContent() {
        Path targetPath = tempDir.resolve("null_content.png");
        String result = qrCodeHandler.qrCodeGenerate(null, targetPath.toString(), null, null, null);
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("内容不能为空"));
    }

    @Test
    void testQrCodeGenerateEmptyContent() {
        Path targetPath = tempDir.resolve("empty_content.png");
        String result = qrCodeHandler.qrCodeGenerate("", targetPath.toString(), null, null, null);
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("内容不能为空"));
    }

    @Test
    void testQrCodeGenerateBlankContent() {
        Path targetPath = tempDir.resolve("blank_content.png");
        String result = qrCodeHandler.qrCodeGenerate("   ", targetPath.toString(), null, null, null);
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("内容不能为空"));
    }

    @Test
    void testQrCodeGenerateInvalidFormat() {
        Path targetPath = tempDir.resolve("test_qr.bmp");
        String result = qrCodeHandler.qrCodeGenerate("test content", targetPath.toString(), null, null, "bmp");
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("不支持的图片格式"));
    }

    @Test
    void testQrCodeGenerateNullTargetPath() {
        String result = qrCodeHandler.qrCodeGenerate("test content", null, null, null, null);
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("输出路径不能为空"));
    }

    @Test
    void testQrCodeGenerateEmptyTargetPath() {
        String result = qrCodeHandler.qrCodeGenerate("test content", "", null, null, null);
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("输出路径不能为空"));
    }

    @Test
    void testQrCodeGenerateWithCustomSize() {
        Path targetPath = tempDir.resolve("custom_size.png");
        String content = "Custom size QR";
        String result = qrCodeHandler.qrCodeGenerate(content, targetPath.toString(), 500, 500, null);
        assertTrue(result.contains("二维码生成成功"));
        assertTrue(result.contains("500x500"));
        assertTrue(Files.exists(targetPath));
    }

    @Test
    void testQrCodeGenerateWithJpgFormat() {
        Path targetPath = tempDir.resolve("test_qr.jpg");
        String content = "JPG format test";
        String result = qrCodeHandler.qrCodeGenerate(content, targetPath.toString(), null, null, "jpg");
        assertTrue(result.contains("二维码生成成功"));
        assertTrue(Files.exists(targetPath));
    }

    @Test
    void testQrCodeGenerateWithLongContent() {
        Path targetPath = tempDir.resolve("long_content.png");
        String content = "A".repeat(100);
        String result = qrCodeHandler.qrCodeGenerate(content, targetPath.toString(), null, null, null);
        assertTrue(result.contains("二维码生成成功"));
        assertTrue(result.contains("..."));
        assertTrue(Files.exists(targetPath));
    }

    // ==================== qrCodeRead 测试 ====================

    @Test
    void testQrCodeReadSuccess() {
        Path targetPath = tempDir.resolve("read_test.png");
        String content = "Hello QR Code";
        qrCodeHandler.qrCodeGenerate(content, targetPath.toString(), null, null, null);

        String result = qrCodeHandler.qrCodeRead(targetPath.toString());
        assertEquals(content, result);
    }

    @Test
    void testQrCodeReadNonExistentFile() {
        String result = qrCodeHandler.qrCodeRead(tempDir.resolve("nonexistent.png")
                                                        .toString());
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("文件不存在"));
    }

    @Test
    void testQrCodeReadNullPath() {
        String result = qrCodeHandler.qrCodeRead(null);
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("图片路径不能为空"));
    }

    @Test
    void testQrCodeReadEmptyPath() {
        String result = qrCodeHandler.qrCodeRead("");
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("图片路径不能为空"));
    }

    // ==================== 生成并读取集成测试 ====================

    @Test
    void testGenerateAndReadRoundTrip() {
        Path targetPath = tempDir.resolve("roundtrip.png");
        String originalContent = "https://github.com/example/project";

        // 生成二维码
        String generateResult = qrCodeHandler.qrCodeGenerate(originalContent, targetPath.toString(), null, null, null);
        assertTrue(generateResult.contains("二维码生成成功"));
        assertTrue(Files.exists(targetPath));

        // 读取二维码
        String readResult = qrCodeHandler.qrCodeRead(targetPath.toString());
        assertEquals(originalContent, readResult);
    }

    @Test
    void testGenerateAndReadWithChineseContent() {
        Path targetPath = tempDir.resolve("chinese_qr.png");
        String originalContent = "你好，世界！Hello World!";

        // 生成二维码
        String generateResult = qrCodeHandler.qrCodeGenerate(originalContent, targetPath.toString(), null, null, null);
        assertTrue(generateResult.contains("二维码生成成功"));
        assertTrue(Files.exists(targetPath));

        // 读取二维码
        String readResult = qrCodeHandler.qrCodeRead(targetPath.toString());
        assertEquals(originalContent, readResult);
    }

    @Test
    void testGenerateAndReadWithUrl() {
        Path targetPath = tempDir.resolve("url_qr.png");
        String originalContent = "https://www.example.com/path?key=value&foo=bar";

        // 生成二维码
        String generateResult = qrCodeHandler.qrCodeGenerate(originalContent, targetPath.toString(), null, null, null);
        assertTrue(generateResult.contains("二维码生成成功"));
        assertTrue(Files.exists(targetPath));

        // 读取二维码
        String readResult = qrCodeHandler.qrCodeRead(targetPath.toString());
        assertEquals(originalContent, readResult);
    }
}