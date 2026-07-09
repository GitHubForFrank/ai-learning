package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ZipHandler 单元测试
 *
 * @author FrankKang
 * @since 2026-07-09
 */
@SpringBootTest
class ZipHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private ZipHandler zipHandler;
    private Path sourceDir;
    private Path testFile1;
    private Path testFile2;
    private Path zipFile;

    @BeforeEach
    void setUp() throws Exception {
        sourceDir = tempDir.resolve("source");
        Files.createDirectories(sourceDir);
        testFile1 = sourceDir.resolve("file1.txt");
        testFile2 = sourceDir.resolve("file2.txt");
        Files.writeString(testFile1, "Content of file 1");
        Files.writeString(testFile2, "Content of file 2");
        zipFile = tempDir.resolve("test.zip");
    }

    @Test
    void testZipCompress() {
        String result = zipHandler.zipCompress(testFile1.toString() + "," + testFile2.toString(), zipFile.toString());
        assertTrue(result.contains("压缩成功"));
        assertTrue(Files.exists(zipFile));
    }

    @Test
    void testZipCompressDirectory() {
        String result = zipHandler.zipCompressDirectory(sourceDir.toString(), zipFile.toString());
        assertTrue(result.contains("目录压缩成功"));
        assertTrue(Files.exists(zipFile));
    }

    @Test
    void testZipList() {
        zipHandler.zipCompress(testFile1.toString() + "," + testFile2.toString(), zipFile.toString());
        String result = zipHandler.zipList(zipFile.toString());
        assertTrue(result.contains("file1.txt"));
        assertTrue(result.contains("file2.txt"));
    }

    @Test
    void testZipDecompress() {
        zipHandler.zipCompress(testFile1.toString() + "," + testFile2.toString(), zipFile.toString());
        Path extractDir = tempDir.resolve("extracted");
        String result = zipHandler.zipDecompress(zipFile.toString(), extractDir.toString());
        assertTrue(result.contains("解压成功"));
        assertTrue(Files.exists(extractDir.resolve("file1.txt")));
        assertTrue(Files.exists(extractDir.resolve("file2.txt")));
    }
}