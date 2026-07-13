package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.excel.ExcelAnalyzeHandler;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ExcelAnalyzeHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class ExcelAnalyzeHandlerTest {

    @TempDir
    Path tempDir;

    @Autowired
    private ExcelAnalyzeHandler excelAnalyzeHandler;

    private Path testFile;

    /**
     * 创建示例 Excel 文件，包含表头"姓名,年龄,城市"和4行数据。
     */
    @BeforeEach
    void setUp() throws Exception {
        testFile = tempDir.resolve("test.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            // 表头
            Row header = sheet.createRow(0);
            header.createCell(0)
                  .setCellValue("姓名");
            header.createCell(1)
                  .setCellValue("年龄");
            header.createCell(2)
                  .setCellValue("城市");

            // 数据行
            String[][] data = {{"张三", "25", "北京"}, {"李四", "30", "上海"}, {"王五", "25", "广州"}, {"赵六", "30", "北京"}};
            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int j = 0; j < data[i].length; j++) {
                    Cell cell = row.createCell(j);
                    // 年龄列存为数值
                    if (j == 1) {
                        cell.setCellValue(Integer.parseInt(data[i][j]));
                    } else {
                        cell.setCellValue(data[i][j]);
                    }
                }
            }
            try (FileOutputStream fos = new FileOutputStream(testFile.toFile())) {
                workbook.write(fos);
            }
        }
    }

    // ==================== 1. excelSortRange 测试 ====================

    @Test
    void testExcelSortRangeAscending() {
        String result = excelAnalyzeHandler.excelSortRange(testFile.toString(), "Sheet1", "A1:C5", 0, true);
        assertNotNull(result);
        assertTrue(result.contains("升序"));
        assertTrue(result.contains("已按第"));
    }

    @Test
    void testExcelSortRangeDescending() {
        String result = excelAnalyzeHandler.excelSortRange(testFile.toString(), "Sheet1", "A1:C5", 0, false);
        assertNotNull(result);
        assertTrue(result.contains("降序"));
        assertTrue(result.contains("已按第"));
    }

    @Test
    void testExcelSortRangeByNumericColumn() {
        String result = excelAnalyzeHandler.excelSortRange(testFile.toString(), "Sheet1", "A1:C5", 1, true);
        assertNotNull(result);
        assertTrue(result.contains("升序"));
        assertTrue(result.contains("已按第 2 列"));
    }

    // ==================== 2. excelFilterRange 测试 ====================

    @Test
    void testExcelFilterRangeWithRange() {
        String result = excelAnalyzeHandler.excelFilterRange(testFile.toString(), "Sheet1", "A1:C5");
        assertNotNull(result);
        assertTrue(result.contains("已添加自动筛选"));
    }

    @Test
    void testExcelFilterRangeWithoutRange() {
        String result = excelAnalyzeHandler.excelFilterRange(testFile.toString(), "Sheet1", null);
        assertNotNull(result);
        assertTrue(result.contains("已添加自动筛选"));
    }

    // ==================== 3. excelFindReplace 测试 ====================

    @Test
    void testExcelFindReplaceWithMatches() {
        String result = excelAnalyzeHandler.excelFindReplace(testFile.toString(), "Sheet1", "北京", "南京", "A1:C5");
        assertNotNull(result);
        assertTrue(result.contains("已替换"));
        assertTrue(result.contains("北京"));
        assertTrue(result.contains("南京"));
    }

    @Test
    void testExcelFindReplaceNoMatches() {
        String result = excelAnalyzeHandler.excelFindReplace(testFile.toString(), "Sheet1", "东京", "纽约", "A1:C5");
        assertNotNull(result);
        assertTrue(result.contains("已替换 0 个单元格"));
    }

    // ==================== 4. excelRemoveDuplicates 测试 ====================

    @Test
    void testExcelRemoveDuplicatesSingleColumn() {
        String result = excelAnalyzeHandler.excelRemoveDuplicates(testFile.toString(), "Sheet1", "1", 1);
        assertNotNull(result);
        assertTrue(result.contains("已移除"));
        assertTrue(result.contains("重复行"));
    }

    @Test
    void testExcelRemoveDuplicatesMultipleColumns() {
        String result = excelAnalyzeHandler.excelRemoveDuplicates(testFile.toString(), "Sheet1", "1,2", 1);
        assertNotNull(result);
        assertTrue(result.contains("已移除"));
        assertTrue(result.contains("重复行"));
    }

    // ==================== 5. excelApplyFormula 测试 ====================

    @Test
    void testExcelApplyFormulaSum() {
        String result = excelAnalyzeHandler.excelApplyFormula(testFile.toString(), "Sheet1", "D2", "=SUM(B2:B5)");
        assertNotNull(result);
        assertTrue(result.contains("已应用公式"));
        assertTrue(result.contains("SUM"));
    }

    // ==================== 6. excelDataValidation 测试 ====================

    @Test
    void testExcelDataValidationDropdown() {
        String result = excelAnalyzeHandler.excelDataValidation(testFile.toString(), "Sheet1", "A2:A5", "张三,李四,王五,赵六");
        assertNotNull(result);
        assertTrue(result.contains("已添加数据验证"));
    }

    // ==================== 7. excelConvertCsv 测试 ====================

    @Test
    void testExcelConvertCsvExcelToCsv() throws Exception {
        Path targetCsv = tempDir.resolve("output.csv");
        String result = excelAnalyzeHandler.excelConvertCsv(testFile.toString(), targetCsv.toString());
        assertNotNull(result);
        assertTrue(result.contains("已从 Excel 转换为 CSV"));
        assertTrue(Files.exists(targetCsv));
    }

    @Test
    void testExcelConvertCsvCsvToExcel() throws Exception {
        // 先创建 CSV 文件
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, "姓名,年龄,城市\n张三,25,北京\n李四,30,上海\n");

        Path targetXlsx = tempDir.resolve("output.xlsx");
        String result = excelAnalyzeHandler.excelConvertCsv(csvFile.toString(), targetXlsx.toString());
        assertNotNull(result);
        assertTrue(result.contains("已从 CSV 转换为 Excel"));
        assertTrue(Files.exists(targetXlsx));
    }

    // ==================== 8. excelMergeWorkbooks 测试 ====================

    @Test
    void testExcelMergeWorkbooks() throws Exception {
        // 创建第二个 Excel 文件
        Path file2 = tempDir.resolve("test2.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0)
                  .setCellValue("产品");
            header.createCell(1)
                  .setCellValue("价格");
            Row row = sheet.createRow(1);
            row.createCell(0)
               .setCellValue("电脑");
            row.createCell(1)
               .setCellValue(5000);
            try (FileOutputStream fos = new FileOutputStream(file2.toFile())) {
                workbook.write(fos);
            }
        }

        Path targetFile = tempDir.resolve("merged.xlsx");
        String sources = testFile.toString() + "," + file2.toString();
        String result = excelAnalyzeHandler.excelMergeWorkbooks(targetFile.toString(), sources, null);
        assertNotNull(result);
        assertTrue(result.contains("已合并"));
        assertTrue(Files.exists(targetFile));
    }

    // ==================== 9. excelPivotTable 测试 ====================

    @Test
    void testExcelPivotTable() {
        String result = excelAnalyzeHandler.excelPivotTable(testFile.toString(), "Sheet1", "A1:C5", "透视表", "A1", "城市", null, "年龄", "SUM");
        assertNotNull(result);
        assertTrue(result.contains("成功创建透视表"));
    }

    // ==================== 10. excelProtectSheet 测试 ====================

    @Test
    void testExcelProtectSheetProtectSheet() {
        String result = excelAnalyzeHandler.excelProtectSheet(testFile.toString(), "Sheet1", "123456", true);
        assertNotNull(result);
        assertTrue(result.contains("已保护工作表"));
    }

    @Test
    void testExcelProtectSheetProtectWorkbook() {
        String result = excelAnalyzeHandler.excelProtectSheet(testFile.toString(), "Sheet1", "123456", false);
        assertNotNull(result);
        assertTrue(result.contains("已保护"));
    }

    // ==================== 11. excelSubtotal 测试 ====================

    @Test
    void testExcelSubtotal() {
        String result = excelAnalyzeHandler.excelSubtotal(testFile.toString(), "Sheet1", 2, 1, "SUM");
        assertNotNull(result);
        assertTrue(result.contains("成功分类汇总"));
        assertTrue(result.contains("个分组"));
    }

    // ==================== 12. excelGroupRows 测试 ====================

    @Test
    void testExcelGroupRows() {
        String result = excelAnalyzeHandler.excelGroupRows(testFile.toString(), "Sheet1", 1, 4);
        assertNotNull(result);
        assertTrue(result.contains("成功分组折叠行"));
    }

    // ==================== 错误处理测试 ====================

    @Test
    void testErrorHandlingNonExistentFile() {
        String nonExistentFile = tempDir.resolve("nonexistent.xlsx")
                                        .toString();
        String result = excelAnalyzeHandler.excelSortRange(nonExistentFile, "Sheet1", "A1:C5", 0, true);
        assertNotNull(result);
        assertTrue(result.contains("错误"));
    }

    @Test
    void testErrorHandlingInvalidSheet() {
        String result = excelAnalyzeHandler.excelSortRange(testFile.toString(), "InvalidSheet", "A1:C5", 0, true);
        assertNotNull(result);
        assertTrue(result.contains("错误"));
    }
}