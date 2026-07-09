package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * TxtHandler 单元测试
 *
 * @author FrankKang
 * @since 2026-07-09
 */
@SpringBootTest
class TxtHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private TxtHandler txtHandler;
    private Path testFile;

    @BeforeEach
    void setUp() {
        testFile = tempDir.resolve("test.txt");
    }

    @Test
    void testCreateTxtFile() {
        String result = txtHandler.createTxtFile(testFile.toString());
        assertTrue(result.contains("空白 TXT 文件已创建"));
        assertTrue(Files.exists(testFile));
    }

    @Test
    void testWriteAndReadFull() {
        txtHandler.writeTxt(testFile.toString(), "Hello World");
        String result = txtHandler.readTxtFull(testFile.toString());
        assertTrue(result.contains("Hello World"));
    }

    @Test
    void testReadTxtLines() {
        txtHandler.writeTxt(testFile.toString(), "Line1\nLine2\nLine3");
        String result = txtHandler.readTxtLines(testFile.toString(), 1, 2);
        assertTrue(result.contains("Line1"));
        assertTrue(result.contains("Line2"));
    }

    @Test
    void testReadTxtSpecificLines() {
        txtHandler.writeTxt(testFile.toString(), "A\nB\nC\nD\nE");
        String result = txtHandler.readTxtSpecificLines(testFile.toString(), "1,3,5");
        assertTrue(result.contains("A"));
        assertTrue(result.contains("C"));
        assertTrue(result.contains("E"));
    }

    @Test
    void testAppendTxt() {
        txtHandler.writeTxt(testFile.toString(), "First");
        txtHandler.appendTxt(testFile.toString(), "\nSecond");
        String result = txtHandler.readTxtFull(testFile.toString());
        assertTrue(result.contains("First"));
        assertTrue(result.contains("Second"));
    }

    @Test
    void testClearTxt() {
        txtHandler.writeTxt(testFile.toString(), "content");
        txtHandler.clearTxt(testFile.toString());
        String result = txtHandler.readTxtFull(testFile.toString());
        assertEquals("", result);
    }

    @Test
    void testSearchTxt() {
        txtHandler.writeTxt(testFile.toString(), "Hello World\nGoodbye World");
        String result = txtHandler.searchTxt(testFile.toString(), "World", null);
        assertTrue(result.contains("World"));
    }

    @Test
    void testReplaceTxt() {
        txtHandler.writeTxt(testFile.toString(), "Hello World");
        String result = txtHandler.replaceTxt(testFile.toString(), "World", "Java");
        assertTrue(result.contains("替换成功"));
        String content = txtHandler.readTxtFull(testFile.toString());
        assertTrue(content.contains("Java"));
    }
}