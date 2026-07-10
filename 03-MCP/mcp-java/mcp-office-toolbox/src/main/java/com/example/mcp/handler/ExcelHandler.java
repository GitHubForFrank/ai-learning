package com.example.mcp.handler;

import com.alibaba.fastjson2.JSON;
import com.example.mcp.pojo.excel.BorderInfo;
import com.example.mcp.pojo.excel.CellStyleInfo;
import com.example.mcp.pojo.excel.FillInfo;
import com.example.mcp.pojo.excel.FontInfo;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.example.mcp.util.LogUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Excel 工具实现，完整复刻 mcp_Excel 的所有功能：
 * excel_describe_sheets、excel_read_sheet、excel_write_to_sheet、
 * excel_copy_sheet、excel_create_table、excel_format_range、excel_screen_capture
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@Service
public class ExcelHandler {

    /**
     * 默认分页行数
     */
    private static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * 屏幕截图渲染参数
     */
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
        String name = path.getFileName()
                          .toString()
                          .toLowerCase();
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
        return new int[]{cra.getFirstRow(), cra.getFirstColumn(), cra.getLastRow(), cra.getLastColumn()};
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
                        return cell.getLocalDateTimeCellValue()
                                   .toString();
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
                    return cell.getLocalDateTimeCellValue()
                               .toString();
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
        org.apache.poi.ss.usermodel.Font font = cell.getSheet()
                                                    .getWorkbook()
                                                    .getFontAt(style.getFontIndex());
        if (font != null) {
            Map<String, Object> fontMap = new LinkedHashMap<>();
            fontMap.put("bold", font.getBold());
            fontMap.put("italic", font.getItalic());
            fontMap.put("size", font.getFontHeightInPoints());
            fontMap.put("underline", font.getUnderline() != 0);
            fontMap.put("strike", font.getStrikeout());
            fontMap.put("color", font.getColor() > 0 ? String.format("#%06X", font.getColor() & 0xFFFFFF) : "#000000");
            styleMap.put("font", fontMap);
        }

        // 填充信息
        styleMap.put("fillPattern", style.getFillPattern()
                                         .name());
        if (style.getFillForegroundColor() > 0) {
            styleMap.put("fillColor", String.format("#%06X", style.getFillForegroundColor() & 0xFFFFFF));
        }

        // 边框
        Map<String, Object> borderMap = new LinkedHashMap<>();
        borderMap.put("top", style.getBorderTop()
                                  .name());
        borderMap.put("bottom", style.getBorderBottom()
                                     .name());
        borderMap.put("left", style.getBorderLeft()
                                   .name());
        borderMap.put("right", style.getBorderRight()
                                    .name());
        styleMap.put("border", borderMap);

        // 对齐方式
        styleMap.put("alignment", style.getAlignment()
                                       .name());
        styleMap.put("verticalAlignment", style.getVerticalAlignment()
                                               .name());

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
        return new java.awt.Color(Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(hex.substring(3, 5), 16),
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
            if (fi.bold() != null) {
                font.setBold(fi.bold());
            }
            if (fi.italic() != null) {
                font.setItalic(fi.italic());
            }
            if (fi.size() != null) {
                font.setFontHeightInPoints(fi.size()
                                             .shortValue());
            }
            if (fi.strike() != null) {
                font.setStrikeout(fi.strike());
            }
            if (fi.color() != null) {
                java.awt.Color c = hexToColor(fi.color());
                byte[] rgb = new byte[]{(byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue()};
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
            if ("solid".equals(fill.type()) && fill.color() != null && !fill.color()
                                                                            .isEmpty()) {
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                java.awt.Color c = hexToColor(fill.color()
                                                  .getFirst());
                if (workbook instanceof HSSFWorkbook) {
                    style.setFillForegroundColor(((HSSFWorkbook) workbook).getCustomPalette()
                                                                          .findSimilarColor(c.getRed(), c.getGreen(), c.getBlue())
                                                                          .getIndex());
                } else {
                    ((org.apache.poi.xssf.usermodel.XSSFCellStyle) style).setFillForegroundColor(
                        new org.apache.poi.xssf.usermodel.XSSFColor(c, null));
                }
            }
        }

        // 数字格式
        if (styleInfo.numFmt() != null) {
            style.setDataFormat(workbook.createDataFormat()
                                        .getFormat(styleInfo.numFmt()));
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
    @Tool(name = "excel_describe_sheets", description = "列出指定 Excel 文件的所有工作表信息")
    public String excelDescribeSheets(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath) {
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
            LogUtil.error("excel_describe_sheets 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 2. excel_read_sheet ---

    /**
     * 读取 Excel 工作表中的值，支持分页、显示公式和样式
     */
    @Tool(name = "excel_read_sheet", description = "从 Excel 工作表分页读取数据。")
    public String excelReadSheet(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
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

                // 分页信息
                int totalRows = sheet.getLastRowNum() + 1;
                result.put("totalRows", totalRows);
                result.put("hasMore", lastRow + 1 < totalRows);

                return JSON.toJSONString(result);
            }
        } catch (Exception e) {
            LogUtil.error("excel_read_sheet 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 3. excel_write_to_sheet ---

    /**
     * 向 Excel 工作表写入值
     */
    @Tool(name = "excel_write_to_sheet", description = "向 Excel 工作表写入数据")
    public String excelWriteToSheet(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
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
            LogUtil.error("excel_write_to_sheet 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 4. excel_copy_sheet ---

    /**
     * 复制已有工作表到新工作表
     */
    @Tool(name = "excel_copy_sheet", description = "将现有工作表复制到新工作表")
    public String excelCopySheet(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
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
            LogUtil.error("excel_copy_sheet 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 5. excel_create_table ---

    /**
     * 在 Excel 工作表中创建表格
     */
    @Tool(name = "excel_create_table", description = "在 Excel 工作表中创建表格")
    public String excelCreateTable(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
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
                    areaRef = new AreaReference(new CellReference(0, 0), new CellReference(lastRow, lastCol - 1), workbook.getSpreadsheetVersion());
                }

                // 创建表格
                XSSFTable table = xssfSheet.createTable(areaRef);
                table.setName(tableName);
                // 设置表格样式
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
        } catch (Exception e) {
            LogUtil.error("excel_create_table 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 6. excel_format_range ---

    /**
     * 对 Excel 工作表中的单元格范围应用样式
     */
    @Tool(name = "excel_format_range", description = "使用样式信息格式化 Excel 工作表中的单元格")
    public String excelFormatRange(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
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
            LogUtil.error("excel_format_range 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 7. excel_screen_capture ---

    /**
     * [Windows/跨平台] 截取 Excel 工作表的屏幕截图，返回 base64 编码的 PNG 图片
     */
    @Tool(name = "excel_screen_capture", description = "截取 Excel 工作表的屏幕截图，支持分页。")
    public String excelScreenCapture(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
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
        } catch (Exception e) {
            LogUtil.error("excel_screen_capture 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 8. excel_create_workbook ---

    /**
     * 新建空白工作簿（.xlsx）
     */
    @Tool(name = "excel_create_workbook", description = "创建新的空白 Excel 工作簿。如果文件已存在则覆盖。")
    public String excelCreateWorkbook(@ToolParam(description = "Absolute path for the new Excel file") String fileAbsolutePath) {
        try {
            Path path = Paths.get(fileAbsolutePath);
            // 确保父目录存在
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                // 创建默认工作表
                workbook.createSheet("Sheet1");
                saveWorkbook(workbook, path);
            }
            return "空白工作簿已创建: " + fileAbsolutePath;
        } catch (Exception e) {
            LogUtil.error("excelCreateWorkbook 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 9. excel_delete_row ---

    /**
     * 删除指定行
     */
    @Tool(name = "excel_delete_row", description = "从 Excel 工作表中删除指定行。行索引从 0 开始。")
    public String excelDeleteRow(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Row index to delete (0-based)") int rowIndex) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                if (rowIndex < 0 || rowIndex > sheet.getLastRowNum()) {
                    return "错误：行索引 " + rowIndex + " 超出范围";
                }
                // 移除行
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    sheet.removeRow(row);
                }
                // 如果删除的不是最后一行，需要上移后续行
                if (rowIndex <= sheet.getLastRowNum()) {
                    int lastRow = sheet.getLastRowNum();
                    sheet.shiftRows(rowIndex + 1, lastRow, -1);
                }
                saveWorkbook(workbook, path);
                return "已删除第 " + (rowIndex + 1) + " 行: " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        } catch (Exception e) {
            LogUtil.error("excelDeleteRow 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 10. excel_delete_column ---

    /**
     * 删除指定列
     */
    @Tool(name = "excel_delete_column", description = "从 Excel 工作表中删除指定列。列索引从 0 开始。")
    public String excelDeleteColumn(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Column index to delete (0-based)") int columnIndex) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                if (columnIndex < 0) {
                    return "错误：列索引不能为负数";
                }
                // 遍历所有行，删除指定列的单元格
                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row != null) {
                        // 移除指定列的单元格
                        Cell cell = row.getCell(columnIndex);
                        if (cell != null) {
                            row.removeCell(cell);
                        }
                        // 将后续列左移
                        int lastCol = row.getLastCellNum();
                        for (int c = columnIndex; c < lastCol - 1; c++) {
                            Cell srcCell = row.getCell(c + 1);
                            Cell destCell = row.getCell(c);
                            if (destCell == null) {
                                destCell = row.createCell(c);
                            }
                            if (srcCell != null) {
                                // 复制值
                                switch (srcCell.getCellType()) {
                                    case STRING -> destCell.setCellValue(srcCell.getStringCellValue());
                                    case NUMERIC -> destCell.setCellValue(srcCell.getNumericCellValue());
                                    case BOOLEAN -> destCell.setCellValue(srcCell.getBooleanCellValue());
                                    case FORMULA -> destCell.setCellFormula(srcCell.getCellFormula());
                                    default -> destCell.setBlank();
                                }
                            }
                        }
                        // 清除最后一列
                        Cell lastCell = row.getCell(lastCol - 1);
                        if (lastCell != null) {
                            row.removeCell(lastCell);
                        }
                    }
                }
                saveWorkbook(workbook, path);
                return "已删除第 " + (columnIndex + 1) + " 列: " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        } catch (Exception e) {
            LogUtil.error("excelDeleteColumn 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 11. excel_clear_sheet ---

    /**
     * 清空整张表格数据
     */
    @Tool(name = "excel_clear_sheet", description = "清空 Excel 工作表中的所有数据，移除所有行但保留工作表结构。")
    public String excelClearSheet(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name to clear") String sheetName) {
        try {
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
        } catch (Exception e) {
            LogUtil.error("excelClearSheet 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // ==================== 内部辅助方法（新增） ====================

    private void copyCellValue(Cell src, Cell dest) {
        switch (src.getCellType()) {
            case STRING -> dest.setCellValue(src.getStringCellValue());
            case NUMERIC -> dest.setCellValue(src.getNumericCellValue());
            case BOOLEAN -> dest.setCellValue(src.getBooleanCellValue());
            case FORMULA -> dest.setCellFormula(src.getCellFormula());
            case BLANK -> dest.setBlank();
            default -> dest.setBlank();
        }
    }

    private void copySheetData(Sheet src, Sheet dest, boolean includeHeader) {
        int startRow = includeHeader ? src.getFirstRowNum() : src.getFirstRowNum() + 1;
        int destRowIdx = 0;
        for (int r = startRow; r <= src.getLastRowNum(); r++) {
            Row srcRow = src.getRow(r);
            if (srcRow == null) continue;
            Row destRow = dest.createRow(destRowIdx++);
            for (int c = srcRow.getFirstCellNum(); c < srcRow.getLastCellNum(); c++) {
                Cell srcCell = srcRow.getCell(c);
                if (srcCell == null) continue;
                Cell destCell = destRow.createCell(c);
                copyCellValue(srcCell, destCell);
            }
        }
    }

    // ==================== P0: 基础操作补齐 ====================

    // --- 12. excel_insert_row ---

    @Tool(name = "excel_insert_row", description = "在指定位置插入空行，现有行向下移动。行索引从 0 开始。")
    public String excelInsertRow(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Row index to insert at (0-based)") int rowIndex) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                int lastRow = sheet.getLastRowNum();
                if (rowIndex < 0 || rowIndex > lastRow + 1)
                    return "错误：行索引 " + rowIndex + " 超出范围（0~" + (lastRow + 1) + "）";
                if (rowIndex <= lastRow) sheet.shiftRows(rowIndex, lastRow, 1);
                sheet.createRow(rowIndex);
                saveWorkbook(workbook, path);
                return "已在第 " + (rowIndex + 1) + " 行插入空行: " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        } catch (Exception e) {
            LogUtil.error("excelInsertRow 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 13. excel_insert_column ---

    @Tool(name = "excel_insert_column", description = "在指定位置插入空列，现有列向右移动。列索引从 0 开始。")
    public String excelInsertColumn(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Column index to insert at (0-based)") int columnIndex) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                if (columnIndex < 0) return "错误：列索引不能为负数";
                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row != null) {
                        int lastCol = row.getLastCellNum();
                        for (int c = lastCol; c > columnIndex; c--) {
                            Cell srcCell = row.getCell(c - 1);
                            Cell destCell = row.getCell(c);
                            if (destCell == null) destCell = row.createCell(c);
                            if (srcCell != null) copyCellValue(srcCell, destCell);
                            else destCell.setBlank();
                        }
                        Cell insertCell = row.getCell(columnIndex);
                        if (insertCell != null) insertCell.setBlank();
                    }
                }
                saveWorkbook(workbook, path);
                return "已在第 " + (columnIndex + 1) + " 列插入空列: " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        } catch (Exception e) {
            LogUtil.error("excelInsertColumn 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 14. excel_delete_sheet ---

    @Tool(name = "excel_delete_sheet", description = "删除 Excel 工作簿中指定名称的工作表。")
    public String excelDeleteSheet(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name to delete") String sheetName) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) return "错误: 工作表不存在 '" + sheetName + "'";
                if (workbook.getNumberOfSheets() <= 1) return "错误: 工作簿至少需要保留一个工作表";
                int idx = workbook.getSheetIndex(sheet);
                workbook.removeSheetAt(idx);
                saveWorkbook(workbook, path);
                return "已删除工作表 '" + sheetName + "': " + fileAbsolutePath;
            }
        } catch (Exception e) {
            LogUtil.error("excelDeleteSheet 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 15. excel_rename_sheet ---

    @Tool(name = "excel_rename_sheet", description = "重命名 Excel 工作簿中的工作表。")
    public String excelRenameSheet(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Current sheet name") String oldName,
        @ToolParam(description = "New sheet name") String newName) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = workbook.getSheet(oldName);
                if (sheet == null) return "错误: 工作表不存在 '" + oldName + "'";
                if (workbook.getSheet(newName) != null) return "错误: 新名称 '" + newName + "' 已存在";
                int idx = workbook.getSheetIndex(sheet);
                workbook.setSheetName(idx, newName);
                saveWorkbook(workbook, path);
                return "已将工作表 '" + oldName + "' 重命名为 '" + newName + "': " + fileAbsolutePath;
            }
        } catch (Exception e) {
            LogUtil.error("excelRenameSheet 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // ==================== P1: 列宽/行高与视图控制 ====================

    // --- 16. excel_auto_fit_columns ---

    @Tool(name = "excel_auto_fit_columns", description = "自动调整 Excel 工作表中指定列的宽度以适配内容。")
    public String excelAutoFitColumns(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Comma-separated column indices to auto-fit (0-based), e.g. \"0,1,2\". Leave empty to auto-fit all columns.", required = false) String columnIndices) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                if (columnIndices == null || columnIndices.isBlank()) {
                    int maxCol = 0;
                    for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
                        Row row = sheet.getRow(r);
                        if (row != null && row.getLastCellNum() > maxCol) maxCol = row.getLastCellNum();
                    }
                    for (int i = 0; i < maxCol; i++) sheet.autoSizeColumn(i);
                    return "已自动调整所有列宽: " + fileAbsolutePath + ", 工作表=" + sheetName;
                } else {
                    for (String idx : columnIndices.split(",")) {
                        int colIdx = Integer.parseInt(idx.trim());
                        sheet.autoSizeColumn(colIdx);
                    }
                    return "已自动调整列宽: " + fileAbsolutePath + ", 工作表=" + sheetName + ", 列=" + columnIndices;
                }
            }
        } catch (Exception e) {
            LogUtil.error("excelAutoFitColumns 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 17. excel_set_column_width ---

    @Tool(name = "excel_set_column_width", description = "设置 Excel 工作表中指定列的宽度（字符数）。")
    public String excelSetColumnWidth(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Column index (0-based)") int columnIndex,
        @ToolParam(description = "Column width in characters") int width) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                if (columnIndex < 0) return "错误：列索引不能为负数";
                sheet.setColumnWidth(columnIndex, width * 256);
                saveWorkbook(workbook, path);
                return "已设置第 " + (columnIndex + 1) + " 列宽度为 " + width + ": " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        } catch (Exception e) {
            LogUtil.error("excelSetColumnWidth 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 18. excel_set_row_height ---

    @Tool(name = "excel_set_row_height", description = "设置 Excel 工作表中指定行的高度（磅值）。")
    public String excelSetRowHeight(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Row index (0-based)") int rowIndex,
        @ToolParam(description = "Row height in points") float height) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                if (rowIndex < 0) return "错误：行索引不能为负数";
                Row row = sheet.getRow(rowIndex);
                if (row == null) row = sheet.createRow(rowIndex);
                row.setHeightInPoints(height);
                saveWorkbook(workbook, path);
                return "已设置第 " + (rowIndex + 1) + " 行高度为 " + height + "pt: " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        } catch (Exception e) {
            LogUtil.error("excelSetRowHeight 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 19. excel_freeze_panes ---

    @Tool(name = "excel_freeze_panes", description = "冻结 Excel 工作表的窗格，滚动时保持指定行列可见。")
    public String excelFreezePanes(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Number of columns to freeze (0-based split index)") int colSplit,
        @ToolParam(description = "Number of rows to freeze (0-based split index)") int rowSplit) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                sheet.createFreezePane(colSplit, rowSplit);
                saveWorkbook(workbook, path);
                return "已冻结窗格: 列=" + colSplit + ", 行=" + rowSplit + ", 文件=" + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        } catch (Exception e) {
            LogUtil.error("excelFreezePanes 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 20. excel_merge_cells ---

    @Tool(name = "excel_merge_cells", description = "合并或取消合并 Excel 工作表中的单元格区域。")
    public String excelMergeCells(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Range to merge, e.g. \"A1:C1\"") String range,
        @ToolParam(description = "True to merge, false to unmerge") boolean merge) {
        try {
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
                        if (existing.formatAsString().equals(range)) {
                            sheet.removeMergedRegion(i);
                            removed = true;
                        }
                    }
                    return removed ? "已取消合并单元格: " + range + ", 文件=" + fileAbsolutePath + ", 工作表=" + sheetName
                        : "未找到合并区域: " + range;
                }
            }
        } catch (Exception e) {
            LogUtil.error("excelMergeCells 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // ==================== P2: 数据处理与分析 ====================

    // --- 21. excel_sort_range ---

    @Tool(name = "excel_sort_range", description = "按指定列对 Excel 工作表数据区域进行排序。第一行为标题行不参与排序。")
    public String excelSortRange(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Range to sort (must include header row), e.g. \"A1:C10\"") String range,
        @ToolParam(description = "Column index within the range to sort by (0-based)") int sortColumnIndex,
        @ToolParam(description = "True for ascending, false for descending") boolean ascending) {
        try {
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
                    for (int c = firstCol; c <= lastCol; c++)
                        rowData.add(getCellJsonValue(row != null ? row.getCell(c) : null, false));
                    allRows.add(rowData);
                }
                int sortIdx = sortColumnIndex;
                allRows.sort((a, b) -> {
                    Object va = a.size() > sortIdx ? a.get(sortIdx) : null;
                    Object vb = b.size() > sortIdx ? b.get(sortIdx) : null;
                    if (va == null && vb == null) return 0;
                    if (va == null) return ascending ? 1 : -1;
                    if (vb == null) return ascending ? -1 : 1;
                    int cmp;
                    if (va instanceof Number na && vb instanceof Number nb)
                        cmp = Double.compare(na.doubleValue(), nb.doubleValue());
                    else cmp = String.valueOf(va).compareTo(String.valueOf(vb));
                    return ascending ? cmp : -cmp;
                });
                for (int i = 0; i < allRows.size(); i++) {
                    int targetRow = firstRow + 1 + i;
                    Row row = sheet.getRow(targetRow);
                    if (row == null) row = sheet.createRow(targetRow);
                    List<Object> rowData = allRows.get(i);
                    for (int c = 0; c < rowData.size(); c++) {
                        Cell cell = row.getCell(firstCol + c);
                        if (cell == null) cell = row.createCell(firstCol + c);
                        writeCellValue(cell, rowData.get(c));
                    }
                }
                saveWorkbook(workbook, path);
                String dir = ascending ? "升序" : "降序";
                return "已按第 " + (sortColumnIndex + 1) + " 列" + dir + "排序: " + fileAbsolutePath + ", 工作表=" + sheetName + ", 范围=" + range;
            }
        } catch (Exception e) {
            LogUtil.error("excelSortRange 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 22. excel_filter_range ---

    @Tool(name = "excel_filter_range", description = "为 Excel 工作表数据区域添加自动筛选。")
    public String excelFilterRange(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Range to apply filter, e.g. \"A1:C10\"", required = false) String range) {
        try {
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
                        if (row != null && row.getLastCellNum() > lastCol) lastCol = row.getLastCellNum();
                    }
                    if (lastRow < 0 || lastCol == 0) return "错误: 工作表为空，无法自动添加筛选";
                    sheet.setAutoFilter(new CellRangeAddress(0, lastRow, 0, lastCol - 1));
                }
                saveWorkbook(workbook, path);
                return "已添加自动筛选: " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        } catch (Exception e) {
            LogUtil.error("excelFilterRange 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 23. excel_find_replace ---

    @Tool(name = "excel_find_replace", description = "在 Excel 工作表中查找并替换文本内容。")
    public String excelFindReplace(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Text to find") String findText,
        @ToolParam(description = "Text to replace with") String replaceText,
        @ToolParam(description = "Range to search, e.g. \"A1:C10\". Leave empty to search entire sheet.", required = false) String range) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                int firstRow, firstCol, lastRow, lastCol;
                if (range != null && !range.isBlank()) {
                    int[] bounds = parseRange(range);
                    firstRow = bounds[0]; firstCol = bounds[1]; lastRow = bounds[2]; lastCol = bounds[3];
                } else {
                    firstRow = 0; firstCol = 0; lastRow = sheet.getLastRowNum();
                    lastCol = 0;
                    for (int r = firstRow; r <= lastRow; r++) {
                        Row row = sheet.getRow(r);
                        if (row != null && row.getLastCellNum() > lastCol) lastCol = row.getLastCellNum();
                    }
                    lastCol = Math.max(0, lastCol - 1);
                }
                int count = 0;
                for (int r = firstRow; r <= lastRow; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
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
                return "已替换 " + count + " 个单元格: 将 '" + findText + "' 替换为 '" + replaceText + "', 文件=" + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        } catch (Exception e) {
            LogUtil.error("excelFindReplace 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 24. excel_remove_duplicates ---

    @Tool(name = "excel_remove_duplicates", description = "按指定列去除 Excel 工作表中的重复行，保留第一次出现的行。")
    public String excelRemoveDuplicates(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Comma-separated column indices (0-based) to check for duplicates, e.g. \"0,1\"") String columnIndices,
        @ToolParam(description = "Row index to start from (0-based). [default: 0, first row is header]", required = false) Integer startRow) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            String[] colStr = columnIndices.split(",");
            int[] cols = new int[colStr.length];
            for (int i = 0; i < colStr.length; i++) cols[i] = Integer.parseInt(colStr[i].trim());
            int start = startRow != null ? startRow : 0;
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                Set<String> seen = new HashSet<>();
                int removed = 0;
                for (int r = sheet.getLastRowNum(); r >= start; r--) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    StringBuilder key = new StringBuilder();
                    for (int c : cols) {
                        Cell cell = row.getCell(c);
                        key.append(getCellStringValue(cell, false)).append("\t");
                    }
                    if (!seen.add(key.toString())) {
                        sheet.removeRow(row);
                        if (r < sheet.getLastRowNum()) sheet.shiftRows(r + 1, sheet.getLastRowNum(), -1);
                        removed++;
                    }
                }
                saveWorkbook(workbook, path);
                return "已移除 " + removed + " 个重复行: " + fileAbsolutePath + ", 工作表=" + sheetName + ", 检查列=" + columnIndices;
            }
        } catch (Exception e) {
            LogUtil.error("excelRemoveDuplicates 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 25. excel_apply_formula ---

    @Tool(name = "excel_apply_formula", description = "对 Excel 工作表中的单元格范围批量应用公式（如 SUM、AVERAGE 等）。")
    public String excelApplyFormula(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Target cell or range to apply formula, e.g. \"D2\" or \"D2:D10\"") String target,
        @ToolParam(description = "Formula to apply (starting with =), e.g. \"=SUM(A2:A10)\"") String formula) {
        try {
            Path path = validateExcelFile(fileAbsolutePath);
            try (Workbook workbook = openWorkbook(path)) {
                Sheet sheet = getSheet(workbook, sheetName);
                String formulaStr = formula.startsWith("=") ? formula.substring(1) : formula;
                int[] bounds = parseRange(target);
                for (int r = bounds[0]; r <= bounds[2]; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) row = sheet.createRow(r);
                    for (int c = bounds[1]; c <= bounds[3]; c++) {
                        Cell cell = row.getCell(c);
                        if (cell == null) cell = row.createCell(c);
                        cell.setCellFormula(formulaStr);
                    }
                }
                saveWorkbook(workbook, path);
                return "已应用公式 '" + formula + "' 到范围 " + target + ": " + fileAbsolutePath + ", 工作表=" + sheetName;
            }
        } catch (Exception e) {
            LogUtil.error("excelApplyFormula 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // ==================== P3: 高级功能 ====================

    // --- 26. excel_data_validation ---

    @Tool(name = "excel_data_validation", description = "为 Excel 工作表单元格添加数据验证，支持下拉列表。")
    public String excelDataValidation(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Range to apply validation, e.g. \"A2:A10\"") String range,
        @ToolParam(description = "Comma-separated list of allowed values for dropdown, e.g. \"是,否\"") String allowedValues) {
        try {
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
        } catch (Exception e) {
            LogUtil.error("excelDataValidation 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 27. excel_convert_csv ---

    @Tool(name = "excel_convert_csv", description = "CSV 与 Excel 文件互转。根据源文件扩展名自动判断转换方向。")
    public String excelConvertCsv(@ToolParam(description = "Absolute path to the source file (.csv or .xlsx)") String sourceFilePath,
        @ToolParam(description = "Absolute path to the target file (.xlsx or .csv)") String targetFilePath) {
        try {
            String srcLower = sourceFilePath.toLowerCase();
            String tgtLower = targetFilePath.toLowerCase();
            if (srcLower.endsWith(".csv") && tgtLower.endsWith(".xlsx")) {
                Path srcPath = Paths.get(sourceFilePath);
                if (!Files.exists(srcPath)) return "错误: 源文件不存在: " + sourceFilePath;
                Path tgtPath = Paths.get(targetFilePath);
                Path parent = tgtPath.getParent();
                if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
                try (BufferedReader reader = new BufferedReader(new FileReader(sourceFilePath));
                     Workbook workbook = new XSSFWorkbook()) {
                    Sheet sheet = workbook.createSheet("Sheet1");
                    String line;
                    int rowNum = 0;
                    while ((line = reader.readLine()) != null) {
                        Row row = sheet.createRow(rowNum++);
                        String[] parts = parseCsvLine(line);
                        for (int i = 0; i < parts.length; i++) row.createCell(i).setCellValue(parts[i]);
                    }
                    saveWorkbook(workbook, tgtPath);
                }
                return "已从 CSV 转换为 Excel: " + sourceFilePath + " → " + targetFilePath;
            } else if ((srcLower.endsWith(".xlsx") || srcLower.endsWith(".xls")) && tgtLower.endsWith(".csv")) {
                Path srcPath = validateExcelFile(sourceFilePath);
                Path tgtPath = Paths.get(targetFilePath);
                Path parent = tgtPath.getParent();
                if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
                try (Workbook workbook = openWorkbook(srcPath);
                     FileWriter writer = new FileWriter(targetFilePath)) {
                    Sheet sheet = workbook.getSheetAt(0);
                    for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                        Row row = sheet.getRow(r);
                        StringBuilder sb = new StringBuilder();
                        if (row != null) {
                            for (int c = 0; c < row.getLastCellNum(); c++) {
                                if (c > 0) sb.append(",");
                                Cell cell = row.getCell(c);
                                String val = getCellStringValue(cell, false);
                                if (val.contains(",") || val.contains("\"") || val.contains("\n"))
                                    val = "\"" + val.replace("\"", "\"\"") + "\"";
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
        } catch (Exception e) {
            LogUtil.error("excelConvertCsv 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                    else inQuotes = false;
                } else sb.append(c);
            } else {
                if (c == '"') inQuotes = true;
                else if (c == ',') { result.add(sb.toString()); sb.setLength(0); }
                else sb.append(c);
            }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }

    // --- 28. excel_merge_workbooks ---

    @Tool(name = "excel_merge_workbooks", description = "将多个 Excel 工作簿的所有工作表合并到一个目标工作簿中。")
    public String excelMergeWorkbooks(@ToolParam(description = "Absolute path to the target Excel file (will be created if not exists)") String targetFilePath,
        @ToolParam(description = "Comma-separated absolute paths to source Excel files") String sourceFilePaths,
        @ToolParam(description = "Whether to include header row when copying data", required = false) Boolean includeHeader) {
        try {
            boolean includeHdr = includeHeader != null && includeHeader;
            String[] sources = sourceFilePaths.split(",");
            Path tgtPath = Paths.get(targetFilePath);
            Path parent = tgtPath.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            try (Workbook targetWorkbook = new XSSFWorkbook()) {
                int totalSheets = 0;
                for (String src : sources) {
                    Path srcFile = Paths.get(src.trim());
                    if (!Files.exists(srcFile)) return "错误: 源文件不存在: " + src.trim();
                    try (Workbook sourceWorkbook = openWorkbook(srcFile)) {
                        for (int i = 0; i < sourceWorkbook.getNumberOfSheets(); i++) {
                            Sheet srcSheet = sourceWorkbook.getSheetAt(i);
                            String sheetName = srcSheet.getSheetName();
                            int suffix = 1;
                            String baseName = sheetName;
                            while (targetWorkbook.getSheet(sheetName) != null)
                                sheetName = baseName + "_" + (suffix++);
                            Sheet destSheet = targetWorkbook.createSheet(sheetName);
                            copySheetData(srcSheet, destSheet, includeHdr);
                            totalSheets++;
                        }
                    }
                }
                saveWorkbook(targetWorkbook, tgtPath);
                return "已合并 " + sources.length + " 个工作簿（共 " + totalSheets + " 个工作表）到: " + targetFilePath;
            }
        } catch (Exception e) {
            LogUtil.error("excelMergeWorkbooks 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 29. excel_conditional_format ---

    @Tool(name = "excel_conditional_format", description = "为 Excel 工作表单元格添加条件格式，根据数值大小高亮显示。")
    public String excelConditionalFormat(@ToolParam(description = "Absolute path to the Excel file") String fileAbsolutePath,
        @ToolParam(description = "Sheet name in the Excel file") String sheetName,
        @ToolParam(description = "Range to apply conditional formatting, e.g. \"A2:A10\"") String range,
        @ToolParam(description = "Type: \"cellValue\" (highlight cells based on value), \"dataBar\" (data bar)") String type,
        @ToolParam(description = "Comparison operator: \">\", \">=\", \"<\", \"<=\", \"=\", \"!=\" (for cellValue type)") String operator,
        @ToolParam(description = "Threshold value for comparison (for cellValue type)") String value,
        @ToolParam(description = "Fill color name from predefined set: RED, YELLOW, GREEN, BLUE, ORANGE, PINK, GREY_25_PERCENT, LIGHT_BLUE, etc.") String fillColor) {
        try {
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
        } catch (Exception e) {
            LogUtil.error("excelConditionalFormat 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }
}