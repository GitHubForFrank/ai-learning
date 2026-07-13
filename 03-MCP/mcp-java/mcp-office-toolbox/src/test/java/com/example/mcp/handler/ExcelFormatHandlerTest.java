package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.excel.ExcelFormatHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ExcelFormatHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class ExcelFormatHandlerTest {

    private static final String SHEET_NAME = "Sheet1";
    @TempDir
    Path tempDir;
    @Autowired
    private ExcelFormatHandler excelFormatHandler;
    private Path testXlsx;
    private Path testXls;

    @BeforeEach
    void setUp() throws Exception {
        testXlsx = tempDir.resolve("test.xlsx");
        testXls = tempDir.resolve("test.xls");

        // 创建 .xlsx 测试文件
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            Row header = sheet.createRow(0);
            header.createCell(0)
                  .setCellValue("姓名");
            header.createCell(1)
                  .setCellValue("年龄");
            header.createCell(2)
                  .setCellValue("城市");

            Row row1 = sheet.createRow(1);
            row1.createCell(0)
                .setCellValue("张三");
            row1.createCell(1)
                .setCellValue(25);
            row1.createCell(2)
                .setCellValue("北京");

            Row row2 = sheet.createRow(2);
            row2.createCell(0)
                .setCellValue("李四");
            row2.createCell(1)
                .setCellValue(30);
            row2.createCell(2)
                .setCellValue("上海");

            try (java.io.OutputStream os = Files.newOutputStream(testXlsx)) {
                workbook.write(os);
            }
        }

        // 创建 .xls 测试文件
        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            Row header = sheet.createRow(0);
            header.createCell(0)
                  .setCellValue("姓名");
            header.createCell(1)
                  .setCellValue("年龄");
            header.createCell(2)
                  .setCellValue("城市");

            try (java.io.OutputStream os = Files.newOutputStream(testXls)) {
                workbook.write(os);
            }
        }
    }

    // ==================== excelAutoFitColumns 测试 ====================

    @Test
    void testAutoFitAllColumns() {
        String result = excelFormatHandler.excelAutoFitColumns(testXlsx.toString(), SHEET_NAME, null);
        assertTrue(result.contains("已自动调整所有列宽"));
        assertTrue(result.contains(SHEET_NAME));
    }

    @Test
    void testAutoFitSpecificColumns() {
        String result = excelFormatHandler.excelAutoFitColumns(testXlsx.toString(), SHEET_NAME, "0,1");
        assertTrue(result.contains("已自动调整列宽"));
        assertTrue(result.contains("0,1"));
    }

    // ==================== excelSetColumnWidth 测试 ====================

    @Test
    void testSetColumnWidthValid() {
        String result = excelFormatHandler.excelSetColumnWidth(testXlsx.toString(), SHEET_NAME, 0, 20);
        assertTrue(result.contains("已设置第 1 列宽度为 20"));
        assertTrue(result.contains(SHEET_NAME));
    }

    @Test
    void testSetColumnWidthNegativeIndex() {
        String result = excelFormatHandler.excelSetColumnWidth(testXlsx.toString(), SHEET_NAME, -1, 20);
        assertTrue(result.contains("列索引不能为负数"));
    }

    // ==================== excelSetRowHeight 测试 ====================

    @Test
    void testSetRowHeightValid() {
        String result = excelFormatHandler.excelSetRowHeight(testXlsx.toString(), SHEET_NAME, 0, 30.5f);
        assertTrue(result.contains("已设置第 1 行高度为 30.5pt"));
        assertTrue(result.contains(SHEET_NAME));
    }

    // ==================== excelFreezePanes 测试 ====================

    @Test
    void testFreezePanes() {
        String result = excelFormatHandler.excelFreezePanes(testXlsx.toString(), SHEET_NAME, 1, 1);
        assertTrue(result.contains("已冻结窗格"));
        assertTrue(result.contains(SHEET_NAME));
    }

    // ==================== excelMergeCells 测试 ====================

    @Test
    void testMergeCells() {
        String result = excelFormatHandler.excelMergeCells(testXlsx.toString(), SHEET_NAME, "A1:C1", true);
        assertTrue(result.contains("已合并单元格"));
        assertTrue(result.contains("A1:C1"));
    }

    @Test
    void testUnmergeCells() {
        excelFormatHandler.excelMergeCells(testXlsx.toString(), SHEET_NAME, "A1:C1", true);
        String result = excelFormatHandler.excelMergeCells(testXlsx.toString(), SHEET_NAME, "A1:C1", false);
        assertTrue(result.contains("已取消合并单元格") || result.contains("未找到合并区域"));
        assertTrue(result.contains("A1:C1"));
    }

    // ==================== excelCreateTable 测试 ====================

    @Test
    void testCreateTable() {
        String result = excelFormatHandler.excelCreateTable(testXlsx.toString(), SHEET_NAME, "TestTable", "A1:C3");
        assertTrue(result.contains("成功创建表格"));
        assertTrue(result.contains("TestTable"));
    }

    @Test
    void testCreateTableXlsError() {
        String result = excelFormatHandler.excelCreateTable(testXls.toString(), SHEET_NAME, "TestTable", "A1:C3");
        assertTrue(result.contains("仅支持 .xlsx 格式文件"));
    }

    // ==================== excelConditionalFormat 测试 ====================

    @Test
    void testConditionalFormat() {
        String result = excelFormatHandler.excelConditionalFormat(testXlsx.toString(), SHEET_NAME, "B2:B3", "cellValue", ">", "20", "GREEN");
        assertTrue(result.contains("已添加条件格式"));
        assertTrue(result.contains("B2:B3"));
    }

    // ==================== excelNamedRange 测试 ====================

    @Test
    void testNamedRangeCreate() {
        String result = excelFormatHandler.excelNamedRange(testXlsx.toString(), SHEET_NAME, "MyRange", "A1:C3", "create");
        assertTrue(result.contains("已创建命名区域"));
        assertTrue(result.contains("MyRange"));
    }

    @Test
    void testNamedRangeList() {
        excelFormatHandler.excelNamedRange(testXlsx.toString(), SHEET_NAME, "MyRange", "A1:C3", "create");
        String result = excelFormatHandler.excelNamedRange(testXlsx.toString(), null, null, null, "list");
        assertNotNull(result);
        assertTrue(result.contains("MyRange"));
    }

    @Test
    void testNamedRangeDelete() {
        excelFormatHandler.excelNamedRange(testXlsx.toString(), SHEET_NAME, "MyRange", "A1:C3", "create");
        String result = excelFormatHandler.excelNamedRange(testXlsx.toString(), null, "MyRange", null, "delete");
        assertTrue(result.contains("已删除命名区域"));
        assertTrue(result.contains("MyRange"));
    }

    // ==================== excelPrintSetup 测试 ====================

    @Test
    void testPrintSetup() {
        String result = excelFormatHandler.excelPrintSetup(testXlsx.toString(), SHEET_NAME, "landscape", "A4", 0.5, 0.5, 0.7, 0.7, 1, 0, "A1:C3");
        assertTrue(result.contains("已设置打印参数"));
        assertTrue(result.contains(SHEET_NAME));
    }

    // ==================== 错误处理测试 ====================

    @Test
    void testNonExistentFile() {
        String nonExistent = tempDir.resolve("nonexistent.xlsx")
                                    .toString();
        String result = excelFormatHandler.excelAutoFitColumns(nonExistent, SHEET_NAME, null);
        assertTrue(result.startsWith("错误: 文件不存在"));
    }

    @Test
    void testInvalidSheet() {
        String result = excelFormatHandler.excelAutoFitColumns(testXlsx.toString(), "NonExistentSheet", null);
        assertTrue(result.contains("工作表不存在"));
    }
}