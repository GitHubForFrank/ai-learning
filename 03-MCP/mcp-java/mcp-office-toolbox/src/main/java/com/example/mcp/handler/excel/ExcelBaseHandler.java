package com.example.mcp.handler.excel;

import com.example.mcp.handler.BaseHandler;
import com.example.mcp.pojo.excel.BorderInfo;
import com.example.mcp.pojo.excel.CellStyleInfo;
import com.example.mcp.pojo.excel.FillInfo;
import com.example.mcp.pojo.excel.FontInfo;
import com.example.mcp.util.FileValidateUtil;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;

/**
 * Excel 工具 Handler 的抽象基类，提供所有子 Handler 共享的辅助方法。
 * 包括文件校验、工作簿打开/保存、单元格读取/写入、样式应用、截图渲染等。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
public abstract class ExcelBaseHandler extends BaseHandler {

    /**
     * 默认分页行数
     */
    protected static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * 屏幕截图渲染参数
     */
    protected static final int SCREENSHOT_COL_WIDTH = 120;
    protected static final int SCREENSHOT_ROW_HEIGHT = 24;
    protected static final int SCREENSHOT_PADDING = 6;
    protected static final int SCREENSHOT_MAX_COLS = 20;
    protected static final int SCREENSHOT_MAX_ROWS = 100;

    // ==================== 文件校验 ====================

    /**
     * 校验文件路径是否存在且为 Excel 文件（.xlsx 或 .xls），委托给 {@link FileValidateUtil}。
     *
     * @param fileAbsolutePath 文件的绝对路径
     * @return 解析后的 Path 对象
     * @throws IllegalArgumentException 如果文件不存在或格式不支持
     */
    protected Path validateExcelFile(String fileAbsolutePath) {
        return FileValidateUtil.validateFile(fileAbsolutePath, ".xlsx", ".xls");
    }

    // ==================== 工作簿操作 ====================

    /**
     * 打开 Excel 工作簿（自动识别 .xls / .xlsx）。
     *
     * @param filePath 文件路径
     * @return Workbook 实例
     * @throws IOException 如果文件读取失败
     */
    protected Workbook openWorkbook(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath)) {
            return WorkbookFactory.create(is);
        }
    }

    /**
     * 保存工作簿到文件。
     *
     * @param workbook 工作簿实例
     * @param filePath 目标文件路径
     * @throws IOException 如果写入失败
     */
    protected void saveWorkbook(Workbook workbook, Path filePath) throws IOException {
        try (OutputStream os = Files.newOutputStream(filePath)) {
            workbook.write(os);
        }
    }

    // ==================== 工作表操作 ====================

    /**
     * 获取工作表；如果不存在则抛出异常。
     *
     * @param workbook  工作簿
     * @param sheetName 工作表名称
     * @return Sheet 实例
     * @throws IllegalArgumentException 如果工作表不存在
     */
    protected Sheet getSheet(Workbook workbook, String sheetName) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("工作表不存在: " + sheetName);
        }
        return sheet;
    }

    // ==================== 范围解析 ====================

    /**
     * 解析范围字符串如 "A1:C10"，返回起始行列和结束行列（0-based）。
     *
     * @param range 范围字符串
     * @return int[]{firstRow, firstCol, lastRow, lastCol}
     */
    protected int[] parseRange(String range) {
        CellRangeAddress cra = CellRangeAddress.valueOf(range);
        return new int[]{cra.getFirstRow(), cra.getFirstColumn(), cra.getLastRow(), cra.getLastColumn()};
    }

    // ==================== 单元格读取 ====================

    /**
     * 获取单元格的字符串值（用于读取）。
     *
     * @param cell        单元格
     * @param showFormula 是否显示公式而非计算值
     * @return 单元格的字符串表示
     */
    protected String getCellStringValue(Cell cell, boolean showFormula) {
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
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }

    /**
     * 获取单元格值的 JSON 表示（保留类型信息）。
     *
     * @param cell        单元格
     * @param showFormula 是否显示公式
     * @return 单元格值的 JSON 兼容表示
     */
    protected Object getCellJsonValue(Cell cell, boolean showFormula) {
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
     * 获取单元格的样式描述（JSON 对象）。
     *
     * @param cell 单元格
     * @return 样式信息的 Map
     */
    protected Map<String, Object> getCellStyleMap(Cell cell) {
        Map<String, Object> styleMap = new LinkedHashMap<>();
        if (cell == null) {
            return styleMap;
        }
        CellStyle style = cell.getCellStyle();
        if (style == null) {
            return styleMap;
        }

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

        styleMap.put("fillPattern", style.getFillPattern()
                                         .name());
        if (style.getFillForegroundColor() > 0) {
            styleMap.put("fillColor", String.format("#%06X", style.getFillForegroundColor() & 0xFFFFFF));
        }

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

        styleMap.put("alignment", style.getAlignment()
                                       .name());
        styleMap.put("verticalAlignment", style.getVerticalAlignment()
                                               .name());
        styleMap.put("dataFormat", style.getDataFormatString());

        return styleMap;
    }

    // ==================== 颜色工具 ====================

    /**
     * 将十六进制颜色字符串转换为 AWT Color。
     *
     * @param hex 十六进制颜色字符串，如 "#FF0000"
     * @return AWT Color 对象
     */
    protected java.awt.Color hexToColor(String hex) {
        if (hex == null || !hex.matches("^#[0-9A-Fa-f]{6}$")) {
            return java.awt.Color.BLACK;
        }
        return new java.awt.Color(Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(hex.substring(3, 5), 16),
                                  Integer.parseInt(hex.substring(5, 7), 16));
    }

    // ==================== 单元格写入 ====================

    /**
     * 向单元格写入值，支持字符串、数字、布尔值、null 和公式（以 = 开头）。
     *
     * @param cell  目标单元格
     * @param value 要写入的值
     */
    protected void writeCellValue(Cell cell, Object value) {
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

    // ==================== 样式应用 ====================

    /**
     * 应用单元格样式。
     *
     * @param workbook  工作簿
     * @param cell      目标单元格
     * @param styleInfo 样式信息
     */
    protected void applyCellStyle(Workbook workbook, Cell cell, CellStyleInfo styleInfo) {
        if (styleInfo == null) {
            return;
        }
        CellStyle style = workbook.createCellStyle();

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

        if (styleInfo.numFmt() != null) {
            style.setDataFormat(workbook.createDataFormat()
                                        .getFormat(styleInfo.numFmt()));
        }

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
                }
            }
        }

        cell.setCellStyle(style);
    }

    // ==================== 截图渲染 ====================

    /**
     * 将工作表渲染为 BufferedImage。
     *
     * @param sheet    工作表
     * @param firstRow 起始行
     * @param firstCol 起始列
     * @param lastRow  结束行
     * @param lastCol  结束列
     * @return 渲染后的 BufferedImage
     */
    protected BufferedImage renderSheetToImage(Sheet sheet, int firstRow, int firstCol, int lastRow, int lastCol) {
        int rowCount = lastRow - firstRow + 1;
        int colCount = lastCol - firstCol + 1;

        colCount = Math.min(colCount, SCREENSHOT_MAX_COLS);
        rowCount = Math.min(rowCount, SCREENSHOT_MAX_ROWS);
        lastCol = firstCol + colCount - 1;
        lastRow = firstRow + rowCount - 1;

        int imgWidth = colCount * SCREENSHOT_COL_WIDTH + 1;
        int imgHeight = (rowCount + 1) * SCREENSHOT_ROW_HEIGHT + 1;

        BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, imgWidth, imgHeight);

        g2d.setColor(new java.awt.Color(200, 200, 200));
        for (int i = 0; i <= colCount; i++) {
            int x = i * SCREENSHOT_COL_WIDTH;
            g2d.drawLine(x, 0, x, imgHeight);
        }
        for (int i = 0; i <= rowCount + 1; i++) {
            int y = i * SCREENSHOT_ROW_HEIGHT;
            g2d.drawLine(0, y, imgWidth, y);
        }

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

        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 11));
        DataFormatter formatter = new DataFormatter();
        for (int row = firstRow; row <= lastRow; row++) {
            Row sheetRow = sheet.getRow(row);
            int drawRow = row - firstRow + 1;
            for (int col = firstCol; col <= lastCol; col++) {
                String text = "";
                if (sheetRow != null) {
                    Cell cell = sheetRow.getCell(col);
                    if (cell != null) {
                        text = formatter.formatCellValue(cell);
                    }
                }
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

    // ==================== 单元格复制 ====================

    /**
     * 将源单元格的值复制到目标单元格。
     *
     * @param src  源单元格
     * @param dest 目标单元格
     */
    protected void copyCellValue(Cell src, Cell dest) {
        switch (src.getCellType()) {
            case STRING -> dest.setCellValue(src.getStringCellValue());
            case NUMERIC -> dest.setCellValue(src.getNumericCellValue());
            case BOOLEAN -> dest.setCellValue(src.getBooleanCellValue());
            case FORMULA -> dest.setCellFormula(src.getCellFormula());
            case BLANK -> dest.setBlank();
            default -> dest.setBlank();
        }
    }

    /**
     * 将源工作表的数据复制到目标工作表。
     *
     * @param src           源工作表
     * @param dest          目标工作表
     * @param includeHeader 是否包含标题行
     */
    protected void copySheetData(Sheet src, Sheet dest, boolean includeHeader) {
        int startRow = includeHeader ? src.getFirstRowNum() : src.getFirstRowNum() + 1;
        int destRowIdx = 0;
        for (int r = startRow; r <= src.getLastRowNum(); r++) {
            Row srcRow = src.getRow(r);
            if (srcRow == null) {
                continue;
            }
            Row destRow = dest.createRow(destRowIdx++);
            for (int c = srcRow.getFirstCellNum(); c < srcRow.getLastCellNum(); c++) {
                Cell srcCell = srcRow.getCell(c);
                if (srcCell == null) {
                    continue;
                }
                Cell destCell = destRow.createCell(c);
                copyCellValue(srcCell, destCell);
            }
        }
    }

    // ==================== 对比辅助 ====================

    /**
     * 读取工作表所有行到 Map，以指定列值为键。
     *
     * @param sheet          工作表
     * @param keyColumnIndex 关键列索引
     * @param map            目标 Map
     */
    protected void readSheetToMap(Sheet sheet, int keyColumnIndex, Map<String, List<Object>> map) {
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            String key = getCellStringValue(row.getCell(keyColumnIndex), false);
            List<Object> rowValues = new ArrayList<>();
            for (int c = 0; c < row.getLastCellNum(); c++) {
                rowValues.add(getCellJsonValue(row.getCell(c), false));
            }
            map.put(key, rowValues);
        }
    }

    /**
     * 比较两行数据是否相等。
     *
     * @param row1 第一行数据
     * @param row2 第二行数据
     * @return 是否相等
     */
    protected boolean rowValuesEqual(List<Object> row1, List<Object> row2) {
        int size = Math.max(row1.size(), row2.size());
        for (int i = 0; i < size; i++) {
            Object v1 = i < row1.size() ? row1.get(i) : null;
            Object v2 = i < row2.size() ? row2.get(i) : null;
            if (v1 == null && v2 == null) {
                continue;
            }
            if (v1 == null || v2 == null) {
                return false;
            }
            if (!String.valueOf(v1)
                       .equals(String.valueOf(v2))) {
                return false;
            }
        }
        return true;
    }

    // ==================== 内部辅助类 ====================

    /**
     * 内部辅助类：存储一行数据。
     */
    protected record RowData(List<Object> values) {

    }
}