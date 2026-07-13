package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.document.DocumentDiffHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * DocumentDiffHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class DocumentDiffHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private DocumentDiffHandler documentDiffHandler;

    @Test
    void testDiffText() throws Exception {
        Path f1 = tempDir.resolve("a.txt");
        Path f2 = tempDir.resolve("b.txt");
        Files.writeString(f1, "line1\nline2\nline3");
        Files.writeString(f2, "line1\nline2_modified\nline3\nline4");

        String result = documentDiffHandler.diffText(f1.toString(), f2.toString());
        assertNotNull(result);
        assertTrue(result.contains("file1"));
    }

    @Test
    void testDiffDirectories() throws Exception {
        Path d1 = tempDir.resolve("dir1");
        Path d2 = tempDir.resolve("dir2");
        Files.createDirectories(d1);
        Files.createDirectories(d2);
        Files.writeString(d1.resolve("a.txt"), "content");

        String result = documentDiffHandler.diffDirectories(d1.toString(), d2.toString());
        assertNotNull(result);
    }
}
