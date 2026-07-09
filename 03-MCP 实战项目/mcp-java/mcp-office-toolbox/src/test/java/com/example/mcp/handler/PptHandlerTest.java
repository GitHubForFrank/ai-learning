package com.example.mcp.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PptHandler 单元测试
 *
 * @author FrankKang
 * @since 2026-07-09
 */
@SpringBootTest
class PptHandlerTest {

    @Autowired
    private PptHandler pptHandler;

    @TempDir
    Path tempDir;

    private Path testPpt;

    @BeforeEach
    void setUp() {
        testPpt = tempDir.resolve("test.pptx");
    }

    @Test
    void testCreatePpt() {
        String result = pptHandler.createPpt(testPpt.toString());
        assertTrue(result.contains("空白 PPT 文件已创建"));
        assertTrue(Files.exists(testPpt));
    }

    @Test
    void testAddPptSlide() {
        pptHandler.createPpt(testPpt.toString());
        String result = pptHandler.addPptSlide(testPpt.toString(), "New Slide");
        assertTrue(result.contains("已添加"));
        assertTrue(Files.exists(testPpt));
    }

    @Test
    void testReadPptText() {
        pptHandler.createPpt(testPpt.toString());
        pptHandler.addPptSlide(testPpt.toString(), "Slide Title");
        String result = pptHandler.readPptText(testPpt.toString());
        assertNotNull(result);
    }

    @Test
    void testDeletePptSlide() {
        pptHandler.createPpt(testPpt.toString());
        pptHandler.addPptSlide(testPpt.toString(), "To Delete");
        String result = pptHandler.deletePptSlide(testPpt.toString(), 1);
        assertTrue(result.contains("已删除"));
    }

    @Test
    void testSavePptAs() {
        pptHandler.createPpt(testPpt.toString());
        Path target = tempDir.resolve("copy.pptx");
        String result = pptHandler.savePptAs(testPpt.toString(), target.toString());
        assertTrue(result.contains("已另存为"));
        assertTrue(Files.exists(target));
    }
}