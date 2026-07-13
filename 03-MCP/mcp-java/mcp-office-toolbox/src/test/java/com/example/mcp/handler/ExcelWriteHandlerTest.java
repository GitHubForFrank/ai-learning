package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.excel.ExcelWriteHandler;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
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
 * ExcelWriteHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class ExcelWriteHandlerTest {

    @TempDir
    Path tempDir;

    @Autowired
    private ExcelWriteHandler excelWriteHandler;

    private Path testExcel;

    @BeforeEach
    void setUp() throws Exception {
        testExcel = tempDir.resolve("test.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0)
                     .setCellValue("姓名");
            headerRow.createCell(1)
                     .setCellValue("年龄");
            headerRow.createCell(2)
                     .setCellValue("城市");

            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0)
                    .setCellValue("张三");
            dataRow1.createCell(1)
                    .setCellValue(25);
            dataRow1.createCell(2)
                    .setCellValue("北京");

            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0)
                    .setCellValue("李四");
            dataRow2.createCell(1)
                    .setCellValue(30);
            dataRow2.createCell(2)
                    .setCellValue("上海");

            try (java.io.OutputStream os = Files.newOutputStream(testExcel)) {
                workbook.write(os);
            }
        }
    }

    // ==================== excelCreateWorkbook ====================

    @Test
    void testCreateWorkbookValidPath() {
        Path newFile = tempDir.resolve("new.xlsx");
        String result = excelWriteHandler.excelCreateWorkbook(newFile.toString());
        assertTrue(result.contains("空白工作簿已创建"));
        assertTrue(Files.exists(newFile));
    }

    @Test
    void testCreateWorkbookOverwrite() {
        Path newFile = tempDir.resolve("overwrite.xlsx");
        excelWriteHandler.excelCreateWorkbook(newFile.toString());
        String result = excelWriteHandler.excelCreateWorkbook(newFile.toString());
        assertTrue(result.contains("空白工作簿已创建"));
        assertTrue(Files.exists(newFile));
    }

    // ==================== excelWriteToSheet ====================

    @Test
    void testWriteToExistingSheet() {
        List<List<Object>> values = new ArrayList<>();
        values.add(Arrays.asList("王五", 28, "广州"));
        String result = excelWriteHandler.excelWriteToSheet(testExcel.toString(), "Sheet1", false, "A3:C3", values);
        assertTrue(result.contains("成功写入"));
    }

    @Test
    void testWriteToNewSheet() {
        List<List<Object>> values = new ArrayList<>();
        values.add(Arrays.asList("赵六", 35, "深圳"));
        String result = excelWriteHandler.excelWriteToSheet(testExcel.toString(), "新表", true, "A1:C1", values);
        assertTrue(result.contains("成功写入"));
    }

    @Test
    void testWriteDataToSheet() {
        List<List<Object>> values = new ArrayList<>();
        values.add(Arrays.asList("孙七", 22, "杭州"));
        values.add(Arrays.asList("周八", 40, "成都"));
        String result = excelWriteHandler.excelWriteToSheet(testExcel.toString(), "Sheet1", false, "A4:C5", values);
        assertTrue(result.contains("成功写入"));
    }

    // ==================== excelCopySheet ====================

    @Test
    void testCopySheetValid() {
        String result = excelWriteHandler.excelCopySheet(testExcel.toString(), "Sheet1", "Sheet1Copy");
        assertTrue(result.contains("成功复制"));
    }

    @Test
    void testCopySheetSourceNotFound() {
        String result = excelWriteHandler.excelCopySheet(testExcel.toString(), "NonExistent", "Copy");
        assertTrue(result.contains("错误"));
    }

    @Test
    void testCopySheetDestAlreadyExists() {
        excelWriteHandler.excelCopySheet(testExcel.toString(), "Sheet1", "Sheet1Copy");
        String result = excelWriteHandler.excelCopySheet(testExcel.toString(), "Sheet1", "Sheet1Copy");
        assertTrue(result.contains("错误"));
    }

    // ==================== excelDeleteRow ====================

    @Test
    void testDeleteRowValid() {
        String result = excelWriteHandler.excelDeleteRow(testExcel.toString(), "Sheet1", 1);
        assertTrue(result.contains("已删除"));
    }

    @Test
    void testDeleteRowOutOfRange() {
        String result = excelWriteHandler.excelDeleteRow(testExcel.toString(), "Sheet1", 100);
        assertTrue(result.contains("错误"));
    }

    // ==================== excelDeleteColumn ====================

    @Test
    void testDeleteColumnValid() {
        String result = excelWriteHandler.excelDeleteColumn(testExcel.toString(), "Sheet1", 1);
        assertTrue(result.contains("已删除"));
    }

    @Test
    void testDeleteColumnNegativeIndex() {
        String result = excelWriteHandler.excelDeleteColumn(testExcel.toString(), "Sheet1", -1);
        assertTrue(result.contains("错误"));
    }

    // ==================== excelClearSheet ====================

    @Test
    void testClearSheet() {
        String result = excelWriteHandler.excelClearSheet(testExcel.toString(), "Sheet1");
        assertTrue(result.contains("已清空"));
    }

    // ==================== excelInsertRow ====================

    @Test
    void testInsertRowValid() {
        String result = excelWriteHandler.excelInsertRow(testExcel.toString(), "Sheet1", 1);
        assertTrue(result.contains("插入空行"));
    }

    @Test
    void testInsertRowOutOfRange() {
        String result = excelWriteHandler.excelInsertRow(testExcel.toString(), "Sheet1", 100);
        assertTrue(result.contains("错误"));
    }

    // ==================== excelInsertColumn ====================

    @Test
    void testInsertColumnValid() {
        String result = excelWriteHandler.excelInsertColumn(testExcel.toString(), "Sheet1", 1);
        assertTrue(result.contains("插入空列"));
    }

    @Test
    void testInsertColumnNegativeIndex() {
        String result = excelWriteHandler.excelInsertColumn(testExcel.toString(), "Sheet1", -1);
        assertTrue(result.contains("错误"));
    }

    // ==================== excelDeleteSheet ====================

    @Test
    void testDeleteSheetValid() throws Exception {
        // 先创建第二个工作表，否则无法删除唯一的 Sheet1
        try (Workbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Sheet1");
            workbook.createSheet("Sheet2");
            Row headerRow = workbook.getSheet("Sheet1")
                                    .createRow(0);
            headerRow.createCell(0)
                     .setCellValue("姓名");
            try (java.io.OutputStream os = Files.newOutputStream(testExcel)) {
                workbook.write(os);
            }
        }
        String result = excelWriteHandler.excelDeleteSheet(testExcel.toString(), "Sheet2");
        assertTrue(result.contains("已删除"));
    }

    @Test
    void testDeleteSheetNotFound() {
        String result = excelWriteHandler.excelDeleteSheet(testExcel.toString(), "NonExistent");
        assertTrue(result.contains("错误"));
    }

    @Test
    void testDeleteSheetSingleSheet() {
        String result = excelWriteHandler.excelDeleteSheet(testExcel.toString(), "Sheet1");
        assertTrue(result.contains("错误"));
    }

    // ==================== excelRenameSheet ====================

    @Test
    void testRenameSheetValid() {
        String result = excelWriteHandler.excelRenameSheet(testExcel.toString(), "Sheet1", "新名称");
        assertTrue(result.contains("重命名"));
    }

    @Test
    void testRenameSheetOldNameNotFound() {
        String result = excelWriteHandler.excelRenameSheet(testExcel.toString(), "NonExistent", "新名称");
        assertTrue(result.contains("错误"));
    }

    @Test
    void testRenameSheetNewNameExists() throws Exception {
        // 创建有两个工作表的文件
        try (Workbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Sheet1");
            workbook.createSheet("Sheet2");
            Row headerRow = workbook.getSheet("Sheet1")
                                    .createRow(0);
            headerRow.createCell(0)
                     .setCellValue("姓名");
            try (java.io.OutputStream os = Files.newOutputStream(testExcel)) {
                workbook.write(os);
            }
        }
        String result = excelWriteHandler.excelRenameSheet(testExcel.toString(), "Sheet1", "Sheet2");
        assertTrue(result.contains("错误"));
    }

    // ==================== excelSplitByColumn ====================

    @Test
    void testSplitByColumn() {
        Path outputDir = tempDir.resolve("split_output");
        String result = excelWriteHandler.excelSplitByColumn(testExcel.toString(), "Sheet1", 2, outputDir.toString());
        assertTrue(result.contains("成功拆分"));
        assertTrue(Files.exists(outputDir));
    }

    // ==================== excelBatchWrite ====================

    @Test
    void testBatchWriteDeleteRow() {
        String operations = "[{\"type\":\"delete_row\",\"rowIndex\":1}]";
        String result = excelWriteHandler.excelBatchWrite(testExcel.toString(), "Sheet1", operations);
        assertTrue(result.contains("批量操作完成"));
    }

    @Test
    void testBatchWriteInsertRow() {
        String operations = "[{\"type\":\"insert_row\",\"rowIndex\":1}]";
        String result = excelWriteHandler.excelBatchWrite(testExcel.toString(), "Sheet1", operations);
        assertTrue(result.contains("批量操作完成"));
    }

    @Test
    void testBatchWriteMultipleOperations() {
        String operations = "[{\"type\":\"insert_row\",\"rowIndex\":0},{\"type\":\"delete_row\",\"rowIndex\":1}]";
        String result = excelWriteHandler.excelBatchWrite(testExcel.toString(), "Sheet1", operations);
        assertTrue(result.contains("批量操作完成"));
    }

    // ==================== excelExportToPdf ====================

    @Test
    void testExportToPdf() {
        Path targetPdf = tempDir.resolve("output.pdf");
        String result = excelWriteHandler.excelExportToPdf(testExcel.toString(), "Sheet1", targetPdf.toString());
        assertTrue(result.contains("Excel 已导出为 PDF"));
        assertTrue(Files.exists(targetPdf));
    }

    // ==================== excelAddImage ====================

    @Test
    void testAddImage() throws Exception {
        // 创建一个简单的测试图片
        Path imagePath = tempDir.resolve("test.png");
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", imagePath.toFile());

        String result = excelWriteHandler.excelAddImage(testExcel.toString(), "Sheet1", imagePath.toString(), 0, 0, 50, 50);
        assertTrue(result.contains("成功插入图片"));
    }
}