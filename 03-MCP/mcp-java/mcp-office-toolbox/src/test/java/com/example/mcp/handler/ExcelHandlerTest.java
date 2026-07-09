package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ExcelHandler 单元测试
 *
 * @author FrankKang
 * @since 2026-07-09
 */
@SpringBootTest
class ExcelHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private ExcelHandler excelHandler;
    private Path testExcel;

    @BeforeEach
    void setUp() {
        testExcel = tempDir.resolve("test.xlsx");
    }

    @Test
    void testCreateWorkbook() {
        String result = excelHandler.excelCreateWorkbook(testExcel.toString());
        assertTrue(result.contains("空白工作簿已创建"));
        assertTrue(Files.exists(testExcel));
    }

    @Test
    void testDescribeSheets() {
        excelHandler.excelCreateWorkbook(testExcel.toString());
        String result = excelHandler.excelDescribeSheets(testExcel.toString());
        assertTrue(result.contains("Sheet1"));
    }

    @Test
    void testWriteAndReadSheet() {
        excelHandler.excelCreateWorkbook(testExcel.toString());
        List<List<Object>> values = List.of(List.of("姓名", "年龄", "城市"), List.of("张三", "25", "北京"));
        excelHandler.excelWriteToSheet(testExcel.toString(), "Sheet1", false, "A1:C2", values);
        String result = excelHandler.excelReadSheet(testExcel.toString(), "Sheet1", null, null, null);
        assertTrue(result.contains("Sheet1"));
    }

    @Test
    void testDeleteRow() {
        excelHandler.excelCreateWorkbook(testExcel.toString());
        List<List<Object>> values = List.of(List.of("Row1", "Data1"), List.of("Row2", "Data2"), List.of("Row3", "Data3"));
        excelHandler.excelWriteToSheet(testExcel.toString(), "Sheet1", false, "A1:B3", values);
        String result = excelHandler.excelDeleteRow(testExcel.toString(), "Sheet1", 1);
        assertTrue(result.contains("已删除"));
    }

    @Test
    void testClearSheet() {
        excelHandler.excelCreateWorkbook(testExcel.toString());
        List<List<Object>> values = List.of(List.of("Data1", "Data2"));
        excelHandler.excelWriteToSheet(testExcel.toString(), "Sheet1", false, "A1:B1", values);
        String result = excelHandler.excelClearSheet(testExcel.toString(), "Sheet1");
        assertTrue(result.contains("数据已清空"));
    }
}