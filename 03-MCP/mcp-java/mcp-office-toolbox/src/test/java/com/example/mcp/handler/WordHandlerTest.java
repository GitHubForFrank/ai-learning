package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.word.WordHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * WordHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@SpringBootTest
class WordHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private WordHandler wordHandler;
    private Path testDoc;

    @BeforeEach
    void setUp() {
        testDoc = tempDir.resolve("test.docx");
    }

    @Test
    void testCreateWordDoc() {
        String result = wordHandler.createWordDoc(testDoc.toString());
        assertTrue(result.contains("空白 Word 文档已创建"));
        assertTrue(Files.exists(testDoc));
    }

    @Test
    void testInsertAndReadWordText() {
        wordHandler.createWordDoc(testDoc.toString());
        wordHandler.insertWordText(testDoc.toString(), "Hello Word");
        String result = wordHandler.readWordFull(testDoc.toString());
        assertTrue(result.contains("Hello Word"));
    }

    @Test
    void testInsertWordHeading() {
        wordHandler.createWordDoc(testDoc.toString());
        String result = wordHandler.insertWordHeading(testDoc.toString(), 1, "Main Title");
        assertTrue(result.contains("标题"));
        assertTrue(Files.exists(testDoc));
    }

    @Test
    void testInsertWordParagraph() {
        wordHandler.createWordDoc(testDoc.toString());
        String result = wordHandler.insertWordParagraph(testDoc.toString(), "Custom paragraph", true, false, 14, null);
        assertTrue(result.contains("自定义段落"));
        assertTrue(Files.exists(testDoc));
    }

    @Test
    void testModifyWordContent() {
        wordHandler.createWordDoc(testDoc.toString());
        wordHandler.insertWordText(testDoc.toString(), "Hello World");
        String result = wordHandler.modifyWordContent(testDoc.toString(), "World", "Java");
        assertTrue(result.contains("内容修改完成"));
    }

    @Test
    void testSaveWordAs() {
        wordHandler.createWordDoc(testDoc.toString());
        Path target = tempDir.resolve("copy.docx");
        String result = wordHandler.saveWordAs(testDoc.toString(), target.toString());
        assertTrue(result.contains("已另存为"));
        assertTrue(Files.exists(target));
    }

    @Test
    void testReadWordParagraphs() {
        wordHandler.createWordDoc(testDoc.toString());
        wordHandler.insertWordText(testDoc.toString(), "Paragraph 1");
        String result = wordHandler.readWordParagraphs(testDoc.toString());
        assertNotNull(result);
    }
}