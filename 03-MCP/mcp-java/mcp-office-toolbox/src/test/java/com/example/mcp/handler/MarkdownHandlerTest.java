package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.convert.MarkdownHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * MarkdownHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@SpringBootTest
class MarkdownHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private MarkdownHandler markdownHandler;
    private Path testFile;

    @BeforeEach
    void setUp() {
        testFile = tempDir.resolve("test.md");
    }

    @Test
    void testCreateMdFile() {
        String result = markdownHandler.createMdFile(testFile.toString());
        assertTrue(result.contains("空白 MD 文件已创建"));
        assertTrue(Files.exists(testFile));
    }

    @Test
    void testSaveAndReadMd() {
        markdownHandler.saveMd(testFile.toString(), "# Hello Markdown");
        String result = markdownHandler.readMd(testFile.toString());
        assertTrue(result.contains("# Hello Markdown"));
    }

    @Test
    void testAppendMd() {
        markdownHandler.saveMd(testFile.toString(), "# Title");
        markdownHandler.appendMd(testFile.toString(), "## Subtitle");
        String result = markdownHandler.readMd(testFile.toString());
        assertTrue(result.contains("# Title"));
        assertTrue(result.contains("## Subtitle"));
    }

    @Test
    void testInsertMd() {
        markdownHandler.saveMd(testFile.toString(), "Line1\nLine3");
        markdownHandler.insertMd(testFile.toString(), 2, "Line2");
        String result = markdownHandler.readMd(testFile.toString());
        assertTrue(result.contains("Line2"));
    }

    @Test
    void testReplaceMdContent() {
        markdownHandler.saveMd(testFile.toString(), "Hello World");
        markdownHandler.replaceMdContent(testFile.toString(), "World", "Markdown");
        String result = markdownHandler.readMd(testFile.toString());
        assertTrue(result.contains("Markdown"));
    }

    @Test
    void testSaveMdAs() {
        markdownHandler.saveMd(testFile.toString(), "# Original");
        Path target = tempDir.resolve("copy.md");
        String result = markdownHandler.saveMdAs(testFile.toString(), target.toString());
        assertTrue(result.contains("已另存为"));
        assertTrue(Files.exists(target));
    }

    @Test
    void testMdGenerateHeading() {
        String result = markdownHandler.mdGenerateHeading(1, "Title");
        assertTrue(result.contains("# Title"));
    }

    @Test
    void testMdGenerateList() {
        String result = markdownHandler.mdGenerateList("Item1\nItem2\nItem3", false);
        assertTrue(result.contains("- Item1"));
        assertTrue(result.contains("- Item2"));
    }

    @Test
    void testMdGenerateCodeBlock() {
        String result = markdownHandler.mdGenerateCodeBlock("java", "System.out.println();");
        assertTrue(result.contains("```java"));
        assertTrue(result.contains("```"));
    }

    @Test
    void testMdGenerateTable() {
        String result = markdownHandler.mdGenerateTable("Name,Age", "John,25\nJane,30");
        assertTrue(result.contains("Name"));
        assertTrue(result.contains("John"));
    }

    @Test
    void testMdGenerateBlockquote() {
        String result = markdownHandler.mdGenerateBlockquote("Quote text");
        assertTrue(result.contains("> Quote text"));
    }
}