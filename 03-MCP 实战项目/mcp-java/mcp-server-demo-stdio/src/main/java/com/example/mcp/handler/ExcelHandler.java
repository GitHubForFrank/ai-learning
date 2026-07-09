package com.example.mcp.handler;

import com.alibaba.fastjson2.JSON;
import com.example.mcp.pojo.excel.BorderInfo;
import com.example.mcp.pojo.excel.CellStyleInfo;
import com.example.mcp.pojo.excel.FillInfo;
import com.example.mcp.pojo.excel.FontInfo;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

/**
 * MCP Excel 工具实现，完整复刻 mcp_Excel 的所有功能：
 * excel_describe_sheets、excel_read_sheet、excel_write_to_sheet、
 * excel_copy_sheet、excel_create_table、excel_format_range、excel_screen_capture
 *
 * @author FrankKang
 * @since 2026-07-09
 */
@Service
public class ExcelHandler {

    private static final Logger log = LoggerFactory.getLogger(ExcelHandler.class);

    /** 默认分页行数 */
    private static final int DEFAULT_PAGE_SIZE = 100;

    /** 屏幕截图渲染参数 */
    private static final int SCREENSHOT_COL_WIDTH = 120;
    private static final int SCREENSHOT_ROW_HEIGHT = 24;
    private static final int SCREENSHOT_PADDING = 6;
    private static final int SCREENSHOT_MAX_COLS = 20;
    private static final int SCREENSHOT_MAX_ROWS = 100;

    // ==================== 内部辅助方法 ====================

    /**
     * 校验文件路径是否存在且为 Excel 文件
     */
    private Path validateExcelFile(String fileAbsolutePath) {
        Path path = Paths.get(fileAbsolutePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("文件不存在: " + fileAbsolutePath);
        }
        String name = path.getFileName().toString().toLowerCase();
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 .xlsx 和 .xls: " + fileAbsolutePath);
        }
        return path;
    }

    /**
     * 打开 Excel 工作簿（自动识别 .xls / .xlsx）
     */
    private Workbook openWorkbook(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath)) {
            return WorkbookFactory.create(is);
        }
    }

    /**
     * 保存工作簿到文件
     */
    private void saveWorkbook(Workbook workbook, Path filePath) throws IOException {
        try (OutputStream os = Files.newOutputStream(filePath)) {
            workbook.write(os);
        }
    }

    /**
     * 解析范围字符串如 "A1:C10"，返回起始行列和结束行列（0-based）
     */
    private int[] parseRange(String range) {
        CellRangeAddress cra = CellRangeAddress.valueOf(range);
        return new int[] { cra.getFirstRow(), cra.getFirstColumn(), cra.getLastRow(), cra.getLastColumn() };
    }

    /**
     * 获取工作表；如果不存在则抛出异常
     */
    private Sheet getSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("工作表不存在: " + sheetName);
        }
        return sheet;
    }

    /**
     * 获取单元格的字符串值（用于读取）
     */
    private String getCellStringValue(Cell cell, boolean showFormula) {
        if (cell == null) {
            return "";
        }
        if (showFormula) {
            switch (cell.getCellType()) {
                case FORMULA:
                    return cell.getCellFormula();
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue().toString();
                    }
                    double numVal = cell.getNumericCellValue();
                    if (numVal == Math.floor(numVal) && !Double.isInfinite(numVal)) {
                        return String.valueOf((long) numVal);
                    }
                    return String.valueOf(numVal);
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case BLANK:
                    return "";
                default:
                    return "";
            }
        }
        // 返回实际显示值
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }

    /**
     * 获取单元格值的 JSON 表示（保留类型信息）
     */
    private Object getCellJsonValue(Cell cell, boolean showFormula) {
        if (cell == null) {
            return null;
        }
        if (showFormula && cell.getCellType() == CellType.FORMULA) {
            return cell.getCellFormula();
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception ex) {
                        return cell.getCellFormula();
                    }
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * 获取单元格的样式描述（JSON 对象）
     */
    private Map<String, Object> getCellStyleMap(Cell cell) {
        Map<String, Object> styleMap = new LinkedHashMap<>();
        if (cell == null) {
            return styleMap;
        }
        CellStyle style = cell.getCellStyle();
        if (style == null) {
            return styleMap;
        }

        // 字体信息
        org.apache.poi.ss.usermodel.Font font = cell.getSheet().getWorkbook().getFontAt(style.getFontIndex());
        if (font != null) {
            Map<String, Object> fontMap = new LinkedHashMap<>();
            fontMap.put("bold", font.getBold());
            fontMap.put("italic", font.getItalic());
            fontMap.put("size", font.getFontHeightInPoints());
            fontMap.put("underline", font.getUnderline() != 0);
            fontMap.put("strike", font.getStrikeout());
            fontMap.put("color", font.getColor() > 0
                    ? String.format("#%06X", font.getColor() & 0xFFFFFF)
                    : "#000000");
            styleMap.put("font", fontMap);
        }

        // 填充信息
        styleMap.put("fillPattern", style.getFillPattern().name());
        if (style.getFillForegroundColor() > 0) {
            styleMap.put("fillColor",
                    String.format("#%06X", style.getFillForegroundColor() & 0xFFFFFF));
        }

        // 边框
        Map<String, Object> borderMap = new LinkedHashMap<>();
        borderMap.put("top", style.getBorderTop().name());
        borderMap.put("bottom", style.getBorderBottom().name());
        borderMap.put("left", style.getBorderLeft().name());
        borderMap.put("right", style.getBorderRight().name());
        styleMap.put("border", borderMap);

        // 对齐方式
        styleMap.put("alignment", style.getAlignment().name());
        styleMap.put("verticalAlignment", style.getVerticalAlignment().name());

        // 数字格式
        styleMap.put("dataFormat", style.getDataFormatString());

        return styleMap;
    }

    /**
     * 将十六进制颜色字符串转换为 AWT Color
     */
    private java.awt.Color hexToColor(String hex) {
        if (hex == null || !hex.matches("^#[0-9A-Fa-f]{6}$")) {
            return java.awt.Color.BLACK;
        }
        return new java.awt.Color(
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16));
    }

    /**
     * 向单元格写入值，支持字符串、数字、布尔值、null 和公式（以 = 开头）
     */
    private void writeCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof String str) {
            if (str.startsWith("=")) {
                cell.setCellFormula(str);
            } else {
                cell.setCellValue(str);
            }
        } else if (value instanceof Number num) {
            cell.setCellValue(num.doubleValue());
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * 应用单元格样式
     */
    private void applyCellStyle(Workbook workbook, Cell cell, CellStyleInfo styleInfo) {
        if (styleInfo == null) {
            return;
        }
        CellStyle style = workbook.createCellStyle();

        // 字体
        if (styleInfo.font() != null) {
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            FontInfo fi = styleInfo.font();
            if (fi.bold() != null)
                font.setBold(fi.bold());
            if (fi.italic() != null)
                font.setItalic(fi.italic());
            if (fi.size() != null)
                font.setFontHeightInPoints(fi.size().shortValue());
            if (fi.strike() != null)
                font.setStrikeout(fi.strike());
            if (fi.color() != null) {
                java.awt.Color c = hexToColor(fi.color());
                byte[] rgb = new byte[] { (byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue() };
                if (font instanceof org.apache.poi.xssf.usermodel.XSSFFont xssfFont) {
                    xssfFont.setColor(new org.apache.poi.xssf.usermodel.XSSFColor(rgb, null));
                }
            }
            if (fi.underline() != null) {
                switch (fi.underline()) {
                    case "single" -> font.setUnderline((byte) 1);
                    case "double" -> font.setUnderline((byte) 2);
                    case "singleAccounting" -> font.setUnderline((byte) 33);
                    case "doubleAccounting" -> font.setUnderline((byte) 34);
                    default -> font.setUnderline((byte) 0);
                }
            }
            if (fi.vertAlign() != null) {
                switch (fi.vertAlign()) {
                    case "superscript" -> font.setTypeOffset((short) 1);
                    case "subscript" -> font.setTypeOffset((short) 2);
                    default -> font.setTypeOffset((short) 0);
                }
            }
            style.setFont(font);
        }

        // 填充
        if (styleInfo.fill() != null) {
            FillInfo fill = styleInfo.fill();
            if ("solid".equals(fill.type()) && fill.color() != null && !fill.color().isEmpty()) {
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                java.awt.Color c = hexToColor(fill.color().get(0));
                if (workbook instanceof HSSFWorkbook) {
                    style.setFillForegroundColor(
                            ((HSSFWorkbook) workbook).getCustomPalette()
                                    .findSimilarColor(c.getRed(), c.getGreen(), c.getBlue()).getIndex());
                } else {
                    ((org.apache.poi.xssf.usermodel.XSSFCellStyle) style).setFillForegroundColor(
                            new org.apache.poi.xssf.usermodel.XSSFColor(c, null));
                }
            }
        }

        // 数字格式
        if (styleInfo.numFmt() != null) {
            style.setDataFormat(workbook.createDataFormat().getFormat(styleInfo.numFmt()));
        }

        // 边框
        if (styleInfo.border() != null) {
            for (BorderInfo bi : styleInfo.border()) {
                BorderStyle borderStyle = switch (bi.style() != null ? bi.style() : "continuous") {
                    case "none" -> BorderStyle.NONE;
                    case "continuous" -> BorderStyle.THIN;
                    case "dash" -> BorderStyle.DASHED;
                    case "dot" -> BorderStyle.DOTTED;
                    case "double" -> BorderStyle.DOUBLE;
                    case "dashDot" -> BorderStyle.DASH_DOT;
                    case "dashDotDot" -> BorderStyle.DASH_DOT_DOT;
                    case "mediumDashDot" -> BorderStyle.MEDIUM_DASH_DOT;
                    case "mediumDashDotDot" -> BorderStyle.MEDIUM_DASH_DOT_DOT;
                    default -> BorderStyle.THIN;
                };
                switch (bi.type() != null ? bi.type() : "left") {
                    case "left" -> style.setBorderLeft(borderStyle);
                    case "right" -> style.setBorderRight(borderStyle);
                    case "top" -> style.setBorderTop(borderStyle);
                    case "bottom" -> style.setBorderBottom(borderStyle);
                    // diagonalDown / diagonalUp 在 POI 中支持有限，跳过
                }
            }
        }

        cell.setCellStyle(style);
    }

    /**
     * 将工作表渲染为 BufferedImage
     */
    private BufferedImage renderSheetToImage(Sheet sheet, int firstRow, int firstCol, int lastRow, int lastCol) {
        int rowCount = lastRow - firstRow + 1;
        int colCount = lastCol - firstCol + 1;

        // 限制最大行列数
        colCount = Math.min(colCount, SCREENSHOT_MAX_COLS);
        rowCount = Math.min(rowCount, SCREENSHOT_MAX_ROWS);
        lastCol = firstCol + colCount - 1;
        lastRow = firstRow + rowCount - 1;

        int imgWidth = colCount * SCREENSHOT_COL_WIDTH + 1;
        int imgHeight = (rowCount + 1) * SCREENSHOT_ROW_HEIGHT + 1; // +1 行用于标题

        BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = image.createGraphics();

        // 抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 白色背景
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, imgWidth, imgHeight);

        // 绘制网格线
        g2d.setColor(new java.awt.Color(200, 200, 200));
        for (int i = 0; i <= colCount; i++) {
            int x = i * SCREENSHOT_COL_WIDTH;
            g2d.drawLine(x, 0, x, imgHeight);
        }
        for (int i = 0; i <= rowCount + 1; i++) {
            int y = i * SCREENSHOT_ROW_HEIGHT;
            g2d.drawLine(0, y, imgWidth, y);
        }

        // 列标题行（灰色背景）
        g2d.setColor(new java.awt.Color(220, 220, 220));
        g2d.fillRect(1, 1, imgWidth - 1, SCREENSHOT_ROW_HEIGHT - 1);
        g2d.setColor(java.awt.Color.BLACK);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 11));
        for (int col = firstCol; col <= lastCol; col++) {
            String colLetter = CellReference.convertNumToColString(col);
            int x = (col - firstCol) * SCREENSHOT_COL_WIDTH + SCREENSHOT_PADDING;
            int y = SCREENSHOT_ROW_HEIGHT - SCREENSHOT_PADDING;
            g2d.drawString(colLetter, x, y);
        }

        // 数据行
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        DataFormatter formatter = new DataFormatter();
        for (int row = firstRow; row <= lastRow; row++) {
            Row sheetRow = sheet.getRow(row);
            int drawRow = row - firstRow + 1; // +1 跳过标题行
            for (int col = firstCol; col <= lastCol; col++) {
                String text = "";
                if (sheetRow != null) {
                    Cell cell = sheetRow.getCell(col);
                    if (cell != null) {
                        text = formatter.formatCellValue(cell);
                    }
                }
                // 截断过长文本
                if (text.length() > 20) {
                    text = text.substring(0, 18) + "..";
                }
                int x = (col - firstCol) * SCREENSHOT_COL_WIDTH + SCREENSHOT_PADDING;
                int y = drawRow * SCREENSHOT_ROW_HEIGHT + SCREENSHOT_ROW_HEIGHT - SCREENSHOT_PADDING;
                g2d.setColor(java.awt.Color.BLACK);
                g2d.drawString(text, x, y);
            }
        }

        g2d.dispose();
        return image;
    }

    // ==================== MCP 工具方法 ====================

    // --- 1. excel_describe_sheets ---

    /**
     * 列出指定 Excel 文件的所有工作表信息
     */
    @Tool(description = "List all sheet information of specified Excel file")
    public String excelDescribeSheets(
            @ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            List<Map<String, Object>> sheets = new ArrayList<>();
            try (Workbook workbook = openWorkbook(path)) {
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("name", sheet.getSheetName());
                    info.put("index", i);
                    info.put("rowCount", sheet.getLastRowNum() + 1);
                    // 计算最大列数
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
        } catch (Exception e) {
            log.error("excel_describe_sheets 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 2. excel_read_sheet ---

    /**
     * 读取 Excel 工作表中的值，支持分页、显示公式和样式
     */
    @Tool(description = "Read values from Excel sheet with pagination.")
    public String excelReadSheet(
            @ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
            @ToolParam(description = "Sheet name in the Excel file") String sheetName,
            @ToolParam(description = "Range of cells to read in the Excel sheet (e.g., \"A1:C10\"). [default: first paging range]", required = false) String range,
            @ToolParam(description = "Show formula instead of value", required = false) Boolean showFormula,
            @ToolParam(description = "Show style information for cells", required = false) Boolean showStyle) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            boolean showF = showFormula != null && showFormula;
            boolean showS = showStyle != null && showStyle;

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
                    // 默认分页：第一页，最多 DEFAULT_PAGE_SIZE 行
                    firstRow = 0;
                    firstCol = 0;
                    lastRow = Math.min(sheet.getLastRowNum(), DEFAULT_PAGE_SIZE - 1);
                    // 计算最大列数
                    int maxCol = 0;
                    for (int r = firstRow; r <= lastRow; r++) {
                        Row row = sheet.getRow(r);
                        if (row != null && row.getLastCellNum() > maxCol) {
                            maxCol = row.getLastCellNum();
                        }
                    }
                    lastCol = Math.max(0, maxCol - 1);
                }

                // 构建返回数据
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("sheetName", sheetName);
                result.put("range", CellReference.convertNumToColString(firstCol) + (firstRow + 1) + ":"
                        + CellReference.convertNumToColString(lastCol) + (lastRow + 1));

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

                // 分页信息
                int totalRows = sheet.getLastRowNum() + 1;
                result.put("totalRows", totalRows);
                result.put("hasMore", lastRow + 1 < totalRows);

                return JSON.toJSONString(result);
            }
        } catch (Exception e) {
            log.error("excel_read_sheet 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 3. excel_write_to_sheet ---

    /**
     * 向 Excel 工作表写入值
     */
    @Tool(description = "Write values to the Excel sheet")
    public String excelWriteToSheet(
            @ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
            @ToolParam(description = "Sheet name in the Excel file") String sheetName,
            @ToolParam(description = "Create a new sheet if true, otherwise write to the existing sheet") Boolean newSheet,
            @ToolParam(description = "Range of cells in the Excel sheet (e.g., \"A1:C10\")") String range,
            @ToolParam(description = "Values to write to the Excel sheet. If the value is a formula, it should start with \"=\"") List<List<Object>> values) {
        try {
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

                // 写入值
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
        } catch (Exception e) {
            log.error("excel_write_to_sheet 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 4. excel_copy_sheet ---

    /**
     * 复制已有工作表到新工作表
     */
    @Tool(description = "Copy existing sheet to a new sheet")
    public String excelCopySheet(
            @ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
            @ToolParam(description = "Source sheet name in the Excel file") String srcSheetName,
            @ToolParam(description = "Sheet name to be copied") String dstSheetName) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);

            try (Workbook workbook = openWorkbook(path)) {
                Sheet srcSheet = getSheet(workbook, srcSheetName);

                if (workbook.getSheet(dstSheetName) != null) {
                    return "错误: 目标工作表已存在 '" + dstSheetName + "'";
                }

                // 使用 POI 的 cloneSheet 方法
                int srcIndex = workbook.getSheetIndex(srcSheet);
                workbook.cloneSheet(srcIndex);
                // cloneSheet 会自动命名为 "原名称 (2)"，需要手动设置名称
                // 获取最后一个 sheet（即克隆出的 sheet）
                int newIndex = workbook.getNumberOfSheets() - 1;
                workbook.setSheetName(newIndex, dstSheetName);

                saveWorkbook(workbook, path);
                return "成功复制: 从 '" + srcSheetName + "' 到 '" + dstSheetName + "', 文件=" + fileAbsolutePath;
            }
        } catch (Exception e) {
            log.error("excel_copy_sheet 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 5. excel_create_table ---

    /**
     * 在 Excel 工作表中创建表格
     */
    @Tool(description = "Create a table in the Excel sheet")
    public String excelCreateTable(
            @ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
            @ToolParam(description = "Sheet name where the table is created") String sheetName,
            @ToolParam(description = "Table name to be created") String tableName,
            @ToolParam(description = "Range to be a table (e.g., \"A1:C10\")", required = false) String range) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);

            try (Workbook workbook = openWorkbook(path)) {
                // 表格功能仅支持 .xlsx 格式
                if (!(workbook instanceof XSSFWorkbook xssfWorkbook)) {
                    return "错误: 创建表格功能仅支持 .xlsx 格式文件";
                }

                XSSFWorkbook xssf = xssfWorkbook;
                org.apache.poi.xssf.usermodel.XSSFSheet xssfSheet = xssf.getSheet(sheetName);
                if (xssfSheet == null) {
                    return "错误: 工作表不存在 '" + sheetName + "'";
                }

                // 确定表格范围
                AreaReference areaRef;
                if (range != null && !range.isBlank()) {
                    areaRef = new AreaReference(range, workbook.getSpreadsheetVersion());
                } else {
                    // 使用工作表的已用范围
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
                    areaRef = new AreaReference(
                            new CellReference(0, 0),
                            new CellReference(lastRow, lastCol - 1),
                            workbook.getSpreadsheetVersion());
                }

                // 创建表格
                XSSFTable table = xssfSheet.createTable(areaRef);
                table.setName(tableName);
                // 设置表格样式
                table.getCTTable().addNewTableStyleInfo();
                table.getCTTable().getTableStyleInfo().setName("TableStyleMedium2");
                table.getCTTable().getTableStyleInfo().setShowColumnStripes(false);
                table.getCTTable().getTableStyleInfo().setShowRowStripes(true);

                saveWorkbook(workbook, path);
                return "成功创建表格: 名称='" + tableName + "', 工作表=" + sheetName + ", 范围="
                        + areaRef.formatAsString() + ", 文件=" + fileAbsolutePath;
            }
        } catch (Exception e) {
            log.error("excel_create_table 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 6. excel_format_range ---

    /**
     * 对 Excel 工作表中的单元格范围应用样式
     */
    @Tool(description = "Format cells in the Excel sheet with style information")
    public String excelFormatRange(
            @ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
            @ToolParam(description = "Sheet name in the Excel file") String sheetName,
            @ToolParam(description = "Range of cells in the Excel sheet (e.g., \"A1:C3\")") String range,
            @ToolParam(description = "2D array of style objects for each cell. If a cell does not change style, use null. The number of items of the array must match the range size.") List<List<CellStyleInfo>> styles) {
        try {
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
                        return "错误: styles 第" + (r + 1) + "行的列数(" + rowStyles.size()
                                + ")与范围列数(" + expectedCols + ")不匹配";
                    }
                    int targetRow = firstRow + r;
                    Row row = sheet.getRow(targetRow);
                    if (row == null) {
                        row = sheet.createRow(targetRow);
                    }
                    for (int c = 0; c < rowStyles.size(); c++) {
                        CellStyleInfo styleInfo = rowStyles.get(c);
                        if (styleInfo == null) {
                            continue; // 跳过不改变样式的单元格
                        }
                        int targetCol = firstCol + c;
                        Cell cell = row.getCell(targetCol);
                        if (cell == null) {
                            cell = row.createCell(targetCol);
                        }
                        applyCellStyle(workbook, cell, styleInfo);
                    }
                }

                saveWorkbook(workbook, path);
                return "成功格式化: 文件=" + fileAbsolutePath + ", 工作表=" + sheetName + ", 范围=" + range;
            }
        } catch (Exception e) {
            log.error("excel_format_range 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 7. excel_screen_capture ---

    /**
     * [Windows/跨平台] 截取 Excel 工作表的屏幕截图，返回 base64 编码的 PNG 图片
     */
    @Tool(description = "[Windows only] Take a screenshot of the Excel sheet with pagination.")
    public String excelScreenCapture(
            @ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
            @ToolParam(description = "Sheet name in the Excel file") String sheetName,
            @ToolParam(description = "Range of cells to read in the Excel sheet (e.g., \"A1:C10\"). [default: first paging range]", required = false) String range) {
        try {
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
                    // 默认渲染第一页：最多 SCREENSHOT_MAX_ROWS 行
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

                // 渲染为图片
                BufferedImage image = renderSheetToImage(sheet, firstRow, firstCol, lastRow, lastCol);

                // 编码为 base64 PNG
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("mimeType", "image/png");
                result.put("sheetName", sheetName);
                result.put("range", CellReference.convertNumToColString(firstCol) + (firstRow + 1) + ":"
                        + CellReference.convertNumToColString(lastCol) + (lastRow + 1));
                result.put("data", base64);

                return JSON.toJSONString(result);
            }
        } catch (Exception e) {
            log.error("excel_screen_capture 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }
}