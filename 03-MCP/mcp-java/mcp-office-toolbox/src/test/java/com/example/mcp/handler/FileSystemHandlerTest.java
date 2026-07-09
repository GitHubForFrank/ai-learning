package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * FileSystemHandler 单元测试
 *
 * @author FrankKang
 * @since 2026-07-09
 */
@SpringBootTest
class FileSystemHandlerTest {

    /**
     * 在项目目录下创建临时测试目录，确保在 FileSystemHandler 的允许范围内
     */
    private static Path tempDir;
    @Autowired
    private FileSystemHandler fileSystemHandler;
    private Path testFile;

    @BeforeAll
    static void setUpClass() throws Exception {
        tempDir = Files.createTempDirectory(Path.of(System.getProperty("user.dir"), "target"), "junit-fs-");
    }

    @AfterAll
    static void tearDownClass() throws Exception {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var walk = Files.walk(tempDir)) {
                walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
            }
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        testFile = tempDir.resolve("test.txt");
    }

    @Test
    void testWriteAndReadFile() {
        fileSystemHandler.writeFile(testFile.toString(), "Hello World");
        String result = fileSystemHandler.readTextFile(testFile.toString(), null, null);
        assertTrue(result.contains("Hello World"));
    }

    @Test
    void testAppendToFile() {
        fileSystemHandler.writeFile(testFile.toString(), "Line1");
        fileSystemHandler.appendToFile(testFile.toString(), "\nLine2");
        String result = fileSystemHandler.readTextFile(testFile.toString(), null, null);
        assertTrue(result.contains("Line1"));
        assertTrue(result.contains("Line2"));
    }

    @Test
    void testCopyFile() {
        fileSystemHandler.writeFile(testFile.toString(), "copy test");
        Path dest = tempDir.resolve("copy.txt");
        String result = fileSystemHandler.copyFile(testFile.toString(), dest.toString());
        assertTrue(result.contains("复制成功"));
        assertTrue(Files.exists(dest));
    }

    @Test
    void testDeleteFile() {
        fileSystemHandler.writeFile(testFile.toString(), "to delete");
        String result = fileSystemHandler.deleteFile(testFile.toString());
        assertTrue(result.contains("删除成功"));
        assertFalse(Files.exists(testFile));
    }

    @Test
    void testClearFile() {
        fileSystemHandler.writeFile(testFile.toString(), "content");
        fileSystemHandler.clearFile(testFile.toString());
        String result = fileSystemHandler.readTextFile(testFile.toString(), null, null);
        assertEquals("", result.trim());
    }

    @Test
    void testSearchInFile() {
        fileSystemHandler.writeFile(testFile.toString(), "Hello World\nGoodbye World");
        String result = fileSystemHandler.searchInFile(testFile.toString(), "World", null);
        assertTrue(result.contains("World"));
    }

    @Test
    void testReplaceInFile() {
        fileSystemHandler.writeFile(testFile.toString(), "Hello World");
        fileSystemHandler.replaceInFile(testFile.toString(), "World", "Java");
        String result = fileSystemHandler.readTextFile(testFile.toString(), null, null);
        assertTrue(result.contains("Java"));
        assertFalse(result.contains("World"));
    }

    @Test
    void testCreateDirectory() {
        Path dir = tempDir.resolve("newdir");
        String result = fileSystemHandler.createDirectory(dir.toString());
        assertTrue(result.contains("目录已创建"));
        assertTrue(Files.isDirectory(dir));
    }

    @Test
    void testListDirectory() {
        fileSystemHandler.writeFile(testFile.toString(), "test");
        String result = fileSystemHandler.listDirectory(tempDir.toString());
        assertTrue(result.contains("test.txt"));
    }

    @Test
    void testGetFileInfo() {
        fileSystemHandler.writeFile(testFile.toString(), "info test");
        String result = fileSystemHandler.getFileInfo(testFile.toString());
        assertTrue(result.contains("test.txt"));
    }

    @Test
    void testListAllowedDirectories() {
        String result = fileSystemHandler.listAllowedDirectories();
        assertNotNull(result);
    }
}