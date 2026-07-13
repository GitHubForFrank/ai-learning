package com.example.mcp.handler.excel;

import com.alibaba.fastjson2.JSON;
import com.example.mcp.util.LogUtil;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Excel 写操作 Handler，提供工作簿创建、数据写入、行列操作、工作表管理、拆分等功能。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class ExcelWriteHandler extends ExcelBaseHandler {

    // ==================== 1. excel_create_workbook ====================

    /**
     * 新建空白工作簿（.xlsx）。
     */
    @Tool(name = "excel_write_create_workbook", description = "创建新的空白 Excel 工作簿。如果文件已存在则覆盖。")
    public String excelCreateWorkbook(@ToolParam(description = "新 Excel 文件的绝对路径") String fileAbsolutePath) {
        return executeWithAudit("excel_create_workbook", "file=" + fileAbsolutePath, () -> {
            Path path = Paths.get(fileAbsolutePath);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (Workbook workbook = new XSSFWorkbook()) {
                workbook.createSheet("Sheet1");
                saveWorkbook(workbook, path);
            }
            return "空白工作簿已创建: " + fileAbsolutePath;
        });
    }

    // ==================== 2. excel_write_to_sheet ====================

    /**
     * 向 Excel 工作表写入值。
     */
    @Tool(name = "excel_write_data", description = "向 Excel 工作表写入数据")
    public String excelWriteToSheet(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName,
        @ToolParam(description = "true 表示创建新工作表，false 表示写入现有工作表") Boolean newSheet,
        @ToolParam(description = "Excel 工作表中的单元格范围（如 \"A1:C10\"）") String range,
        @ToolParam(description = "要写入工作表的值。如果值是公式，需以\"=\"开头。") List<List<Object>> values) {
        return executeWithAudit("excel_write_to_sheet", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", range=" + range, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            int[] bounds = parseRange(range);
            int firstRow = bounds[0];
            int firstCol = bounds[1];

            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet;
                if (Boolean.TRUE.equals(newSheet)) {
                    if (workbook.getSheet(sheetName) != null) {
                        return "错误: 工作表已存在 '" + sheetName + "'，请使用不同的名称或设置 newSheet=false";
                    }
                    sheet = workbook.createSheet(sheetName);
                } else {
                    sheet = getSheet(workbook, sheetName);
                }

                for (int r = 0; r < values.size(); r++) {
                    List<Object> rowValues = values.get(r);
                    int targetRow = firstRow + r;
                    Row row = sheet.getRow(targetRow);
                    if (row == null) {
                        row = sheet.createRow(targetRow);
                    }
                    for (int c = 0; c < rowValues.size(); c++) {
                        Cell cell = row.getCell(firstCol + c);
                        if (cell == null) {
                            cell = row.createCell(firstCol + c);
                        }
                        writeCellValue(cell, rowValues.get(c));
                    }
                }

                saveWorkbook(workbook, path);
                return "成功写入: 文件=" + fileAbsolutePath + ", 工作表=" + sheetName + ", 范围=" + range;
            }
        });
    }

    // ==================== 3. excel_copy_sheet ====================

    /**
     * 复制已有工作表到新工作表。
     */
    @Tool(name = "excel_write_copy_sheet", description = "将现有工作表复制到新工作表")
    public String excelCopySheet(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "源工作表名称") String srcSheetName, @ToolParam(description = "要复制为的工作表名称") String dstSheetName) {
        return executeWithAudit("excel_copy_sheet", "file=" + fileAbsolutePath + ", src=" + srcSheetName + ", dst=" + dstSheetName, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet srcSheet = getSheet(workbook, srcSheetName);
                if (workbook.getSheet(dstSheetName) != null) {
                    return "错误: 目标工作表已存在 '" + dstSheetName + "'";
                }
                int srcIndex = workbook.getSheetIndex(srcSheet);
                workbook.cloneSheet(srcIndex);
                int newIndex = workbook.getNumberOfSheets() - 1;
                workbook.setSheetName(newIndex, dstSheetName);
                saveWorkbook(workbook, path);
                return "成功复制: 从 '" + srcSheetName + "' 到 '" + dstSheetName + "', 文件=" + fileAbsolutePath;
            }
        });
    }

    // ==================== 4. excel_delete_row ====================

    /**
     * 删除指定行。
     */
    @Tool(name = "excel_write_delete_row", description = "从 Excel 工作表中删除指定行。行索引从 0 开始。")
    public String excelDeleteRow(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName, @ToolParam(description = "要删除的行索引（从0开始）") int rowIndex) {
        return executeWithAudit("excel_delete_row", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", rowIndex=" + rowIndex, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                if (rowIndex < 0 || rowIndex > sheet.getLastRowNum()) {
                    return "错误: 行索引 " + rowIndex + " 超出范围";
                }
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    sheet.removeRow(row);
                }
                if (rowIndex <= sheet.getLastRowNum()) {
                    int lastRow = sheet.getLastRowNum();
                    sheet.shiftRows(rowIndex + 1, lastRow, -1);
                }
                saveWorkbook(workbook, path);
                return "已删除第 " + (rowIndex + 1) + " 行: " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        });
    }

    // ==================== 5. excel_delete_column ====================

    /**
     * 删除指定列。
     */
    @Tool(name = "excel_write_delete_column", description = "从 Excel 工作表中删除指定列。列索引从 0 开始。")
    public String excelDeleteColumn(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName, @ToolParam(description = "要删除的列索引（从0开始）") int columnIndex) {
        return executeWithAudit("excel_delete_column", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", columnIndex=" + columnIndex, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                if (columnIndex < 0) {
                    return "错误: 列索引不能为负数";
                }
                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row != null) {
                        Cell cell = row.getCell(columnIndex);
                        if (cell != null) {
                            row.removeCell(cell);
                        }
                        int lastCol = row.getLastCellNum();
                        for (int c = columnIndex; c < lastCol - 1; c++) {
                            Cell srcCell = row.getCell(c + 1);
                            Cell destCell = row.getCell(c);
                            if (destCell == null) {
                                destCell = row.createCell(c);
                            }
                            if (srcCell != null) {
                                switch (srcCell.getCellType()) {
                                    case STRING -> destCell.setCellValue(srcCell.getStringCellValue());
                                    case NUMERIC -> destCell.setCellValue(srcCell.getNumericCellValue());
                                    case BOOLEAN -> destCell.setCellValue(srcCell.getBooleanCellValue());
                                    case FORMULA -> destCell.setCellFormula(srcCell.getCellFormula());
                                    default -> destCell.setBlank();
                                }
                            }
                        }
                        Cell lastCell = row.getCell(lastCol - 1);
                        if (lastCell != null) {
                            row.removeCell(lastCell);
                        }
                    }
                }
                saveWorkbook(workbook, path);
                return "已删除第 " + (columnIndex + 1) + " 列: " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        });
    }

    // ==================== 6. excel_clear_sheet ====================

    /**
     * 清空整张表格数据。
     */
    @Tool(name = "excel_write_clear_sheet", description = "清空 Excel 工作表中的所有数据，移除所有行但保留工作表结构。")
    public String excelClearSheet(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "要清空的工作表名称") String sheetName) {
        return executeWithAudit("excel_clear_sheet", "file=" + fileAbsolutePath + ", sheet=" + sheetName, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                int lastRow = sheet.getLastRowNum();
                for (int r = lastRow; r >= 0; r--) {
                    Row row = sheet.getRow(r);
                    if (row != null) {
                        sheet.removeRow(row);
                    }
                }
                saveWorkbook(workbook, path);
                return "工作表 '" + sheetName + "' 数据已清空: " + fileAbsolutePath;
            }
        });
    }

    // ==================== 7. excel_insert_row ====================

    /**
     * 在指定位置插入空行。
     */
    @Tool(name = "excel_write_insert_row", description = "在指定位置插入空行，现有行向下移动。行索引从 0 开始。")
    public String excelInsertRow(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName, @ToolParam(description = "要插入的行索引（从0开始）") int rowIndex) {
        return executeWithAudit("excel_insert_row", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", rowIndex=" + rowIndex, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                int lastRow = sheet.getLastRowNum();
                if (rowIndex < 0 || rowIndex > lastRow + 1) {
                    return "错误: 行索引 " + rowIndex + " 超出范围（0~" + (lastRow + 1) + "）";
                }
                if (rowIndex <= lastRow) {
                    sheet.shiftRows(rowIndex, lastRow, 1);
                }
                sheet.createRow(rowIndex);
                saveWorkbook(workbook, path);
                return "已在第 " + (rowIndex + 1) + " 行插入空行: " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        });
    }

    // ==================== 8. excel_insert_column ====================

    /**
     * 在指定位置插入空列。
     */
    @Tool(name = "excel_write_insert_column", description = "在指定位置插入空列，现有列向右移动。列索引从 0 开始。")
    public String excelInsertColumn(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName, @ToolParam(description = "要插入的列索引（从0开始）") int columnIndex) {
        return executeWithAudit("excel_insert_column", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", columnIndex=" + columnIndex, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                if (columnIndex < 0) {
                    return "错误: 列索引不能为负数";
                }
                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row != null) {
                        int lastCol = row.getLastCellNum();
                        for (int c = lastCol; c > columnIndex; c--) {
                            Cell srcCell = row.getCell(c - 1);
                            Cell destCell = row.getCell(c);
                            if (destCell == null) {
                                destCell = row.createCell(c);
                            }
                            if (srcCell != null) {
                                copyCellValue(srcCell, destCell);
                            } else {
                                destCell.setBlank();
                            }
                        }
                        Cell insertCell = row.getCell(columnIndex);
                        if (insertCell != null) {
                            insertCell.setBlank();
                        }
                    }
                }
                saveWorkbook(workbook, path);
                return "已在第 " + (columnIndex + 1) + " 列插入空列: " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        });
    }

    // ==================== 9. excel_delete_sheet ====================

    /**
     * 删除工作表。
     */
    @Tool(name = "excel_write_delete_sheet", description = "删除 Excel 工作簿中指定名称的工作表。")
    public String excelDeleteSheet(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "要删除的工作表名称") String sheetName) {
        return executeWithAudit("excel_delete_sheet", "file=" + fileAbsolutePath + ", sheet=" + sheetName, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    return "错误: 工作表不存在 '" + sheetName + "'";
                }
                if (workbook.getNumberOfSheets() <= 1) {
                    return "错误: 工作簿至少需要保留一个工作表";
                }
                int idx = workbook.getSheetIndex(sheet);
                workbook.removeSheetAt(idx);
                saveWorkbook(workbook, path);
                return "已删除工作表 '" + sheetName + "': " + fileAbsolutePath;
            }
        });
    }

    // ==================== 10. excel_rename_sheet ====================

    /**
     * 重命名工作表。
     */
    @Tool(name = "excel_write_rename_sheet", description = "重命名 Excel 工作簿中的工作表。")
    public String excelRenameSheet(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "当前工作表名称") String oldName, @ToolParam(description = "新工作表名称") String newName) {
        return executeWithAudit("excel_rename_sheet", "file=" + fileAbsolutePath + ", old=" + oldName + ", new=" + newName, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = workbook.getSheet(oldName);
                if (sheet == null) {
                    return "错误: 工作表不存在 '" + oldName + "'";
                }
                if (workbook.getSheet(newName) != null) {
                    return "错误: 新名称 '" + newName + "' 已存在";
                }
                int idx = workbook.getSheetIndex(sheet);
                workbook.setSheetName(idx, newName);
                saveWorkbook(workbook, path);
                return "已将工作表 '" + oldName + "' 重命名为 '" + newName + "': " + fileAbsolutePath;
            }
        });
    }

    // ==================== 11. excel_split_by_column ====================

    /**
     * 按指定列的值将工作表拆分为多个独立的 Excel 文件。
     */
    @Tool(name = "excel_write_split_by_column", description = "按指定列的值将工作表拆分为多个独立的 Excel 文件，每个唯一值生成一个文件。")
    public String excelSplitByColumn(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "工作表名称") String sheetName, @ToolParam(description = "按哪一列拆分（0-based 列索引）") int columnIndex,
        @ToolParam(description = "输出目录的绝对路径") String outputDir) {
        return executeWithAudit("excel_split_by_column", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", columnIndex=" + columnIndex, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                Map<String, List<RowData>> groups = new LinkedHashMap<>();
                List<String> headers = new ArrayList<>();
                int maxCol = 0;
                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    int lastCol = row.getLastCellNum();
                    if (lastCol > maxCol) {
                        maxCol = lastCol;
                    }
                    if (r == 0) {
                        for (int c = 0; c < lastCol; c++) {
                            headers.add(getCellStringValue(row.getCell(c), false));
                        }
                        continue;
                    }
                    String key = getCellStringValue(row.getCell(columnIndex), false);
                    groups.computeIfAbsent(key, k -> new ArrayList<>());
                    List<Object> rowValues = new ArrayList<>();
                    for (int c = 0; c < maxCol; c++) {
                        rowValues.add(getCellJsonValue(row.getCell(c), false));
                    }
                    groups.get(key)
                          .add(new RowData(rowValues));
                }
                Path outDir = Paths.get(outputDir);
                if (!Files.exists(outDir)) {
                    Files.createDirectories(outDir);
                }
                int fileCount = 0;
                for (Map.Entry<String, List<RowData>> entry : groups.entrySet()) {
                    String groupKey = entry.getKey();
                    String safeName = groupKey.replaceAll("[\\\\/:*?\"<>|]", "_");
                    Path outFile = outDir.resolve(safeName + ".xlsx");
                    try (Workbook newWorkbook = new XSSFWorkbook()) {
                        Sheet newSheet = newWorkbook.createSheet("Sheet1");
                        Row headerRow = newSheet.createRow(0);
                        for (int c = 0; c < headers.size(); c++) {
                            headerRow.createCell(c)
                                     .setCellValue(headers.get(c));
                        }
                        int rowIdx = 1;
                        for (RowData rd : entry.getValue()) {
                            Row dataRow = newSheet.createRow(rowIdx++);
                            for (int c = 0; c < rd.values()
                                                  .size(); c++) {
                                Cell cell = dataRow.createCell(c);
                                writeCellValue(cell, rd.values()
                                                       .get(c));
                            }
                        }
                        try (OutputStream os = Files.newOutputStream(outFile)) {
                            newWorkbook.write(os);
                        }
                    }
                    fileCount++;
                }
                LogUtil.info("excel_split_by_column 成功: 拆分为 {} 个文件, 输出目录={}", fileCount, outputDir);
                return "成功拆分: 共生成 " + fileCount + " 个文件, 按列 " + (columnIndex + 1) + " 拆分, 输出目录=" + outputDir;
            }
        });
    }

    // ==================== 12. excel_add_image (新增) ====================

    /**
     * 在工作表中插入图片到指定单元格。
     */
    @Tool(name = "excel_write_add_image", description = "在工作表中插入图片到指定单元格位置。")
    public String excelAddImage(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "工作表名称") String sheetName, @ToolParam(description = "图片文件路径") String imagePath,
        @ToolParam(description = "目标行索引（0-based）") int rowIndex, @ToolParam(description = "目标列索引（0-based）") int colIndex,
        @ToolParam(description = "图片宽度（像素），默认100", required = false) Integer width,
        @ToolParam(description = "图片高度（像素），默认100", required = false) Integer height) {
        return executeWithAudit("excel_add_image", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", image=" + imagePath, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            Path imgPath = Paths.get(imagePath);
            if (!Files.exists(imgPath)) {
                return "错误: 图片文件不存在: " + imagePath;
            }
            int w = width != null ? width : 100;
            int h = height != null ? height : 100;

            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                // 读取图片字节
                byte[] imageBytes;
                try (FileInputStream fis = new FileInputStream(imgPath.toFile())) {
                    imageBytes = IOUtils.toByteArray(fis);
                }
                // 添加图片到工作簿
                int pictureIdx = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
                Drawing<?> drawing = sheet.createDrawingPatriarch();
                ClientAnchor anchor = workbook.getCreationHelper()
                                              .createClientAnchor();
                anchor.setCol1(colIndex);
                anchor.setRow1(rowIndex);
                anchor.setCol2(colIndex + 1);
                anchor.setRow2(rowIndex + 1);
                drawing.createPicture(anchor, pictureIdx);
                // 调整图片大小（通过锚点偏移量模拟）
                anchor.setDx2(w * 9525 / 100);  // 像素转 EMU 近似值
                anchor.setDy2(h * 9525 / 100);

                saveWorkbook(workbook, path);
                return "成功插入图片: 行=" + (rowIndex + 1) + ", 列=" + (colIndex + 1) + ", 文件=" + fileAbsolutePath;
            }
        });
    }

    // ==================== 13. excel_batch_write (新增) ====================

    /**
     * 批量写入操作，支持 delete_row、insert_row 等操作。
     */
    @Tool(name = "excel_write_batch", description = "批量写入操作，支持 delete_row、insert_row、delete_column、insert_column 等操作。通过 JSON 数组指定操作序列。")
    public String excelBatchWrite(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "工作表名称") String sheetName,
        @ToolParam(description = "操作列表，JSON数组格式，如 [{\\\"type\\\":\\\"delete_row\\\",\\\"rowIndex\\\":0},{\\\"type\\\":\\\"insert_row\\\",\\\"rowIndex\\\":1}]") String operations) {
        return executeWithAudit("excel_batch_write", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", ops=" + operations, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            @SuppressWarnings("unchecked") List<Map<String, Object>> ops = (List<Map<String, Object>>) (List<?>) JSON.parseArray(operations,
                                                                                                                                 Map.class);
            if (ops == null || ops.isEmpty()) {
                return "错误: 操作列表为空";
            }

            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                int successCount = 0;
                List<String> errors = new ArrayList<>();

                for (int i = 0; i < ops.size(); i++) {
                    Map<String, Object> op = ops.get(i);
                    String type = (String) op.get("type");
                    if (type == null) {
                        errors.add("操作 " + i + ": 缺少 type 字段");
                        continue;
                    }
                    try {
                        switch (type) {
                            case "delete_row" -> {
                                int rowIdx = getIntParam(op, "rowIndex");
                                if (rowIdx >= 0 && rowIdx <= sheet.getLastRowNum()) {
                                    Row row = sheet.getRow(rowIdx);
                                    if (row != null) {
                                        sheet.removeRow(row);
                                    }
                                    if (rowIdx <= sheet.getLastRowNum()) {
                                        sheet.shiftRows(rowIdx + 1, sheet.getLastRowNum(), -1);
                                    }
                                    successCount++;
                                }
                            }
                            case "insert_row" -> {
                                int rowIdx = getIntParam(op, "rowIndex");
                                int lastRow = sheet.getLastRowNum();
                                if (rowIdx >= 0 && rowIdx <= lastRow + 1) {
                                    if (rowIdx <= lastRow) {
                                        sheet.shiftRows(rowIdx, lastRow, 1);
                                    }
                                    sheet.createRow(rowIdx);
                                    successCount++;
                                }
                            }
                            case "delete_column" -> {
                                int colIdx = getIntParam(op, "columnIndex");
                                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                                    Row row = sheet.getRow(r);
                                    if (row != null) {
                                        Cell cell = row.getCell(colIdx);
                                        if (cell != null) {
                                            row.removeCell(cell);
                                        }
                                        int lastCol = row.getLastCellNum();
                                        for (int c = colIdx; c < lastCol - 1; c++) {
                                            Cell srcCell = row.getCell(c + 1);
                                            Cell destCell = row.getCell(c);
                                            if (destCell == null) {
                                                destCell = row.createCell(c);
                                            }
                                            if (srcCell != null) {
                                                copyCellValue(srcCell, destCell);
                                            }
                                        }
                                    }
                                }
                                successCount++;
                            }
                            case "insert_column" -> {
                                int colIdx = getIntParam(op, "columnIndex");
                                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                                    Row row = sheet.getRow(r);
                                    if (row != null) {
                                        int lastCol = row.getLastCellNum();
                                        for (int c = lastCol; c > colIdx; c--) {
                                            Cell srcCell = row.getCell(c - 1);
                                            Cell destCell = row.getCell(c);
                                            if (destCell == null) {
                                                destCell = row.createCell(c);
                                            }
                                            if (srcCell != null) {
                                                copyCellValue(srcCell, destCell);
                                            } else {
                                                destCell.setBlank();
                                            }
                                        }
                                    }
                                }
                                successCount++;
                            }
                            default -> errors.add("操作 " + i + ": 不支持的操作类型 '" + type + "'");
                        }
                    } catch (Exception e) {
                        errors.add("操作 " + i + " (" + type + "): " + e.getMessage());
                    }
                }

                saveWorkbook(workbook, path);
                StringBuilder result = new StringBuilder(
                    "批量操作完成: 成功 " + successCount + " 个, 文件=" + fileAbsolutePath + ", 工作表=" + sheetName);
                if (!errors.isEmpty()) {
                    result.append("\n错误: ")
                          .append(String.join("; ", errors));
                }
                return result.toString();
            }
        });
    }

    private int getIntParam(Map<String, Object> op, String key) {
        Object val = op.get(key);
        if (val instanceof Number num) {
            return num.intValue();
        }
        return Integer.parseInt(String.valueOf(val));
    }

    // ==================== 14. excel_export_to_pdf (新增) ====================

    /**
     * 将 Excel 工作表导出为 PDF 文件。
     * 使用 Apache POI 将 sheet 渲染为图片，再用 PDFBox 合并为 PDF。
     */
    @Tool(name = "excel_write_export_to_pdf", description = "将 Excel 工作表导出为 PDF 文件，每页渲染为图片后合并为 PDF。")
    public String excelExportToPdf(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "工作表名称") String sheetName, @ToolParam(description = "目标 PDF 文件路径") String targetPath) {
        return executeWithAudit("excel_export_to_pdf", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", target=" + targetPath, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            Path tgtPath = Paths.get(targetPath);
            Path tgtParent = tgtPath.getParent();
            if (tgtParent != null && !Files.exists(tgtParent)) {
                Files.createDirectories(tgtParent);
            }

            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                int totalRows = sheet.getLastRowNum() + 1;
                if (totalRows <= 0) {
                    return "错误: 工作表为空";
                }

                // 渲染整个工作表为图片
                int maxCol = 0;
                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row != null && row.getLastCellNum() > maxCol) {
                        maxCol = row.getLastCellNum();
                    }
                }
                maxCol = Math.max(1, maxCol);

                // 分页渲染：每页最多 SCREENSHOT_MAX_ROWS 行
                int rowsPerPage = SCREENSHOT_MAX_ROWS;
                int totalPages = (int) Math.ceil((double) totalRows / rowsPerPage);

                PDDocument pdfDoc = new PDDocument();

                for (int page = 0; page < totalPages; page++) {
                    int pageFirstRow = page * rowsPerPage;
                    int pageLastRow = Math.min(pageFirstRow + rowsPerPage - 1, sheet.getLastRowNum());
                    int pageFirstCol = 0;
                    int pageLastCol = Math.min(maxCol - 1, SCREENSHOT_MAX_COLS - 1);

                    BufferedImage image = renderSheetToImage(sheet, pageFirstRow, pageFirstCol, pageLastRow, pageLastCol);

                    float imgWidth = image.getWidth();
                    float imgHeight = image.getHeight();
                    PDPage pdPage = new PDPage(new PDRectangle(imgWidth, imgHeight));
                    pdfDoc.addPage(pdPage);
                    PDImageXObject pdImage = LosslessFactory.createFromImage(pdfDoc, image);
                    try (PDPageContentStream cs = new PDPageContentStream(pdfDoc, pdPage)) {
                        cs.drawImage(pdImage, 0, 0, imgWidth, imgHeight);
                    }
                }

                pdfDoc.save(tgtPath.toFile());
                pdfDoc.close();
                LogUtil.info("excel_export_to_pdf 成功: 文件={}, 目标={}, 共{}页", fileAbsolutePath, targetPath, totalPages);
                return "Excel 已导出为 PDF: " + targetPath + " (共 " + totalPages + " 页)";
            }
        });
    }
}