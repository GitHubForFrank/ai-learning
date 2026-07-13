package com.example.mcp.handler.excel;

import com.example.mcp.pojo.excel.CellStyleInfo;
import com.example.mcp.util.LogUtil;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Excel 格式化操作 Handler，提供单元格样式格式化、表格创建、列宽/行高调整、冻结窗格、合并单元格、条件格式、图表创建等功能。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class ExcelFormatHandler extends ExcelBaseHandler {

    // ==================== 1. excel_format_range ====================

    /**
     * 对 Excel 工作表中的单元格范围应用样式。
     */
    @Tool(name = "excel_format_range", description = "使用样式信息格式化 Excel 工作表中的单元格")
    public String excelFormatRange(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName,
        @ToolParam(description = "Excel 工作表中的单元格范围（如 \"A1:C3\"）") String range,
        @ToolParam(description = "每个单元格的样式对象二维数组。不需要修改的用 null 表示。数组数量必须与范围大小匹配。") List<List<CellStyleInfo>> styles) {
        return executeWithAudit("excel_format_range", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", range=" + range, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            int[] bounds = parseRange(range);
            int firstRow = bounds[0];
            int firstCol = bounds[1];
            int lastRow = bounds[2];
            int lastCol = bounds[3];

            int expectedRows = lastRow - firstRow + 1;
            int expectedCols = lastCol - firstCol + 1;

            if (styles.size() != expectedRows) {
                return "错误: styles 的行数(" + styles.size() + ")与范围行数(" + expectedRows + ")不匹配";
            }

            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);

                for (int r = 0; r < styles.size(); r++) {
                    List<CellStyleInfo> rowStyles = styles.get(r);
                    if (rowStyles.size() != expectedCols) {
                        return "错误: styles 第" + (r + 1) + "行的列数(" + rowStyles.size() + ")与范围列数(" + expectedCols + ")不匹配";
                    }
                    int targetRow = firstRow + r;
                    Row row = sheet.getRow(targetRow);
                    if (row == null) {
                        row = sheet.createRow(targetRow);
                    }
                    for (int c = 0; c < rowStyles.size(); c++) {
                        CellStyleInfo styleInfo = rowStyles.get(c);
                        if (styleInfo == null) {
                            continue;
                        }
                        int targetCol = firstCol + c;
                        org.apache.poi.ss.usermodel.Cell cell = row.getCell(targetCol);
                        if (cell == null) {
                            cell = row.createCell(targetCol);
                        }
                        applyCellStyle(workbook, cell, styleInfo);
                    }
                }

                saveWorkbook(workbook, path);
                return "成功格式化: 文件=" + fileAbsolutePath + ", 工作表=" + sheetName + ", 范围=" + range;
            }
        });
    }

    // ==================== 2. excel_create_table ====================

    /**
     * 在 Excel 工作表中创建表格。
     */
    @Tool(name = "excel_format_create_table", description = "在 Excel 工作表中创建表格")
    public String excelCreateTable(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "创建表格的工作表名称") String sheetName, @ToolParam(description = "要创建的表格名称") String tableName,
        @ToolParam(description = "表格范围（如 \"A1:C10\"）", required = false) String range) {
        return executeWithAudit("excel_create_table", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", table=" + tableName, () -> {
            Path path = validateExcelFile(fileAbsolutePath);

            try (Workbook workbook = openWorkbook(path)) {
                if (!(workbook instanceof XSSFWorkbook xssfWorkbook)) {
                    return "错误: 创建表格功能仅支持 .xlsx 格式文件";
                }

                XSSFWorkbook xssf = xssfWorkbook;
                org.apache.poi.xssf.usermodel.XSSFSheet xssfSheet = xssf.getSheet(sheetName);
                if (xssfSheet == null) {
                    return "错误: 工作表不存在 '" + sheetName + "'";
                }

                AreaReference areaRef;
                if (range != null && !range.isBlank()) {
                    areaRef = new AreaReference(range, workbook.getSpreadsheetVersion());
                } else {
                    int lastRow = xssfSheet.getLastRowNum();
                    int lastCol = 0;
                    for (int r = 0; r <= lastRow; r++) {
                        Row row = xssfSheet.getRow(r);
                        if (row != null && row.getLastCellNum() > lastCol) {
                            lastCol = row.getLastCellNum();
                        }
                    }
                    if (lastRow < 0 || lastCol == 0) {
                        return "错误: 工作表为空，无法自动确定表格范围，请指定 range 参数";
                    }
                    areaRef = new AreaReference(new CellReference(0, 0), new CellReference(lastRow, lastCol - 1), workbook.getSpreadsheetVersion());
                }

                XSSFTable table = xssfSheet.createTable(areaRef);
                table.setName(tableName);
                table.getCTTable()
                     .addNewTableStyleInfo();
                table.getCTTable()
                     .getTableStyleInfo()
                     .setName("TableStyleMedium2");
                table.getCTTable()
                     .getTableStyleInfo()
                     .setShowColumnStripes(false);
                table.getCTTable()
                     .getTableStyleInfo()
                     .setShowRowStripes(true);

                saveWorkbook(workbook, path);
                return "成功创建表格: 名称='" + tableName + "', 工作表=" + sheetName + ", 范围=" + areaRef.formatAsString() + ", 文件="
                    + fileAbsolutePath;
            }
        });
    }

    // ==================== 3. excel_auto_fit_columns ====================

    /**
     * 自动调整列宽以适配内容。
     */
    @Tool(name = "excel_format_auto_fit_columns", description = "自动调整 Excel 工作表中指定列的宽度以适配内容。")
    public String excelAutoFitColumns(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName,
        @ToolParam(description = "要自动调整宽度的列索引（从0开始），逗号分隔，如 \"0,1,2\"。留空则调整所有列。", required = false) String columnIndices) {
        return executeWithAudit("excel_auto_fit_columns", "file=" + fileAbsolutePath + ", sheet=" + sheetName, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                if (columnIndices == null || columnIndices.isBlank()) {
                    int maxCol = 0;
                    for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
                        Row row = sheet.getRow(r);
                        if (row != null && row.getLastCellNum() > maxCol) {
                            maxCol = row.getLastCellNum();
                        }
                    }
                    for (int i = 0; i < maxCol; i++) {
                        sheet.autoSizeColumn(i);
                    }
                    return "已自动调整所有列宽: " + fileAbsolutePath + ", 工作表=" + sheetName;
                } else {
                    for (String idx : columnIndices.split(",")) {
                        int colIdx = Integer.parseInt(idx.trim());
                        sheet.autoSizeColumn(colIdx);
                    }
                    return "已自动调整列宽: " + fileAbsolutePath + ", 工作表=" + sheetName + ", 列=" + columnIndices;
                }
            }
        });
    }

    // ==================== 4. excel_set_column_width ====================

    /**
     * 设置指定列的宽度。
     */
    @Tool(name = "excel_format_set_column_width", description = "设置 Excel 工作表中指定列的宽度（字符数）。")
    public String excelSetColumnWidth(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName, @ToolParam(description = "列索引（从0开始）") int columnIndex,
        @ToolParam(description = "列宽（字符数）") int width) {
        return executeWithAudit("excel_set_column_width",
                                "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", col=" + columnIndex + ", width=" + width, () -> {
                Path path = validateExcelFile(fileAbsolutePath);
                try (Workbook workbook = openWorkbook(path)) {
                    Sheet sheet = getSheet(workbook, sheetName);
                    if (columnIndex < 0) {
                        return "错误: 列索引不能为负数";
                    }
                    sheet.setColumnWidth(columnIndex, width * 256);
                    saveWorkbook(workbook, path);
                    return "已设置第 " + (columnIndex + 1) + " 列宽度为 " + width + ": " + fileAbsolutePath + ", 工作表=" + sheetName;
                }
            });
    }

    // ==================== 5. excel_set_row_height ====================

    /**
     * 设置指定行的高度。
     */
    @Tool(name = "excel_format_set_row_height", description = "设置 Excel 工作表中指定行的高度（磅值）。")
    public String excelSetRowHeight(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName, @ToolParam(description = "行索引（从0开始）") int rowIndex,
        @ToolParam(description = "行高（磅）") float height) {
        return executeWithAudit("excel_set_row_height",
                                "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", row=" + rowIndex + ", height=" + height, () -> {
                Path path = validateExcelFile(fileAbsolutePath);
                try (Workbook workbook = openWorkbook(path)) {
                    Sheet sheet = getSheet(workbook, sheetName);
                    if (rowIndex < 0) {
                        return "错误: 行索引不能为负数";
                    }
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        row = sheet.createRow(rowIndex);
                    }
                    row.setHeightInPoints(height);
                    saveWorkbook(workbook, path);
                    return "已设置第 " + (rowIndex + 1) + " 行高度为 " + height + "pt: " + fileAbsolutePath + ", 工作表=" + sheetName;
                }
            });
    }

    // ==================== 6. excel_freeze_panes ====================

    /**
     * 冻结窗格。
     */
    @Tool(name = "excel_format_freeze_panes", description = "冻结 Excel 工作表的窗格，滚动时保持指定行列可见。")
    public String excelFreezePanes(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName, @ToolParam(description = "冻结的列数（从0开始的分割索引）") int colSplit,
        @ToolParam(description = "冻结的行数（从0开始的分割索引）") int rowSplit) {
        return executeWithAudit("excel_freeze_panes",
                                "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", colSplit=" + colSplit + ", rowSplit=" + rowSplit, () -> {
                Path path = validateExcelFile(fileAbsolutePath);
                try (Workbook workbook = openWorkbook(path)) {
                    Sheet sheet = getSheet(workbook, sheetName);
                    sheet.createFreezePane(colSplit, rowSplit);
                    saveWorkbook(workbook, path);
                    return "已冻结窗格: 列=" + colSplit + ", 行=" + rowSplit + ", 文件=" + fileAbsolutePath + ", 工作表=" + sheetName;
                }
            });
    }

    // ==================== 7. excel_merge_cells ====================

    /**
     * 合并或取消合并单元格。
     */
    @Tool(name = "excel_format_merge_cells", description = "合并或取消合并 Excel 工作表中的单元格区域。")
    public String excelMergeCells(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName, @ToolParam(description = "要合并的范围，如 \"A1:C1\"") String range,
        @ToolParam(description = "true 表示合并，false 表示取消合并") boolean merge) {
        return executeWithAudit("excel_merge_cells", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", range=" + range, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                int[] bounds = parseRange(range);
                CellRangeAddress cra = new CellRangeAddress(bounds[0], bounds[2], bounds[1], bounds[3]);
                if (merge) {
                    sheet.addMergedRegion(cra);
                    return "已合并单元格: " + range + ", 文件=" + fileAbsolutePath + ", 工作表=" + sheetName;
                } else {
                    int numMerged = sheet.getNumMergedRegions();
                    boolean removed = false;
                    for (int i = numMerged - 1; i >= 0; i--) {
                        CellRangeAddress existing = sheet.getMergedRegion(i);
                        if (existing.formatAsString()
                                    .equals(range)) {
                            sheet.removeMergedRegion(i);
                            removed = true;
                        }
                    }
                    return removed ? "已取消合并单元格: " + range + ", 文件=" + fileAbsolutePath + ", 工作表=" + sheetName
                        : "未找到合并区域: " + range;
                }
            }
        });
    }

    // ==================== 8. excel_conditional_format ====================

    /**
     * 添加条件格式。
     */
    @Tool(name = "excel_format_conditional", description = "为 Excel 工作表单元格添加条件格式，根据数值大小高亮显示。")
    public String excelConditionalFormat(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "Excel 文件中的工作表名称") String sheetName,
        @ToolParam(description = "要应用条件格式的范围，如 \"A2:A10\"") String range,
        @ToolParam(description = "类型：cellValue（按值高亮单元格）、dataBar（数据条）") String type,
        @ToolParam(description = "比较运算符：>、>=、<、<=、=、!=（仅 cellValue 类型）") String operator,
        @ToolParam(description = "比较的阈值（仅 cellValue 类型）") String value,
        @ToolParam(description = "填充颜色，可选值：RED、YELLOW、GREEN、BLUE、ORANGE、PINK、GREY_25_PERCENT、LIGHT_BLUE 等") String fillColor) {
        return executeWithAudit("excel_conditional_format",
                                "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", range=" + range + ", type=" + type, () -> {
                Path path = validateExcelFile(fileAbsolutePath);
                try (Workbook workbook = openWorkbook(path)) {
                    Sheet sheet = getSheet(workbook, sheetName);
                    SheetConditionalFormatting scf = sheet.getSheetConditionalFormatting();
                    ConditionalFormattingRule rule;
                    if ("cellValue".equals(type)) {
                        byte op = switch (operator) {
                            case ">" -> ComparisonOperator.GT;
                            case ">=" -> ComparisonOperator.GE;
                            case "<" -> ComparisonOperator.LT;
                            case "<=" -> ComparisonOperator.LE;
                            case "=" -> ComparisonOperator.EQUAL;
                            case "!=" -> ComparisonOperator.NOT_EQUAL;
                            default -> throw new IllegalArgumentException("不支持的操作符: " + operator);
                        };
                        rule = scf.createConditionalFormattingRule(op, value, null);
                    } else {
                        return "错误: 不支持的条件格式类型 '" + type + "', 支持: cellValue";
                    }
                    PatternFormatting fill = rule.createPatternFormatting();
                    try {
                        IndexedColors color = IndexedColors.valueOf(fillColor.toUpperCase());
                        fill.setFillBackgroundColor(color.index);
                    } catch (IllegalArgumentException e) {
                        fill.setFillBackgroundColor(IndexedColors.RED.index);
                    }
                    fill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
                    int[] bounds = parseRange(range);
                    CellRangeAddress[] regions = {new CellRangeAddress(bounds[0], bounds[2], bounds[1], bounds[3])};
                    scf.addConditionalFormatting(regions, rule);
                    saveWorkbook(workbook, path);
                    return "已添加条件格式: 类型=" + type + ", 范围=" + range + ", 文件=" + fileAbsolutePath + ", 工作表=" + sheetName;
                }
            });
    }

    // ==================== 9. excel_chart_create ====================

    /**
     * 在 Excel 工作表中创建图表。
     */
    @Tool(name = "excel_format_chart_create", description = "在 Excel 工作表中创建图表，支持柱状图(bar)、折线图(line)、饼图(pie)、面积图(area)、散点图(scatter)。")
    public String excelChartCreate(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "工作表名称") String sheetName, @ToolParam(description = "图表类型：bar/line/pie/area/scatter") String chartType,
        @ToolParam(description = "数据范围，如 \"A1:B10\"") String dataRange,
        @ToolParam(description = "分类轴范围，如 \"A2:A10\"") String categoryRange,
        @ToolParam(description = "数值轴范围，如 \"B2:B10\"") String valueRange, @ToolParam(description = "图表标题") String chartTitle,
        @ToolParam(description = "图表左上角行位置，默认0", required = false) Integer positionRow1,
        @ToolParam(description = "图表左上角列位置，默认0", required = false) Integer positionCol1,
        @ToolParam(description = "图表右下角行位置，默认15", required = false) Integer positionRow2,
        @ToolParam(description = "图表右下角列位置，默认8", required = false) Integer positionCol2) {
        return executeWithAudit("excel_chart_create", "file=" + fileAbsolutePath + ", sheet=" + sheetName + ", chartType=" + chartType, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            int pr1 = positionRow1 != null ? positionRow1 : 0;
            int pc1 = positionCol1 != null ? positionCol1 : 0;
            int pr2 = positionRow2 != null ? positionRow2 : 15;
            int pc2 = positionCol2 != null ? positionCol2 : 8;
            try (Workbook workbook = openWorkbook(path)) {
                if (!(workbook instanceof XSSFWorkbook)) {
                    return "错误: 创建图表功能仅支持 .xlsx 格式文件";
                }
                Sheet sheet = getSheet(workbook, sheetName);
                Drawing<?> drawing = sheet.createDrawingPatriarch();
                XSSFClientAnchor anchor = (XSSFClientAnchor) drawing.createAnchor(0, 0, 0, 0, pc1, pr1, pc2, pr2);
                XSSFChart chart = ((XSSFDrawing) drawing).createChart(anchor);
                chart.setTitleText(chartTitle);
                chart.setTitleOverlay(false);
                XDDFChartLegend legend = chart.getOrAddLegend();
                legend.setPosition(LegendPosition.BOTTOM);

                XDDFDataSource<String> cats = XDDFDataSourcesFactory.fromStringCellRange((org.apache.poi.xssf.usermodel.XSSFSheet) sheet,
                                                                                         CellRangeAddress.valueOf(categoryRange));
                XDDFNumericalDataSource<Double> vals = XDDFDataSourcesFactory.fromNumericCellRange((org.apache.poi.xssf.usermodel.XSSFSheet) sheet,
                                                                                                   CellRangeAddress.valueOf(valueRange));

                String ct = chartType.toLowerCase();
                XDDFChartData data;
                switch (ct) {
                    case "bar" -> {
                        data = chart.createData(ChartTypes.BAR, null, null);
                        data.addSeries(cats, vals);
                        chart.plot(data);
                    }
                    case "line" -> {
                        data = chart.createData(ChartTypes.LINE, null, null);
                        data.addSeries(cats, vals);
                        chart.plot(data);
                    }
                    case "pie" -> {
                        data = chart.createData(ChartTypes.PIE, null, null);
                        data.addSeries(cats, vals);
                        chart.plot(data);
                    }
                    case "area" -> {
                        data = chart.createData(ChartTypes.AREA, null, null);
                        data.addSeries(cats, vals);
                        chart.plot(data);
                    }
                    case "scatter" -> {
                        data = chart.createData(ChartTypes.SCATTER, null, null);
                        data.addSeries(null, vals);
                        chart.plot(data);
                    }
                    default -> {
                        return "错误: 不支持的图表类型 '" + chartType + "'，支持: bar/line/pie/area/scatter";
                    }
                }
                saveWorkbook(workbook, path);
                LogUtil.info("excel_chart_create 成功: 类型={}, 标题={}, 文件={}", chartType, chartTitle, fileAbsolutePath);
                return "成功创建图表: 类型=" + chartType + ", 标题=" + chartTitle + ", 工作表=" + sheetName + ", 文件=" + fileAbsolutePath;
            }
        });
    }

    // ==================== 10. excel_named_range (新增) ====================

    /**
     * 定义/管理命名区域。
     */
    @Tool(name = "excel_format_named_range", description = "定义、删除或列出命名区域。支持 create/delete/list 三种操作。")
    public String excelNamedRange(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "工作表名称（list 操作时为可选）", required = false) String sheetName,
        @ToolParam(description = "命名区域名称") String rangeName,
        @ToolParam(description = "区域地址，如 \"A1:C10\"（create 时需要）", required = false) String rangeAddress,
        @ToolParam(description = "操作类型：create/delete/list") String action) {
        return executeWithAudit("excel_named_range", "file=" + fileAbsolutePath + ", action=" + action + ", name=" + rangeName, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                String act = action != null ? action.toLowerCase() : "list";

                switch (act) {
                    case "create" -> {
                        if (rangeAddress == null || rangeAddress.isBlank()) {
                            return "错误: create 操作需要指定 rangeAddress 参数";
                        }
                        if (sheetName == null || sheetName.isBlank()) {
                            return "错误: create 操作需要指定 sheetName 参数";
                        }
                        Name name = workbook.createName();
                        name.setNameName(rangeName);
                        name.setRefersToFormula(sheetName + "!" + rangeAddress);
                        saveWorkbook(workbook, path);
                        return "已创建命名区域 '" + rangeName + "': " + sheetName + "!" + rangeAddress + ", 文件=" + fileAbsolutePath;
                    }
                    case "delete" -> {
                        Name name = workbook.getName(rangeName);
                        if (name == null) {
                            return "错误: 命名区域 '" + rangeName + "' 不存在";
                        }
                        workbook.removeName(name);
                        saveWorkbook(workbook, path);
                        return "已删除命名区域 '" + rangeName + "': " + fileAbsolutePath;
                    }
                    case "list" -> {
                        List<Map<String, String>> names = new ArrayList<>();
                        for (Name name : workbook.getAllNames()) {
                            Map<String, String> info = new LinkedHashMap<>();
                            info.put("name", name.getNameName());
                            info.put("refersTo", name.getRefersToFormula());
                            info.put("sheetName", name.getSheetName());
                            names.add(info);
                        }
                        return com.alibaba.fastjson2.JSON.toJSONString(names);
                    }
                    default -> {
                        return "错误: 不支持的操作类型 '" + action + "'，支持: create/delete/list";
                    }
                }
            }
        });
    }

    // ==================== 11. excel_print_setup (新增) ====================

    /**
     * 打印设置。
     */
    @Tool(name = "excel_format_print_setup", description = "设置 Excel 工作表的打印参数，包括纸张方向、纸张大小、页边距、缩放比例和打印区域。")
    public String excelPrintSetup(@ToolParam(description = "Excel 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "工作表名称") String sheetName,
        @ToolParam(description = "纸张方向：portrait（纵向）/landscape（横向）", required = false) String orientation,
        @ToolParam(description = "纸张大小：A4/A3/LETTER", required = false) String paperSize,
        @ToolParam(description = "上边距（英寸），默认0.5", required = false) Double marginTop,
        @ToolParam(description = "下边距（英寸），默认0.5", required = false) Double marginBottom,
        @ToolParam(description = "左边距（英寸），默认0.7", required = false) Double marginLeft,
        @ToolParam(description = "右边距（英寸），默认0.7", required = false) Double marginRight,
        @ToolParam(description = "适应页面宽度（页数），默认1", required = false) Integer fitToWidth,
        @ToolParam(description = "适应页面高度（页数），默认0", required = false) Integer fitToHeight,
        @ToolParam(description = "打印区域，如 \"A1:F50\"", required = false) String printArea) {
        return executeWithAudit("excel_print_setup", "file=" + fileAbsolutePath + ", sheet=" + sheetName, () -> {
            Path path = validateExcelFile(fileAbsolutePath);
            double mt = marginTop != null ? marginTop : 0.5;
            double mb = marginBottom != null ? marginBottom : 0.5;
            double ml = marginLeft != null ? marginLeft : 0.7;
            double mr = marginRight != null ? marginRight : 0.7;
            int ftw = fitToWidth != null ? fitToWidth : 1;
            int fth = fitToHeight != null ? fitToHeight : 0;

            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                PrintSetup ps = sheet.getPrintSetup();

                if (orientation != null) {
                    ps.setLandscape("landscape".equalsIgnoreCase(orientation));
                }
                if (paperSize != null) {
                    switch (paperSize.toUpperCase()) {
                        case "A4" -> ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
                        case "A3" -> ps.setPaperSize(PrintSetup.A3_PAPERSIZE);
                        case "LETTER" -> ps.setPaperSize(PrintSetup.LETTER_PAPERSIZE);
                        default -> ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
                    }
                }
                ps.setFitWidth((short) ftw);
                ps.setFitHeight((short) fth);

                sheet.setMargin(Sheet.TopMargin, mt / 2.54);    // 英寸转厘米
                sheet.setMargin(Sheet.BottomMargin, mb / 2.54);
                sheet.setMargin(Sheet.LeftMargin, ml / 2.54);
                sheet.setMargin(Sheet.RightMargin, mr / 2.54);

                if (printArea != null && !printArea.isBlank()) {
                    workbook.setPrintArea(workbook.getSheetIndex(sheet), printArea);
                } else {
                    // 如果没有指定打印区域，清除之前的打印区域
                    workbook.setPrintArea(workbook.getSheetIndex(sheet), -1, -1, -1, -1);
                }

                saveWorkbook(workbook, path);
                return "已设置打印参数: 工作表=" + sheetName + ", 方向=" + (orientation != null ? orientation : "默认") + ", 纸张=" + (
                    paperSize != null ? paperSize : "默认") + ", 文件=" + fileAbsolutePath;
            }
        });
    }
}