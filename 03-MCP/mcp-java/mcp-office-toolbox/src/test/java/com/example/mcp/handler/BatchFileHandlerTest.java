package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.mcp.handler.batch.BatchFileHandler;
import com.example.mcp.pojo.batch.BatchRenameRequest;
import com.example.mcp.pojo.batch.BatchReplaceTextRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * BatchFileHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class BatchFileHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private BatchFileHandler batchFileHandler;

    @Test
    void testBatchRename() throws Exception {
        Files.writeString(tempDir.resolve("old_a.txt"), "content");
        Files.writeString(tempDir.resolve("old_b.txt"), "content");
        BatchRenameRequest req = new BatchRenameRequest(tempDir.toString(), "replace", "old_|new_", "*.txt", false);
        String result = batchFileHandler.batchRename(req);
        assertNotNull(result);
    }

    @Test
    void testBatchReplaceText() throws Exception {
        Files.writeString(tempDir.resolve("test.txt"), "Hello World");
        BatchReplaceTextRequest req = new BatchReplaceTextRequest(tempDir.toString(), "World", "Java", "*.txt", false, false);
        String result = batchFileHandler.batchReplaceText(req);
        assertNotNull(result);
    }

    @Test
    void testFindDuplicateFiles() throws Exception {
        Files.writeString(tempDir.resolve("a.txt"), "same content");
        Files.writeString(tempDir.resolve("b.txt"), "same content");
        Files.writeString(tempDir.resolve("c.txt"), "different");
        String result = batchFileHandler.findDuplicateFiles(tempDir.toString(), null, false);
        assertNotNull(result);
    }
}
