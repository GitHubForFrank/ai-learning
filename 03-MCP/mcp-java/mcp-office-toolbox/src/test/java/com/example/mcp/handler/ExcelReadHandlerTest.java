package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.mcp.handler.excel.ExcelReadHandler;
import java.io.FileOutputStream;
import java.nio.file.Path;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ExcelReadHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class ExcelReadHandlerTest {

    @TempDir
    Path tempDir;

    @Autowired
    private ExcelReadHandler excelReadHandler;

    private Path testExcelFile;
    private String sheetName;

    @BeforeEach
    void setUp() throws Exception {
        testExcelFile = tempDir.resolve("test.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            sheetName = sheet.getSheetName();

            // 标题行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0)
                     .setCellValue("姓名");
            headerRow.createCell(1)
                     .setCellValue("年龄");
            headerRow.createCell(2)
                     .setCellValue("城市");

            // 数据行
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

            Row row3 = sheet.createRow(3);
            row3.createCell(0)
                .setCellValue("王五");
            row3.createCell(1)
                .setCellValue(28);
            row3.createCell(2)
                .setCellValue("广州");

            try (FileOutputStream fos = new FileOutputStream(testExcelFile.toFile())) {
                workbook.write(fos);
            }
        }
    }

    /**
     * 创建测试用的 Excel 文件。
     *
     * @param filename  文件名
     * @param sheetName 工作表名称
     * @param data      二维数据数组，第一行为标题，后续行为数据
     * @return 文件路径
     */
    private Path createExcelFile(String filename, String sheetName, String[][] data) throws Exception {
        Path filePath = tempDir.resolve(filename);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            for (int i = 0; i < data.length; i++) {
                Row row = sheet.createRow(i);
                for (int j = 0; j < data[i].length; j++) {
                    row.createCell(j)
                       .setCellValue(data[i][j]);
                }
            }
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                workbook.write(fos);
            }
        }
        return filePath;
    }

    // ==================== excelDescribeSheets 测试 ====================

    @Test
    void testDescribeSheetsValidFile() {
        String result = excelReadHandler.excelDescribeSheets(testExcelFile.toString());
        JSONArray sheets = JSON.parseArray(result);
        assertFalse(sheets.isEmpty());
        JSONObject sheetInfo = sheets.getJSONObject(0);
        assertEquals(sheetName, sheetInfo.getString("name"));
        assertEquals(0, sheetInfo.getIntValue("index"));
        assertEquals(4, sheetInfo.getIntValue("rowCount"));
        assertEquals(3, sheetInfo.getIntValue("columnCount"));
    }

    @Test
    void testDescribeSheetsNonExistentFile() {
        String result = excelReadHandler.excelDescribeSheets(tempDir.resolve("nonexistent.xlsx")
                                                                    .toString());
        assertTrue(result.startsWith("错误:"));
        assertTrue(result.contains("文件不存在"));
    }

    // ==================== excelReadSheet 测试 ====================

    @Test
    void testReadSheetValidFile() {
        String result = excelReadHandler.excelReadSheet(testExcelFile.toString(), sheetName, null, null, null, null, null);
        JSONObject json = JSON.parseObject(result);
        assertEquals(sheetName, json.getString("sheetName"));
        assertNotNull(json.getJSONArray("cells"));
        assertEquals(4, json.getIntValue("totalRows"));
        assertFalse(json.getBooleanValue("hasMore"));
    }

    @Test
    void testReadSheetNonExistentSheet() {
        String result = excelReadHandler.excelReadSheet(testExcelFile.toString(), "NonExistentSheet", null, null, null, null, null);
        assertTrue(result.startsWith("错误:"));
        assertTrue(result.contains("工作表不存在"));
    }

    @Test
    void testReadSheetWithPagination() {
        String result = excelReadHandler.excelReadSheet(testExcelFile.toString(), sheetName, null, null, null, 0, 2);
        JSONObject json = JSON.parseObject(result);
        JSONArray cells = json.getJSONArray("cells");
        assertEquals(2, cells.size());
        assertEquals(0, json.getIntValue("offset"));
        assertEquals(2, json.getIntValue("limit"));
        assertTrue(json.getBooleanValue("hasMore"));
    }

    @Test
    void testReadSheetWithRange() {
        String result = excelReadHandler.excelReadSheet(testExcelFile.toString(), sheetName, "A1:B2", null, null, null, null);
        JSONObject json = JSON.parseObject(result);
        JSONArray cells = json.getJSONArray("cells");
        assertEquals(2, cells.size());
        JSONArray firstRow = cells.getJSONArray(0);
        assertEquals(2, firstRow.size());
    }

    @Test
    void testReadSheetWithShowFormula() {
        String result = excelReadHandler.excelReadSheet(testExcelFile.toString(), sheetName, null, true, null, null, null);
        JSONObject json = JSON.parseObject(result);
        assertNotNull(json.getJSONArray("cells"));
    }

    @Test
    void testReadSheetWithShowStyle() {
        String result = excelReadHandler.excelReadSheet(testExcelFile.toString(), sheetName, null, null, true, null, null);
        JSONObject json = JSON.parseObject(result);
        JSONArray cells = json.getJSONArray("cells");
        JSONArray firstRow = cells.getJSONArray(0);
        JSONObject firstCell = firstRow.getJSONObject(0);
        assertTrue(firstCell.containsKey("value"));
        assertTrue(firstCell.containsKey("style"));
    }

    // ==================== excelScreenCapture 测试 ====================

    @Test
    void testScreenCaptureValidFile() {
        String result = excelReadHandler.excelScreenCapture(testExcelFile.toString(), sheetName, null);
        JSONObject json = JSON.parseObject(result);
        assertEquals("image/png", json.getString("mimeType"));
        assertEquals(sheetName, json.getString("sheetName"));
        String base64Data = json.getString("data");
        assertNotNull(base64Data);
        assertFalse(base64Data.isEmpty());
    }

    @Test
    void testScreenCaptureWithRange() {
        String result = excelReadHandler.excelScreenCapture(testExcelFile.toString(), sheetName, "A1:B2");
        JSONObject json = JSON.parseObject(result);
        assertEquals("image/png", json.getString("mimeType"));
        assertEquals("A1:B2", json.getString("range"));
        assertNotNull(json.getString("data"));
    }

    // ==================== excelAnalyze 测试 ====================

    @Test
    void testAnalyzeValidRange() {
        String result = excelReadHandler.excelAnalyze(testExcelFile.toString(), sheetName, "A1:C4");
        JSONObject json = JSON.parseObject(result);
        assertEquals(sheetName, json.getString("sheetName"));
        assertEquals("A1:C4", json.getString("range"));
        assertEquals(3, json.getIntValue("totalDataRows"));
        assertEquals(3, json.getIntValue("totalColumns"));

        JSONArray columns = json.getJSONArray("columns");
        assertEquals(3, columns.size());

        // 姓名列应为文本类型
        JSONObject nameCol = columns.getJSONObject(0);
        assertEquals("姓名", nameCol.getString("column"));
        assertEquals("text", nameCol.getString("type"));

        // 年龄列应为数值类型，自动计算统计信息
        JSONObject ageCol = columns.getJSONObject(1);
        assertEquals("年龄", ageCol.getString("column"));
        assertEquals("numeric", ageCol.getString("type"));
        assertEquals(3, ageCol.getIntValue("count"));
        assertEquals(83.0, ageCol.getDoubleValue("sum"), 0.01);
        assertEquals(25.0, ageCol.getDoubleValue("min"), 0.01);
        assertEquals(30.0, ageCol.getDoubleValue("max"), 0.01);

        // 城市列应为文本类型
        JSONObject cityCol = columns.getJSONObject(2);
        assertEquals("城市", cityCol.getString("column"));
        assertEquals("text", cityCol.getString("type"));
    }

    // ==================== excelCompare 测试 ====================

    @Test
    void testCompareIdenticalSheets() throws Exception {
        Path file1 = createExcelFile("compare1.xlsx", "Sheet1",
                                     new String[][]{{"姓名", "年龄", "城市"}, {"张三", "25", "北京"}, {"李四", "30", "上海"}});
        Path file2 = createExcelFile("compare2.xlsx", "Sheet1",
                                     new String[][]{{"姓名", "年龄", "城市"}, {"张三", "25", "北京"}, {"李四", "30", "上海"}});

        String result = excelReadHandler.excelCompare(file1.toString(), "Sheet1", file2.toString(), "Sheet1", 0);
        JSONObject json = JSON.parseObject(result);
        assertEquals(0, json.getIntValue("addedCount"));
        assertEquals(0, json.getIntValue("removedCount"));
        assertEquals(0, json.getIntValue("modifiedCount"));
        assertEquals(2, json.getIntValue("totalRows1"));
        assertEquals(2, json.getIntValue("totalRows2"));
    }

    @Test
    void testCompareDifferentSheets() throws Exception {
        Path file1 = createExcelFile("diff1.xlsx", "Sheet1",
                                     new String[][]{{"姓名", "年龄", "城市"}, {"张三", "25", "北京"}, {"李四", "30", "上海"}});
        Path file2 = createExcelFile("diff2.xlsx", "Sheet1",
                                     new String[][]{{"姓名", "年龄", "城市"}, {"张三", "25", "北京"}, {"王五", "28", "广州"}});

        String result = excelReadHandler.excelCompare(file1.toString(), "Sheet1", file2.toString(), "Sheet1", 0);
        JSONObject json = JSON.parseObject(result);
        assertEquals(1, json.getIntValue("addedCount"));
        assertEquals(1, json.getIntValue("removedCount"));
        assertEquals(0, json.getIntValue("modifiedCount"));

        JSONArray added = json.getJSONArray("added");
        assertEquals("王五", added.getJSONObject(0)
                                  .getString("key"));

        JSONArray removed = json.getJSONArray("removed");
        assertEquals("李四", removed.getJSONObject(0)
                                    .getString("key"));
    }

    // ==================== excelPreview 测试 ====================

    @Test
    void testPreviewValidFile() {
        String result = excelReadHandler.excelPreview(testExcelFile.toString(), sheetName, 10);
        assertTrue(result.contains("|"));
        assertTrue(result.contains("姓名"));
        assertTrue(result.contains("年龄"));
        assertTrue(result.contains("城市"));
        assertTrue(result.contains("张三"));
        assertTrue(result.contains("李四"));
        assertTrue(result.contains("王五"));
        assertTrue(result.contains("---"));
    }

    @Test
    void testPreviewWithMaxRows() {
        String result = excelReadHandler.excelPreview(testExcelFile.toString(), sheetName, 2);
        assertTrue(result.contains("张三"));
        assertFalse(result.contains("李四"));
    }

    @Test
    void testPreviewNonExistentSheet() {
        String result = excelReadHandler.excelPreview(testExcelFile.toString(), "NonExistentSheet", null);
        assertTrue(result.startsWith("错误:"));
        assertTrue(result.contains("工作表不存在"));
    }
}