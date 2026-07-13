package com.example.mcp.handler.excel;

import com.alibaba.fastjson2.JSON;
import com.example.mcp.util.LogUtil;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Excel 只读操作 Handler，提供工作表信息查看、数据读取、截图、数据分析和对比功能。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class ExcelReadHandler extends ExcelBaseHandler {

    // ==================== 1. excel_describe_sheets ====================

    /**
     * 列出指定 Excel 文件的所有工作表信息。
     */
    @Tool(name = "excel_read_describe_sheets", description = "列出指定 Excel 文件的所有工作表信息")
    public String excelDescribeSheets(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath) {
        return executeWithAudit("excel_describe_sheets", "file=" + fileAbsolutePath, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            List<Map<String, Object>> sheets = new ArrayList<>();
            try (Workbook workbook = openWorkbook(path)) {
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("name", sheet.getSheetName());
                    info.put("index", i);
                    info.put("rowCount", sheet.getLastRowNum() + 1);
                    int maxCols = 0;
                    for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
                        Row row = sheet.getRow(r);
                        if (row != null && row.getLastCellNum() > maxCols) {
                            maxCols = row.getLastCellNum();
                        }
                    }
                    info.put("columnCount", maxCols);
                    sheets.add(info);
                }
            }
            return JSON.toJSONString(sheets);
        });
    }

    // ==================== 2. excel_read_sheet ====================

    /**
     * 读取 Excel 工作表中的值，支持分页、显示公式和样式。
     */
    @Tool(name = "excel_read_sheet", description = "从 Excel 工作表分页读取数据，支持 offset/limit 分页参数。")
    public String excelReadSheet(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName,
        @ToolParam(description = "要读取的单元格范围（如 \"A1:C10\"），默认：首个分页范围", required = false) String range,
        @ToolParam(description = "显示公式而非值", required = false) Boolean showFormula,
        @ToolParam(description = "显示单元格样式信息", required = false) Boolean showStyle,
        @ToolParam(description = "分页偏移量（跳过的行数），默认0", required = false) Integer offset,
        @ToolParam(description = "每页最大行数，默认100", required = false) Integer limit) {
        return executeWithAudit("excel_read_sheet", "file=" + fileAbsolutePath + ", sheet=" + sheetName, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            boolean showF = showFormula != null && showFormula;
            boolean showS = showStyle != null && showStyle;
            int pageOffset = offset != null ? offset : 0;
            int pageLimit = limit != null ? limit : DEFAULT_PAGE_SIZE;

            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);

                int firstRow, firstCol, lastRow, lastCol;
                if (range != null && !range.isBlank()) {
                    int[] bounds = parseRange(range);
                    firstRow = bounds[0];
                    firstCol = bounds[1];
                    lastRow = bounds[2];
                    lastCol = bounds[3];
                } else {
                    firstRow = pageOffset;
                    firstCol = 0;
                    lastRow = Math.min(sheet.getLastRowNum(), firstRow + pageLimit - 1);
                    int maxCol = 0;
                    for (int r = firstRow; r <= lastRow; r++) {
                        Row row = sheet.getRow(r);
                        if (row != null && row.getLastCellNum() > maxCol) {
                            maxCol = row.getLastCellNum();
                        }
                    }
                    lastCol = Math.max(0, maxCol - 1);
                }

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("sheetName", sheetName);
                result.put("range",
                           CellReference.convertNumToColString(firstCol) + (firstRow + 1) + ":" + CellReference.convertNumToColString(lastCol) + (
                               lastRow + 1));

                List<List<Object>> cells = new ArrayList<>();
                for (int r = firstRow; r <= lastRow; r++) {
                    Row row = sheet.getRow(r);
                    List<Object> rowData = new ArrayList<>();
                    for (int c = firstCol; c <= lastCol; c++) {
                        if (row == null) {
                            rowData.add(showS ? Map.of("value", null, "style", null) : null);
                            continue;
                        }
                        Cell cell = row.getCell(c);
                        if (showS) {
                            Map<String, Object> cellMap = new LinkedHashMap<>();
                            cellMap.put("value", getCellJsonValue(cell, showF));
                            cellMap.put("style", getCellStyleMap(cell));
                            rowData.add(cellMap);
                        } else {
                            rowData.add(getCellJsonValue(cell, showF));
                        }
                    }
                    cells.add(rowData);
                }
                result.put("cells", cells);

                int totalRows = sheet.getLastRowNum() + 1;
                result.put("totalRows", totalRows);
                result.put("hasMore", lastRow + 1 < totalRows);
                result.put("offset", pageOffset);
                result.put("limit", pageLimit);

                return JSON.toJSONString(result);
            }
        });
    }

    // ==================== 3. excel_screen_capture ====================

    /**
     * 截取 Excel 工作表的屏幕截图，返回 base64 编码的 PNG 图片。
     */
    @Tool(name = "excel_read_screen_capture", description = "截取 Excel 工作表的屏幕截图，支持分页。")
    public String excelScreenCapture(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName,
        @ToolParam(description = "要读取的单元格范围（如 \"A1:C10\"），默认：首个分页范围", required = false) String range) {
        return executeWithAudit("excel_screen_capture", "file=" + fileAbsolutePath + ", sheet=" + sheetName, () -> {
            Path path = validateExcelFile(fileAbsolutePath);

            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);

                int firstRow, firstCol, lastRow, lastCol;
                if (range != null && !range.isBlank()) {
                    int[] bounds = parseRange(range);
                    firstRow = bounds[0];
                    firstCol = bounds[1];
                    lastRow = bounds[2];
                    lastCol = bounds[3];
                } else {
                    firstRow = 0;
                    firstCol = 0;
                    lastRow = Math.min(sheet.getLastRowNum(), SCREENSHOT_MAX_ROWS - 1);
                    int maxCol = 0;
                    for (int r = firstRow; r <= lastRow; r++) {
                        Row row = sheet.getRow(r);
                        if (row != null && row.getLastCellNum() > maxCol) {
                            maxCol = row.getLastCellNum();
                        }
                    }
                    lastCol = Math.max(0, Math.min(maxCol - 1, SCREENSHOT_MAX_COLS - 1));
                }

                BufferedImage image = renderSheetToImage(sheet, firstRow, firstCol, lastRow, lastCol);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                String base64 = Base64.getEncoder()
                                      .encodeToString(baos.toByteArray());

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("mimeType", "image/png");
                result.put("sheetName", sheetName);
                result.put("range",
                           CellReference.convertNumToColString(firstCol) + (firstRow + 1) + ":" + CellReference.convertNumToColString(lastCol) + (
                               lastRow + 1));
                result.put("data", base64);

                return JSON.toJSONString(result);
            }
        });
    }

    // ==================== 4. excel_analyze ====================

    /**
     * 对 Excel 工作表指定范围的数据进行摘要分析，自动识别数值列并计算 sum/avg/max/min/count。
     */
    @Tool(name = "excel_read_analyze", description = "对 Excel 工作表指定范围的数据进行摘要分析，自动识别数值列并计算汇总统计信息。")
    public String excelAnalyze(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "工作表名称") String sheetName, @ToolParam(description = "分析范围，如 \"A1:C50\"（第一行为标题行）") String range) {
        return executeWithAudit("excel_analyze", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", range=" + range, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            int[] bounds = parseRange(range);
            int firstRow = bounds[0];
            int firstCol = bounds[1];
            int lastRow = bounds[2];
            int lastCol = bounds[3];
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                Row headerRow = sheet.getRow(firstRow);
                List<String> headers = new ArrayList<>();
                for (int c = firstCol; c <= lastCol; c++) {
                    Cell cell = headerRow != null ? headerRow.getCell(c) : null;
                    headers.add(getCellStringValue(cell, false));
                }
                int colCount = lastCol - firstCol + 1;
                boolean[] isNumeric = new boolean[colCount];
                List<List<Double>> numericValues = new ArrayList<>();
                for (int c = 0; c < colCount; c++) {
                    numericValues.add(new ArrayList<>());
                }
                int dataRowCount = 0;
                for (int r = firstRow + 1; r <= lastRow; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    dataRowCount++;
                    for (int c = firstCol; c <= lastCol; c++) {
                        Cell cell = row.getCell(c);
                        if (cell != null && cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
                            isNumeric[c - firstCol] = true;
                            numericValues.get(c - firstCol)
                                         .add(cell.getNumericCellValue());
                        } else if (cell != null && cell.getCellType() == CellType.FORMULA) {
                            try {
                                double val = cell.getNumericCellValue();
                                isNumeric[c - firstCol] = true;
                                numericValues.get(c - firstCol)
                                             .add(val);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
                List<Map<String, Object>> columnStats = new ArrayList<>();
                for (int c = 0; c < colCount; c++) {
                    Map<String, Object> stat = new LinkedHashMap<>();
                    stat.put("column", headers.get(c));
                    stat.put("index", c);
                    List<Double> vals = numericValues.get(c);
                    if (isNumeric[c] && !vals.isEmpty()) {
                        double sum = 0;
                        double min = Double.MAX_VALUE;
                        double max = -Double.MAX_VALUE;
                        for (double v : vals) {
                            sum += v;
                            if (v < min) {
                                min = v;
                            }
                            if (v > max) {
                                max = v;
                            }
                        }
                        stat.put("type", "numeric");
                        stat.put("count", vals.size());
                        stat.put("sum", Math.round(sum * 100.0) / 100.0);
                        stat.put("avg", Math.round(sum / vals.size() * 100.0) / 100.0);
                        stat.put("max", max);
                        stat.put("min", min);
                    } else {
                        stat.put("type", "text");
                        stat.put("count", dataRowCount);
                    }
                    columnStats.add(stat);
                }
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("sheetName", sheetName);
                result.put("range", range);
                result.put("totalDataRows", dataRowCount);
                result.put("totalColumns", colCount);
                result.put("columns", columnStats);
                LogUtil.info("excel_analyze 成功: 文件={}, 范围={}", fileAbsolutePath, range);
                return JSON.toJSONString(result);
            }
        });
    }

    // ==================== 5. excel_compare ====================

    /**
     * 对比两个 Excel 工作表，按关键列匹配行并找出新增、删除、修改的行。
     */
    @Tool(name = "excel_read_compare", description = "对比两个 Excel 工作表，按关键列匹配行并找出新增、删除、修改的行，返回差异报告。")
    public String excelCompare(@ToolParam(description = "第一个 Excel 文件的绝对路径") String filePath1,
        @ToolParam(description = "第一个工作表名称") String sheetName1, @ToolParam(description = "第二个 Excel 文件的绝对路径") String filePath2,
        @ToolParam(description = "第二个工作表名称") String sheetName2,
        @ToolParam(description = "用于匹配行的关键列索引（0-based）") int keyColumnIndex) {
        return executeWithAudit("excel_compare", "file1=" + filePath1 + ", file2=" + filePath2, () -> {
            Path path1 = validateExcelFile(filePath1);
            Path path2 = validateExcelFile(filePath2);
            try (Workbook wb1 = openWorkbook(path1); Workbook wb2 = openWorkbook(path2)) {
                Sheet sheet1 = getSheet(wb1, sheetName1);
                Sheet sheet2 = getSheet(wb2, sheetName2);
                Map<String, List<Object>> map1 = new LinkedHashMap<>();
                readSheetToMap(sheet1, keyColumnIndex, map1);
                Map<String, List<Object>> map2 = new LinkedHashMap<>();
                readSheetToMap(sheet2, keyColumnIndex, map2);
                List<Map<String, Object>> added = new ArrayList<>();
                List<Map<String, Object>> removed = new ArrayList<>();
                List<Map<String, Object>> modified = new ArrayList<>();
                for (Map.Entry<String, List<Object>> entry : map2.entrySet()) {
                    if (!map1.containsKey(entry.getKey())) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("key", entry.getKey());
                        item.put("row", entry.getValue());
                        added.add(item);
                    }
                }
                for (Map.Entry<String, List<Object>> entry : map1.entrySet()) {
                    if (!map2.containsKey(entry.getKey())) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("key", entry.getKey());
                        item.put("row", entry.getValue());
                        removed.add(item);
                    }
                }
                for (Map.Entry<String, List<Object>> entry : map1.entrySet()) {
                    String key = entry.getKey();
                    if (map2.containsKey(key)) {
                        List<Object> row1 = entry.getValue();
                        List<Object> row2 = map2.get(key);
                        if (!rowValuesEqual(row1, row2)) {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("key", key);
                            item.put("oldRow", row1);
                            item.put("newRow", row2);
                            modified.add(item);
                        }
                    }
                }
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("file1", filePath1 + ":" + sheetName1);
                result.put("file2", filePath2 + ":" + sheetName2);
                result.put("keyColumnIndex", keyColumnIndex);
                result.put("totalRows1", map1.size());
                result.put("totalRows2", map2.size());
                result.put("addedCount", added.size());
                result.put("removedCount", removed.size());
                result.put("modifiedCount", modified.size());
                result.put("added", added);
                result.put("removed", removed);
                result.put("modified", modified);
                LogUtil.info("excel_compare 成功: 新增={}, 删除={}, 修改={}", added.size(), removed.size(), modified.size());
                return JSON.toJSONString(result);
            }
        });
    }

    // ==================== 6. excel_preview (新增) ====================

    /**
     * 预览工作表，返回前N行数据的 Markdown 表格格式。
     */
    @Tool(name = "excel_read_preview", description = "预览工作表，返回前N行数据的 Markdown 表格格式。")
    public String excelPreview(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "工作表名称") String sheetName, @ToolParam(description = "最大预览行数，默认20", required = false) Integer maxRows) {
        return executeWithAudit("excel_preview",
                                "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", maxRows=" + (maxRows != null ? maxRows : 20), () -> {
                Path path = validateExcelFile(fileAbsolutePath);
                int previewRows = maxRows != null ? maxRows : 20;

                try (Workbook workbook = openWorkbook(path)) {
                    Sheet sheet = getSheet(workbook, sheetName);
                    int lastRow = Math.min(sheet.getLastRowNum(), previewRows - 1);
                    if (lastRow < 0) {
                        return "工作表为空";
                    }

                    // 计算最大列数
                    int maxCol = 0;
                    for (int r = 0; r <= lastRow; r++) {
                        Row row = sheet.getRow(r);
                        if (row != null && row.getLastCellNum() > maxCol) {
                            maxCol = row.getLastCellNum();
                        }
                    }
                    if (maxCol == 0) {
                        return "工作表为空";
                    }

                    // 构建 Markdown 表格
                    StringBuilder sb = new StringBuilder();
                    // 标题行
                    sb.append("|");
                    for (int c = 0; c < maxCol; c++) {
                        Row headerRow = sheet.getRow(0);
                        String header = headerRow != null ? getCellStringValue(headerRow.getCell(c), false) : "列" + (c + 1);
                        sb.append(" ")
                          .append(header.isEmpty() ? "列" + (c + 1) : header)
                          .append(" |");
                    }
                    sb.append("\n");

                    // 分隔行
                    sb.append("|");
                    for (int c = 0; c < maxCol; c++) {
                        sb.append(" --- |");
                    }
                    sb.append("\n");

                    // 数据行
                    for (int r = 1; r <= lastRow; r++) {
                        sb.append("|");
                        Row row = sheet.getRow(r);
                        for (int c = 0; c < maxCol; c++) {
                            String val = row != null ? getCellStringValue(row.getCell(c), false) : "";
                            // 转义 Markdown 中的管道符
                            val = val.replace("|", "\\|");
                            sb.append(" ")
                              .append(val)
                              .append(" |");
                        }
                        sb.append("\n");
                    }

                    int totalRows = sheet.getLastRowNum() + 1;
                    if (totalRows > previewRows) {
                        sb.append("\n*（共 ")
                          .append(totalRows)
                          .append(" 行，仅显示前 ")
                          .append(previewRows)
                          .append(" 行）*\n");
                    }

                    return sb.toString();
                }
            });
    }
}