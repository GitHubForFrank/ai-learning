package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.tool.BarcodeHandler;
import com.example.mcp.pojo.barcode.BarcodeGenerateRequest;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * BarcodeHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class BarcodeHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private BarcodeHandler barcodeHandler;

    @Test
    void testBarcodeGenerateEan13() {
        BarcodeGenerateRequest req = new BarcodeGenerateRequest("1234567890128", tempDir.resolve("barcode.png")
                                                                                        .toString(), "EAN_13", null, null, null);
        String result = barcodeHandler.barcodeGenerate(req);
        assertNotNull(result);
        assertTrue(result.contains("条形码"));
    }

    @Test
    void testBarcodeGenerateCode128() {
        BarcodeGenerateRequest req = new BarcodeGenerateRequest("ABC123", tempDir.resolve("code128.png")
                                                                                 .toString(), "CODE_128", null, null, null);
        String result = barcodeHandler.barcodeGenerate(req);
        assertNotNull(result);
    }

    /**
     * 测试空内容参数，预期返回错误信息
     */
    @Test
    void testBarcodeGenerateEmptyContent() {
        BarcodeGenerateRequest req = new BarcodeGenerateRequest("", tempDir.resolve("barcode.png")
                                                                           .toString(), "CODE_128", null, null, null);
        String result = barcodeHandler.barcodeGenerate(req);
        assertNotNull(result);
        assertTrue(result.contains("错误"));
    }

    /**
     * 测试空目标路径参数，预期返回错误信息
     */
    @Test
    void testBarcodeGenerateEmptyTargetPath() {
        BarcodeGenerateRequest req = new BarcodeGenerateRequest("1234567890128", "", "CODE_128", null, null, null);
        String result = barcodeHandler.barcodeGenerate(req);
        assertNotNull(result);
        assertTrue(result.contains("错误"));
    }

    /**
     * 测试无效的条形码类型与内容不匹配（EAN_13 要求纯数字，传入字母内容），预期返回错误信息
     */
    @Test
    void testBarcodeGenerateInvalidTypeContentMismatch() {
        BarcodeGenerateRequest req = new BarcodeGenerateRequest("ABC", tempDir.resolve("barcode.png")
                                                                              .toString(), "EAN_13", null, null, null);
        String result = barcodeHandler.barcodeGenerate(req);
        assertNotNull(result);
        assertTrue(result.contains("错误"));
    }
}
