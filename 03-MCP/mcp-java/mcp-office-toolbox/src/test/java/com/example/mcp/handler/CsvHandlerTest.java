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
 * CsvHandler 单元测试
 *
 * @author FrankKang
 * @since 2026-07-09
 */
@SpringBootTest
class CsvHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private CsvHandler csvHandler;
    private Path testCsv;

    @BeforeEach
    void setUp() {
        testCsv = tempDir.resolve("test.csv");
    }

    @Test
    void testCreateCsvWithHeaders() {
        String result = csvHandler.csvCreate(testCsv.toString(), "姓名,年龄,城市");
        assertTrue(result.contains("CSV 文件已创建"));
        assertTrue(Files.exists(testCsv));
    }

    @Test
    void testCreateCsvWithoutHeaders() {
        String result = csvHandler.csvCreate(testCsv.toString(), null);
        assertTrue(result.contains("空白 CSV 文件已创建"));
        assertTrue(Files.exists(testCsv));
    }

    @Test
    void testWriteAndReadCsv() {
        csvHandler.csvWrite(testCsv.toString(), "张三,25,北京\n李四,30,上海", "姓名,年龄,城市", null);
        String result = csvHandler.csvRead(testCsv.toString(), null, null, true, null);
        assertTrue(result.contains("张三"));
        assertTrue(result.contains("李四"));
    }

    @Test
    void testReadCsvHeaders() {
        csvHandler.csvWrite(testCsv.toString(), "张三,25,北京", "姓名,年龄,城市", null);
        String result = csvHandler.csvReadHeaders(testCsv.toString(), null);
        assertTrue(result.contains("姓名"));
        assertTrue(result.contains("年龄"));
    }

    @Test
    void testAppendCsv() {
        csvHandler.csvWrite(testCsv.toString(), "张三,25,北京", "姓名,年龄,城市", null);
        csvHandler.csvAppend(testCsv.toString(), "李四,30,上海");
        String result = csvHandler.csvRead(testCsv.toString(), null, null, false, null);
        assertTrue(result.contains("李四"));
    }

    @Test
    void testCsvInfo() {
        csvHandler.csvWrite(testCsv.toString(), "张三,25,北京\n李四,30,上海", "姓名,年龄,城市", null);
        String result = csvHandler.csvInfo(testCsv.toString(), null);
        assertTrue(result.contains("test.csv"));
        assertTrue(result.contains("totalRows"));
    }
}