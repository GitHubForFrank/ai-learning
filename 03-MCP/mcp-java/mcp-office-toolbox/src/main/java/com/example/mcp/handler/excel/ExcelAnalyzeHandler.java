package com.example.mcp.handler.excel;

import com.example.mcp.util.CsvUtil;
import com.example.mcp.util.LogUtil;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataConsolidateFunction;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Excel 数据分析与处理 Handler，提供排序、筛选、查找替换、去重、公式应用、数据验证、CSV互转、工作簿合并、透视表、保护、分类汇总、分组折叠等功能。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class ExcelAnalyzeHandler extends ExcelBaseHandler {

    // ==================== 1. excel_sort_range ====================

    /**
     * 按指定列对 Excel 工作表数据区域进行排序。
     */
    @Tool(name = "excel_analyze_sort_range", description = "按指定列对 Excel 工作表数据区域进行排序。第一行为标题行不参与排序。")
    public String excelSortRange(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName,
        @ToolParam(description = "排序范围（必须包含标题行），如 \"A1:C10\"") String range,
        @ToolParam(description = "要排序的列索引（从0开始）") int sortColumnIndex,
        @ToolParam(description = "true 表示升序，false 表示降序") boolean ascending) {
        return executeWithAudit("excel_sort_range", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", range=" + range, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            int[] bounds = parseRange(range);
            int firstRow = bounds[0];
            int firstCol = bounds[1];
            int lastRow = bounds[2];
            int lastCol = bounds[3];
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                List<List<Object>> allRows = new ArrayList<>();
                for (int r = firstRow + 1; r <= lastRow; r++) {
                    Row row = sheet.getRow(r);
                    List<Object> rowData = new ArrayList<>();
                    for (int c = firstCol; c <= lastCol; c++) {
                        rowData.add(getCellJsonValue(row != null ? row.getCell(c) : null, false));
                    }
                    allRows.add(rowData);
                }
                int sortIdx = sortColumnIndex;
                allRows.sort((a, b) -> {
                    Object va = a.size() > sortIdx ? a.get(sortIdx) : null;
                    Object vb = b.size() > sortIdx ? b.get(sortIdx) : null;
                    if (va == null && vb == null) {
                        return 0;
                    }
                    if (va == null) {
                        return ascending ? 1 : -1;
                    }
                    if (vb == null) {
                        return ascending ? -1 : 1;
                    }
                    int cmp;
                    if (va instanceof Number na && vb instanceof Number nb) {
                        cmp = Double.compare(na.doubleValue(), nb.doubleValue());
                    } else {
                        cmp = String.valueOf(va)
                                    .compareTo(String.valueOf(vb));
                    }
                    return ascending ? cmp : -cmp;
                });
                for (int i = 0; i < allRows.size(); i++) {
                    int targetRow = firstRow + 1 + i;
                    Row row = sheet.getRow(targetRow);
                    if (row == null) {
                        row = sheet.createRow(targetRow);
                    }
                    List<Object> rowData = allRows.get(i);
                    for (int c = 0; c < rowData.size(); c++) {
                        Cell cell = row.getCell(firstCol + c);
                        if (cell == null) {
                            cell = row.createCell(firstCol + c);
                        }
                        writeCellValue(cell, rowData.get(c));
                    }
                }
                saveWorkbook(workbook, path);
                String dir = ascending ? "升序" : "降序";
                return "已按第 " + (sortColumnIndex + 1) + " 列" + dir + "排序: " + fileAbsolutePath + ", 工作表=" + sheetName + ", 范围=" + range;
            }
        });
    }

    // ==================== 2. excel_filter_range ====================

    /**
     * 为 Excel 工作表数据区域添加自动筛选。
     */
    @Tool(name = "excel_analyze_filter_range", description = "为 Excel 工作表数据区域添加自动筛选。")
    public String excelFilterRange(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName,
        @ToolParam(description = "要应用筛选的范围，如 \"A1:C10\"", required = false) String range) {
        return executeWithAudit("excel_filter_range", "file=" + fileAbsolutePath + ", sheet=" + sheetName, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                if (range != null && !range.isBlank()) {
                    int[] bounds = parseRange(range);
                    sheet.setAutoFilter(new CellRangeAddress(bounds[0], bounds[2], bounds[1], bounds[3]));
                } else {
                    int lastRow = sheet.getLastRowNum();
                    int lastCol = 0;
                    for (int r = 0; r <= lastRow; r++) {
                        Row row = sheet.getRow(r);
                        if (row != null && row.getLastCellNum() > lastCol) {
                            lastCol = row.getLastCellNum();
                        }
                    }
                    if (lastRow < 0 || lastCol == 0) {
                        return "错误: 工作表为空，无法自动添加筛选";
                    }
                    sheet.setAutoFilter(new CellRangeAddress(0, lastRow, 0, lastCol - 1));
                }
                saveWorkbook(workbook, path);
                return "已添加自动筛选: " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        });
    }

    // ==================== 3. excel_find_replace ====================

    /**
     * 在 Excel 工作表中查找并替换文本内容。
     */
    @Tool(name = "excel_analyze_find_replace", description = "在 Excel 工作表中查找并替换文本内容。")
    public String excelFindReplace(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName, @ToolParam(description = "要查找的文本") String findText,
        @ToolParam(description = "替换文本") String replaceText,
        @ToolParam(description = "搜索范围，如 \"A1:C10\"。留空则搜索整个工作表。", required = false) String range) {
        return executeWithAudit("excel_find_replace", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", find=" + findText, () -> {
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
                    lastRow = sheet.getLastRowNum();
                    lastCol = 0;
                    for (int r = firstRow; r <= lastRow; r++) {
                        Row row = sheet.getRow(r);
                        if (row != null && row.getLastCellNum() > lastCol) {
                            lastCol = row.getLastCellNum();
                        }
                    }
                    lastCol = Math.max(0, lastCol - 1);
                }
                int count = 0;
                for (int r = firstRow; r <= lastRow; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    for (int c = firstCol; c <= lastCol; c++) {
                        Cell cell = row.getCell(c);
                        if (cell != null && cell.getCellType() == CellType.STRING) {
                            String val = cell.getStringCellValue();
                            if (val.contains(findText)) {
                                cell.setCellValue(val.replace(findText, replaceText));
                                count++;
                            }
                        }
                    }
                }
                saveWorkbook(workbook, path);
                return "已替换 " + count + " 个单元格: 将 '" + findText + "' 替换为 '" + replaceText + "', 文件=" + fileAbsolutePath + ", 工作表="
                    + sheetName;
            }
        });
    }

    // ==================== 4. excel_remove_duplicates ====================

    /**
     * 按指定列去除 Excel 工作表中的重复行。
     */
    @Tool(name = "excel_analyze_remove_duplicates", description = "按指定列去除 Excel 工作表中的重复行，保留第一次出现的行。")
    public String excelRemoveDuplicates(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName,
        @ToolParam(description = "用于检查重复的列索引（从0开始），逗号分隔，如 \"0,1\"") String columnIndices,
        @ToolParam(description = "起始行索引（从0开始），默认：0（第一行为标题行）", required = false) Integer startRow) {
        return executeWithAudit("excel_remove_duplicates", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", cols=" + columnIndices, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            String[] colStr = columnIndices.split(",");
            int[] cols = new int[colStr.length];
            for (int i = 0; i < colStr.length; i++) {
                cols[i] = Integer.parseInt(colStr[i].trim());
            }
            int startRowIdx = startRow != null ? startRow : 0;
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                Set<String> seen = new HashSet<>();
                int removed = 0;
                for (int r = sheet.getLastRowNum(); r >= startRowIdx; r--) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    StringBuilder key = new StringBuilder();
                    for (int c : cols) {
                        Cell cell = row.getCell(c);
                        key.append(getCellStringValue(cell, false))
                           .append("\t");
                    }
                    if (!seen.add(key.toString())) {
                        sheet.removeRow(row);
                        if (r < sheet.getLastRowNum()) {
                            sheet.shiftRows(r + 1, sheet.getLastRowNum(), -1);
                        }
                        removed++;
                    }
                }
                saveWorkbook(workbook, path);
                return "已移除 " + removed + " 个重复行: " + fileAbsolutePath + ", 工作表=" + sheetName + ", 检查列=" + columnIndices;
            }
        });
    }

    // ==================== 5. excel_apply_formula ====================

    /**
     * 对 Excel 工作表中的单元格范围批量应用公式。
     */
    @Tool(name = "excel_analyze_apply_formula", description = "对 Excel 工作表中的单元格范围批量应用公式（如 SUM、AVERAGE 等）。")
    public String excelApplyFormula(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName,
        @ToolParam(description = "要应用公式的目标单元格或范围，如 \"D2\" 或 \"D2:D10\"") String target,
        @ToolParam(description = "要应用的公式（以=开头），如 \"=SUM(A2:A10)\"") String formula) {
        return executeWithAudit("excel_apply_formula", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", target=" + target, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                String formulaStr = formula.startsWith("=") ? formula.substring(1) : formula;
                int[] bounds = parseRange(target);
                for (int r = bounds[0]; r <= bounds[2]; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        row = sheet.createRow(r);
                    }
                    for (int c = bounds[1]; c <= bounds[3]; c++) {
                        Cell cell = row.getCell(c);
                        if (cell == null) {
                            cell = row.createCell(c);
                        }
                        cell.setCellFormula(formulaStr);
                    }
                }
                saveWorkbook(workbook, path);
                return "已应用公式 '" + formula + "' 到范围 " + target + ": " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        });
    }

    // ==================== 6. excel_data_validation ====================

    /**
     * 为 Excel 工作表单元格添加数据验证。
     */
    @Tool(name = "excel_analyze_data_validation", description = "为 Excel 工作表单元格添加数据验证，支持下拉列表。")
    public String excelDataValidation(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName,
        @ToolParam(description = "要应用验证的范围，如 \"A2:A10\"") String range,
        @ToolParam(description = "下拉列表的允许值，逗号分隔，如 \"是,否\"") String allowedValues) {
        return executeWithAudit("excel_data_validation", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", range=" + range, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                int[] bounds = parseRange(range);
                DataValidationHelper helper = sheet.getDataValidationHelper();
                DataValidationConstraint constraint = helper.createExplicitListConstraint(allowedValues.split(","));
                CellRangeAddressList addressList = new CellRangeAddressList(bounds[0], bounds[2], bounds[1], bounds[3]);
                DataValidation validation = helper.createValidation(constraint, addressList);
                validation.setShowErrorBox(true);
                validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                validation.createErrorBox("输入错误", "请从下拉列表中选择有效值");
                sheet.addValidationData(validation);
                saveWorkbook(workbook, path);
                return "已添加数据验证: 范围=" + range + ", 允许值=" + allowedValues + ", 文件=" + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        });
    }

    // ==================== 7. excel_convert_csv ====================

    /**
     * CSV 与 Excel 文件互转。
     */
    @Tool(name = "excel_analyze_convert_csv", description = "CSV 与 Excel 文件互转。根据源文件扩展名自动判断转换方向。")
    public String excelConvertCsv(@ToolParam(description = "源文件的绝对路径（.csv 或 .xlsx）") String sourceFilePath,
        @ToolParam(description = "目标文件的绝对路径（.xlsx 或 .csv）") String targetFilePath) {
        return executeWithAudit("excel_convert_csv", "src=" + sourceFilePath + ", tgt=" + targetFilePath, () -> {
            String srcLower = sourceFilePath.toLowerCase();
            String tgtLower = targetFilePath.toLowerCase();
            if (srcLower.endsWith(".csv") && tgtLower.endsWith(".xlsx")) {
                Path srcPath = Paths.get(sourceFilePath);
                if (!Files.exists(srcPath)) {
                    return "错误: 源文件不存在: " + sourceFilePath;
                }
                Path tgtPath = Paths.get(targetFilePath);
                Path parent = tgtPath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                try (BufferedReader reader = new BufferedReader(new FileReader(sourceFilePath)); Workbook workbook = new XSSFWorkbook()) {
                    Sheet sheet = workbook.createSheet("Sheet1");
                    String line;
                    int rowNum = 0;
                    while ((line = reader.readLine()) != null) {
                        Row row = sheet.createRow(rowNum++);
                        String[] parts = CsvUtil.parseCsvLine(line);
                        for (int i = 0; i < parts.length; i++) {
                            row.createCell(i)
                               .setCellValue(parts[i]);
                        }
                    }
                    saveWorkbook(workbook, tgtPath);
                }
                return "已从 CSV 转换为 Excel: " + sourceFilePath + " → " + targetFilePath;
            } else if ((srcLower.endsWith(".xlsx") || srcLower.endsWith(".xls")) && tgtLower.endsWith(".csv")) {
                Path srcPath = validateExcelFile(sourceFilePath);
                Path tgtPath = Paths.get(targetFilePath);
                Path parent = tgtPath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                try (Workbook workbook = openWorkbook(srcPath); FileWriter writer = new FileWriter(targetFilePath)) {
                    Sheet sheet = workbook.getSheetAt(0);
                    for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                        Row row = sheet.getRow(r);
                        StringBuilder sb = new StringBuilder();
                        if (row != null) {
                            for (int c = 0; c < row.getLastCellNum(); c++) {
                                if (c > 0) {
                                    sb.append(",");
                                }
                                Cell cell = row.getCell(c);
                                String val = getCellStringValue(cell, false);
                                if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                                    val = "\"" + val.replace("\"", "\"\"") + "\"";
                                }
                                sb.append(val);
                            }
                        }
                        writer.write(sb.toString() + "\n");
                    }
                }
                return "已从 Excel 转换为 CSV: " + sourceFilePath + " → " + targetFilePath;
            } else {
                return "错误: 不支持的文件格式组合。请使用 .csv → .xlsx 或 .xls/.xlsx → .csv";
            }
        });
    }

    // ==================== 8. excel_merge_workbooks ====================

    /**
     * 将多个 Excel 工作簿的所有工作表合并到一个目标工作簿中。
     */
    @Tool(name = "excel_analyze_merge_workbooks", description = "将多个 Excel 工作簿的所有工作表合并到一个目标工作簿中。")
    public String excelMergeWorkbooks(@ToolParam(description = "目标 Excel 文件的绝对路径（不存在则创建）") String targetFilePath,
        @ToolParam(description = "源 Excel 文件的绝对路径，逗号分隔") String sourceFilePaths,
        @ToolParam(description = "复制数据时是否包含标题行", required = false) Boolean includeHeader) {
        return executeWithAudit("excel_merge_workbooks", "target=" + targetFilePath + ", sources=" + sourceFilePaths, () -> {
            boolean includeHdr = includeHeader != null && includeHeader;
            String[] sources = sourceFilePaths.split(",");
            Path tgtPath = Paths.get(targetFilePath);
            Path parent = tgtPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (Workbook targetWorkbook = new XSSFWorkbook()) {
                int totalSheets = 0;
                for (String src : sources) {
                    Path srcFile = Paths.get(src.trim());
                    if (!Files.exists(srcFile)) {
                        return "错误: 源文件不存在: " + src.trim();
                    }
                    try (Workbook sourceWorkbook = openWorkbook(srcFile)) {
                        for (int i = 0; i < sourceWorkbook.getNumberOfSheets(); i++) {
                            Sheet srcSheet = sourceWorkbook.getSheetAt(i);
                            String sheetName = srcSheet.getSheetName();
                            int suffix = 1;
                            String baseName = sheetName;
                            while (targetWorkbook.getSheet(sheetName) != null) {
                                sheetName = baseName + "_" + (suffix++);
                            }
                            Sheet destSheet = targetWorkbook.createSheet(sheetName);
                            copySheetData(srcSheet, destSheet, includeHdr);
                            totalSheets++;
                        }
                    }
                }
                saveWorkbook(targetWorkbook, tgtPath);
                return "已合并 " + sources.length + " 个工作簿（共 " + totalSheets + " 个工作表）到: " + targetFilePath;
            }
        });
    }

    // ==================== 9. excel_pivot_table ====================

    /**
     * 在 Excel 中创建数据透视表。
     */
    @Tool(name = "excel_analyze_pivot_table", description = "在 Excel 中创建数据透视表，支持 SUM/COUNT/AVERAGE/MAX/MIN 汇总方式。")
    public String excelPivotTable(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "源数据工作表名称") String sheetName,
        @ToolParam(description = "源数据范围，如 \"A1:D50\"（第一行为标题行）") String sourceRange,
        @ToolParam(description = "透视表目标工作表名称") String targetSheetName,
        @ToolParam(description = "透视表起始单元格，如 \"A1\"") String targetCell,
        @ToolParam(description = "行字段列名（标题行中的列名）") String rowField,
        @ToolParam(description = "列字段列名（标题行中的列名，可选）", required = false) String columnField,
        @ToolParam(description = "数据字段列名（标题行中的列名）") String dataField,
        @ToolParam(description = "汇总方式：SUM/COUNT/AVERAGE/MAX/MIN，默认SUM", required = false) String operation) {
        return executeWithAudit("excel_pivot_table", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", rowField=" + rowField, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            String op = operation != null ? operation.toUpperCase() : "SUM";
            try (Workbook workbook = openWorkbook(path)) {
                if (!(workbook instanceof XSSFWorkbook)) {
                    return "错误: 创建透视表功能仅支持 .xlsx 格式文件";
                }
                org.apache.poi.xssf.usermodel.XSSFSheet sourceSheet = ((XSSFWorkbook) workbook).getSheet(sheetName);
                if (sourceSheet == null) {
                    return "错误: 源工作表不存在 '" + sheetName + "'";
                }
                int[] bounds = parseRange(sourceRange);
                Row headerRow = sourceSheet.getRow(bounds[0]);
                if (headerRow == null) {
                    return "错误: 源范围第一行（标题行）为空";
                }
                Map<String, Integer> headerMap = new LinkedHashMap<>();
                for (int c = bounds[1]; c <= bounds[3]; c++) {
                    Cell cell = headerRow.getCell(c);
                    String colName = getCellStringValue(cell, false).trim();
                    if (!colName.isEmpty()) {
                        headerMap.put(colName, c);
                    }
                }
                Integer rowFieldIdx = headerMap.get(rowField);
                if (rowFieldIdx == null) {
                    return "错误: 找不到行字段 '" + rowField + "'，可用列: " + headerMap.keySet();
                }
                Integer dataFieldIdx = headerMap.get(dataField);
                if (dataFieldIdx == null) {
                    return "错误: 找不到数据字段 '" + dataField + "'，可用列: " + headerMap.keySet();
                }
                Integer columnFieldIdx = null;
                if (columnField != null && !columnField.isBlank()) {
                    columnFieldIdx = headerMap.get(columnField);
                    if (columnFieldIdx == null) {
                        return "错误: 找不到列字段 '" + columnField + "'，可用列: " + headerMap.keySet();
                    }
                }
                org.apache.poi.xssf.usermodel.XSSFSheet targetSheet = ((XSSFWorkbook) workbook).getSheet(targetSheetName);
                if (targetSheet == null) {
                    targetSheet = ((XSSFWorkbook) workbook).createSheet(targetSheetName);
                }
                AreaReference areaRef = new AreaReference(sourceRange, SpreadsheetVersion.EXCEL2007);
                CellReference cellRef = new CellReference(targetCell);
                XSSFPivotTable pivotTable = targetSheet.createPivotTable(areaRef, cellRef, sourceSheet);
                pivotTable.addRowLabel(rowFieldIdx);
                if (columnFieldIdx != null) {
                    pivotTable.addColLabel(columnFieldIdx);
                }
                DataConsolidateFunction func = switch (op) {
                    case "COUNT" -> DataConsolidateFunction.COUNT;
                    case "AVERAGE" -> DataConsolidateFunction.AVERAGE;
                    case "MAX" -> DataConsolidateFunction.MAX;
                    case "MIN" -> DataConsolidateFunction.MIN;
                    default -> DataConsolidateFunction.SUM;
                };
                pivotTable.addColumnLabel(func, dataFieldIdx, op + "项:" + dataField);
                saveWorkbook(workbook, path);
                LogUtil.info("excel_pivot_table 成功: 行字段={}, 数据字段={}, 操作={}, 文件={}", rowField, dataField, op, fileAbsolutePath);
                return "成功创建透视表: 行字段=" + rowField + ", 数据字段=" + dataField + ", 汇总方式=" + op + ", 目标工作表=" + targetSheetName
                    + ", 文件=" + fileAbsolutePath;
            }
        });
    }

    // ==================== 10. excel_protect_sheet ====================

    /**
     * 保护 Excel 工作表或工作簿结构。
     */
    @Tool(name = "excel_analyze_protect_sheet", description = "保护 Excel 工作表或工作簿结构，设置密码后需要密码才能取消保护。")
    public String excelProtectSheet(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "工作表名称") String sheetName, @ToolParam(description = "保护密码") String password,
        @ToolParam(description = "true=保护工作表，false=保护工作簿结构，默认true", required = false) Boolean protectSheet) {
        return executeWithAudit("excel_protect_sheet", "file=" + fileAbsolutePath + ", sheet=" + sheetName, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            boolean protect = protectSheet == null || protectSheet;
            try (Workbook workbook = openWorkbook(path)) {
                if (protect) {
                    Sheet sheet = getSheet(workbook, sheetName);
                    sheet.protectSheet(password);
                    saveWorkbook(workbook, path);
                    LogUtil.info("excel_protect_sheet 成功: 保护工作表={}, 文件={}", sheetName, fileAbsolutePath);
                    return "已保护工作表 '" + sheetName + "': " + fileAbsolutePath;
                } else {
                    if (workbook instanceof XSSFWorkbook xssf) {
                        xssf.lockStructure();
                    } else if (workbook instanceof HSSFWorkbook) {
                        return "错误: .xls 格式不支持工作簿结构保护，请使用 .xlsx 格式";
                    }
                    saveWorkbook(workbook, path);
                    LogUtil.info("excel_protect_sheet 成功: 保护工作簿结构, 文件={}", fileAbsolutePath);
                    return "已保护工作簿结构: " + fileAbsolutePath;
                }
            }
        });
    }

    // ==================== 11. excel_subtotal ====================

    /**
     * 按指定列分组并对另一列进行汇总计算。
     */
    @Tool(name = "excel_analyze_subtotal", description = "按指定列分组并对另一列进行汇总计算（SUM/COUNT/AVERAGE），在工作表中插入汇总行。")
    public String excelSubtotal(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "工作表名称") String sheetName, @ToolParam(description = "分组列索引（0-based）") int groupByColumnIndex,
        @ToolParam(description = "汇总列索引（0-based）") int subtotalColumnIndex,
        @ToolParam(description = "汇总方式：SUM/COUNT/AVERAGE，默认SUM", required = false) String operation) {
        return executeWithAudit("excel_subtotal", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", groupBy=" + groupByColumnIndex, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            String op = operation != null ? operation.toUpperCase() : "SUM";
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                List<RowData> allRows = new ArrayList<>();
                int maxCol = 0;
                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    if (row.getLastCellNum() > maxCol) {
                        maxCol = row.getLastCellNum();
                    }
                    List<Object> rowValues = new ArrayList<>();
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        rowValues.add(getCellJsonValue(row.getCell(c), false));
                    }
                    allRows.add(new RowData(rowValues));
                }
                allRows.sort(Comparator.comparing(r -> {
                    Object val = r.values()
                                  .size() > groupByColumnIndex ? r.values()
                                                                  .get(groupByColumnIndex) : null;
                    return val != null ? val.toString() : "";
                }));
                Row headerRow = sheet.getRow(0);
                List<String> headers = new ArrayList<>();
                if (headerRow != null) {
                    for (int c = 0; c < Math.max(maxCol, headerRow.getLastCellNum()); c++) {
                        headers.add(getCellStringValue(headerRow.getCell(c), false));
                    }
                }
                for (int r = sheet.getLastRowNum(); r >= 0; r--) {
                    Row row = sheet.getRow(r);
                    if (row != null) {
                        sheet.removeRow(row);
                    }
                }
                Row newHeaderRow = sheet.createRow(0);
                for (int c = 0; c < headers.size(); c++) {
                    newHeaderRow.createCell(c)
                                .setCellValue(headers.get(c));
                }
                int writeRowIdx = 1;
                int groupCount = 0;
                int i = 0;
                while (i < allRows.size()) {
                    String currentGroup = allRows.get(i)
                                                 .values()
                                                 .size() > groupByColumnIndex ? String.valueOf(allRows.get(i)
                                                                                                      .values()
                                                                                                      .get(groupByColumnIndex)) : "";
                    double sum = 0;
                    int count = 0;
                    int j = i;
                    while (j < allRows.size()) {
                        String nextGroup = allRows.get(j)
                                                  .values()
                                                  .size() > groupByColumnIndex ? String.valueOf(allRows.get(j)
                                                                                                       .values()
                                                                                                       .get(groupByColumnIndex)) : "";
                        if (!nextGroup.equals(currentGroup)) {
                            break;
                        }
                        Row dataRow = sheet.createRow(writeRowIdx++);
                        List<Object> vals = allRows.get(j)
                                                   .values();
                        for (int c = 0; c < vals.size(); c++) {
                            Cell cell = dataRow.createCell(c);
                            writeCellValue(cell, vals.get(c));
                        }
                        Object valObj = vals.size() > subtotalColumnIndex ? vals.get(subtotalColumnIndex) : null;
                        if (valObj instanceof Number num) {
                            double v = num.doubleValue();
                            sum += v;
                            count++;
                        }
                        j++;
                    }
                    Row subtotalRow = sheet.createRow(writeRowIdx++);
                    for (int c = 0; c < Math.max(maxCol, headers.size()); c++) {
                        Cell cell = subtotalRow.createCell(c);
                        if (c == groupByColumnIndex) {
                            cell.setCellValue(currentGroup + " 汇总");
                        } else if (c == subtotalColumnIndex) {
                            double resultVal = switch (op) {
                                case "COUNT" -> count;
                                case "AVERAGE" -> count > 0 ? sum / count : 0;
                                default -> sum;
                            };
                            resultVal = Math.round(resultVal * 100.0) / 100.0;
                            cell.setCellValue(resultVal);
                        }
                    }
                    groupCount++;
                    i = j;
                }
                saveWorkbook(workbook, path);
                LogUtil.info("excel_subtotal 成功: {} 个分组, 操作={}, 文件={}", groupCount, op, fileAbsolutePath);
                return "成功分类汇总: " + groupCount + " 个分组, 汇总方式=" + op + ", 分组列=" + (groupByColumnIndex + 1) + ", 汇总列=" + (
                    subtotalColumnIndex + 1) + ", 文件=" + fileAbsolutePath;
            }
        });
    }

    // ==================== 12. excel_group_rows ====================

    /**
     * 对 Excel 工作表中的行进行分组折叠。
     */
    @Tool(name = "excel_analyze_group_rows", description = "对 Excel 工作表中的行进行分组折叠，方便数据展示和阅读。")
    public String excelGroupRows(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "工作表名称") String sheetName, @ToolParam(description = "起始行索引（0-based）") int startRow,
        @ToolParam(description = "结束行索引（0-based）") int endRow) {
        return executeWithAudit("excel_group_rows",
                                "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", startRow=" + startRow + ", endRow=" + endRow, () -> {
                Path path = validateExcelFile(fileAbsolutePath);
                try (Workbook workbook = openWorkbook(path)) {
                    Sheet sheet = getSheet(workbook, sheetName);
                    if (startRow < 0 || endRow < startRow) {
                        return "错误: 行索引无效，startRow=" + startRow + ", endRow=" + endRow;
                    }
                    sheet.groupRow(startRow, endRow);
                    sheet.setRowGroupCollapsed(startRow, true);
                    saveWorkbook(workbook, path);
                    LogUtil.info("excel_group_rows 成功: 行范围={}-{}, 文件={}", startRow, endRow, fileAbsolutePath);
                    return "成功分组折叠行: 第 " + (startRow + 1) + " 行到第 " + (endRow + 1) + " 行, 工作表=" + sheetName + ", 文件="
                        + fileAbsolutePath;
                }
            });
    }
}