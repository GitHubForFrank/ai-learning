package com.example.mcp.handler.ppt;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.pojo.ppt.AddChartRequest;
import com.example.mcp.pojo.ppt.AddShapeRequest;
import com.example.mcp.util.FileValidateUtil;
import com.example.mcp.util.LogUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP PPT 演示文稿高级操作工具，提供幻灯片导出图片、图表占位符、图形添加和幻灯片统计功能。
 * 基于 Apache POI 库操作 pptx 格式文件。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class PptAdvancedHandler extends BaseHandler {

    /**
     * 校验 PPT 文件路径
     */
    private Path validatePptxFile(String fileAbsolutePath) {
        return FileValidateUtil.validateFile(fileAbsolutePath, ".pptx");
    }

    /**
     * 打开 PPT 文档
     */
    private XMLSlideShow openSlideShow(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath)) {
            return new XMLSlideShow(is);
        }
    }

    /**
     * 保存 PPT 文档
     */
    private void saveSlideShow(XMLSlideShow slideShow, Path filePath) throws IOException {
        try (OutputStream os = Files.newOutputStream(filePath)) {
            slideShow.write(os);
        }
    }

    // --- 1. ppt_export_slides_as_images ---

    /**
     * 将 PPT 的所有幻灯片导出为 PNG 格式图片，保存到指定目录。
     * 使用 Java AWT 渲染幻灯片内容，不支持复杂的动画和过渡效果。
     *
     * @param fileAbsolutePath PPT 文件的绝对路径
     * @param outputDir        输出目录的绝对路径，图片命名格式为 slide_1.png, slide_2.png ...
     * @return 操作结果消息
     */
    @Tool(name = "ppt_export_slides_as_images", description = "将幻灯片导出为 PNG 格式图片，输出到指定目录。")
    public String pptExportSlidesAsImages(@ToolParam(description = "PPT 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "输出目录的绝对路径，图片命名格式为 slide_1.png, slide_2.png ...") String outputDir) {
        return execute("pptExportSlidesAsImages", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            Path outDir = Paths.get(outputDir);
            if (!Files.exists(outDir)) {
                Files.createDirectories(outDir);
            }

            XMLSlideShow ppt = new XMLSlideShow(Files.newInputStream(path));
            Dimension pageSize = ppt.getPageSize();
            List<XSLFSlide> slides = ppt.getSlides();
            int exportedCount = 0;

            for (int i = 0; i < slides.size(); i++) {
                XSLFSlide slide = slides.get(i);
                BufferedImage img = new BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = img.createGraphics();
                graphics.setPaint(Color.WHITE);
                graphics.fill(new Rectangle2D.Float(0, 0, pageSize.width, pageSize.height));
                slide.draw(graphics);
                graphics.dispose();

                String fileName = "slide_" + (i + 1) + ".png";
                Path outputPath = outDir.resolve(fileName);
                ImageIO.write(img, "PNG", outputPath.toFile());
                exportedCount++;
            }

            ppt.close();
            LogUtil.info("pptExportSlidesAsImages 完成，共导出 {} 张图片到: {}", exportedCount, outputDir);
            return "已导出 " + exportedCount + " 张幻灯片图片到: " + outputDir;
        });
    }

    // --- 2. ppt_add_chart ---

    /**
     * 在指定幻灯片中添加图表占位区域。
     * 注意：此工具创建的是一个图表占位区域（矩形+文字说明），
     * 实际的图表数据需要在 PowerPoint 中手动创建和编辑。
     *
     * @param fileAbsolutePath PPT 文件的绝对路径
     * @param slideIndex       幻灯片索引（从0开始）
     * @param chartType        图表类型: bar(柱状图), line(折线图), pie(饼图), area(面积图), scatter(散点图)
     * @param title            图表标题
     * @param x                X坐标（默认 50）
     * @param y                Y坐标（默认 100）
     * @param width            图表宽度（默认 600）
     * @param height           图表高度（默认 350）
     * @return 操作结果消息
     */
    @Tool(name = "ppt_add_chart", description = "在幻灯片中嵌入图表占位区域。实际图表数据需在 PowerPoint 中手动编辑。")
    public String pptAddChart(@ToolParam(description = "PPT添加图表请求参数") AddChartRequest request) {
        return execute("pptAddChart", () -> {
            Path path = validatePptxFile(request.fileAbsolutePath());
            int dx = request.x() != null ? request.x() : 50;
            int dy = request.y() != null ? request.y() : 100;
            int dw = request.width() != null ? request.width() : 600;
            int dh = request.height() != null ? request.height() : 350;

            // 验证图表类型
            String chartType = request.chartType();
            String normalizedType = (chartType != null && !chartType.isBlank()) ? chartType.toLowerCase()
                                                                                           .trim() : "bar";
            String chartTypeDisplay = switch (normalizedType) {
                case "bar" -> "柱状图";
                case "line" -> "折线图";
                case "pie" -> "饼图";
                case "area" -> "面积图";
                case "scatter" -> "散点图";
                default -> {
                    LogUtil.warn("pptAddChart 不支持的图表类型: {}", chartType);
                    yield "柱状图";
                }
            };

            try (XMLSlideShow ppt = openSlideShow(path)) {
                int slideIndex = request.slideIndex();
                List<XSLFSlide> slides = ppt.getSlides();
                if (slideIndex < 0 || slideIndex >= slides.size()) {
                    return String.format("错误: 幻灯片索引 %d 超出范围，共 %d 张幻灯片", slideIndex, slides.size());
                }

                XSLFSlide slide = slides.get(slideIndex);

                // 创建图表占位矩形区域
                XSLFAutoShape shape = slide.createAutoShape();
                shape.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
                shape.setAnchor(new Rectangle2D.Double(dx, dy, dw, dh));
                shape.setFillColor(new Color(240, 240, 250));
                shape.setLineColor(new Color(180, 180, 200));
                shape.setLineWidth(2.0);

                // 在矩形内添加标题文本
                String title = request.title();
                XSLFTextBox textBox = slide.createTextBox();
                textBox.setAnchor(new Rectangle2D.Double(dx + 20, dy + dh / 2 - 40, dw - 40, 80));
                XSLFTextParagraph paragraph = textBox.addNewTextParagraph();
                paragraph.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
                XSLFTextRun run = paragraph.addNewTextRun();
                run.setText("[" + chartTypeDisplay + "] " + title);
                run.setFontSize(18.0);
                run.setBold(true);
                run.setFontColor(new Color(80, 80, 120));

                XSLFTextParagraph subParagraph = textBox.addNewTextParagraph();
                subParagraph.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
                XSLFTextRun subRun = subParagraph.addNewTextRun();
                subRun.setText("（请在 PowerPoint 中双击编辑图表数据）");
                subRun.setFontSize(11.0);
                subRun.setFontColor(new Color(150, 150, 150));

                saveSlideShow(ppt, path);
                LogUtil.info("pptAddChart 完成，幻灯片: {}, 图表类型: {}, 标题: {}", slideIndex + 1, chartTypeDisplay, title);
                return "已添加" + chartTypeDisplay + "图表占位区域 '" + title + "' 到幻灯片 " + (slideIndex + 1) + ": " + request.fileAbsolutePath();
            }
        });
    }

    // --- 3. ppt_add_shape ---

    /**
     * 在指定幻灯片中添加图形（矩形/圆形/箭头/星形等），支持自定义位置、大小和颜色。
     *
     * @param fileAbsolutePath PPT 文件的绝对路径
     * @param slideIndex       幻灯片索引（从0开始）
     * @param shapeType        图形类型: rectangle(矩形), circle(圆形/椭圆), arrow_right(右箭头), arrow_left(左箭头),
     *                         arrow_up(上箭头), arrow_down(下箭头), star(五角星), triangle(三角形), diamond(菱形),
     *                         rounded_rect(圆角矩形), pentagon(五边形), hexagon(六边形)
     * @param x                X坐标（默认 100）
     * @param y                Y坐标（默认 100）
     * @param width            图形宽度（默认 200）
     * @param height           图形高度（默认 200）
     * @param fillColor        填充颜色，十六进制格式，如 "#FF0000"（默认 "#4472C4"）
     * @param lineColor        边框颜色，十六进制格式（默认 "#333333"）
     * @param lineWidth        边框宽度（默认 1.0）
     * @return 操作结果消息
     */
    @Tool(name = "ppt_add_shape", description = "在幻灯片中添加图形（矩形/圆形/箭头/星形等），支持自定义位置、大小和颜色。")
    public String pptAddShape(
        @ToolParam(description = "PPT添加图形请求参数，包含文件路径、幻灯片索引、图形类型、位置、大小和样式") AddShapeRequest request) {
        return execute("pptAddShape", () -> {
            Path path = validatePptxFile(request.fileAbsolutePath());
            int dx = request.x() != null ? request.x() : 100;
            int dy = request.y() != null ? request.y() : 100;
            int dw = request.width() != null ? request.width() : 200;
            int dh = request.height() != null ? request.height() : 200;
            String fillColor = request.fillColor();
            String fill = (fillColor != null && !fillColor.isBlank()) ? fillColor : "#4472C4";
            String lineColor = request.lineColor();
            String line = (lineColor != null && !lineColor.isBlank()) ? lineColor : "#333333";
            Double lineWidth = request.lineWidth();
            double lw = lineWidth != null ? lineWidth : 1.0;

            String shapeType = request.shapeType();
            org.apache.poi.sl.usermodel.ShapeType poiShapeType = parseShapeType(shapeType);

            try (XMLSlideShow ppt = openSlideShow(path)) {
                int slideIndex = request.slideIndex();
                List<XSLFSlide> slides = ppt.getSlides();
                if (slideIndex < 0 || slideIndex >= slides.size()) {
                    return String.format("错误: 幻灯片索引 %d 超出范围，共 %d 张幻灯片", slideIndex, slides.size());
                }

                XSLFSlide slide = slides.get(slideIndex);
                XSLFAutoShape shape = slide.createAutoShape();
                shape.setShapeType(poiShapeType);
                shape.setAnchor(new Rectangle2D.Double(dx, dy, dw, dh));
                shape.setFillColor(Color.decode(fill));
                shape.setLineColor(Color.decode(line));
                shape.setLineWidth(lw);

                saveSlideShow(ppt, path);
                String shapeName = (shapeType != null && !shapeType.isBlank()) ? shapeType : "rectangle";
                LogUtil.info("pptAddShape 完成，幻灯片: {}, 类型: {}, 位置: ({},{}), 大小: {}x{}", slideIndex + 1, shapeName, dx, dy, dw, dh);
                return "已添加 " + shapeName + " 图形到幻灯片 " + (slideIndex + 1) + ": " + request.fileAbsolutePath();
            }
        });
    }

    /**
     * 解析图形类型字符串为 POI ShapeType
     */
    private org.apache.poi.sl.usermodel.ShapeType parseShapeType(String shapeType) {
        if (shapeType == null || shapeType.isBlank()) {
            return org.apache.poi.sl.usermodel.ShapeType.RECT;
        }
        return switch (shapeType.toLowerCase()
                                .trim()) {
            case "rectangle", "rect" -> org.apache.poi.sl.usermodel.ShapeType.RECT;
            case "circle", "oval", "ellipse" -> org.apache.poi.sl.usermodel.ShapeType.ELLIPSE;
            case "arrow_right", "arrowright" -> org.apache.poi.sl.usermodel.ShapeType.RIGHT_ARROW;
            case "arrow_left", "arrowleft" -> org.apache.poi.sl.usermodel.ShapeType.LEFT_ARROW;
            case "arrow_up", "arrowup" -> org.apache.poi.sl.usermodel.ShapeType.UP_ARROW;
            case "arrow_down", "arrowdown" -> org.apache.poi.sl.usermodel.ShapeType.DOWN_ARROW;
            case "star", "pentagram" -> org.apache.poi.sl.usermodel.ShapeType.PENTAGON;
            case "triangle" -> org.apache.poi.sl.usermodel.ShapeType.TRIANGLE;
            case "diamond", "rhombus" -> org.apache.poi.sl.usermodel.ShapeType.DIAMOND;
            case "rounded_rect", "roundedrect", "roundrect" -> org.apache.poi.sl.usermodel.ShapeType.ROUND_RECT;
            case "pentagon" -> org.apache.poi.sl.usermodel.ShapeType.PENTAGON;
            case "hexagon" -> org.apache.poi.sl.usermodel.ShapeType.HEXAGON;
            case "chevron" -> org.apache.poi.sl.usermodel.ShapeType.CHEVRON;
            case "parallelogram" -> org.apache.poi.sl.usermodel.ShapeType.PARALLELOGRAM;
            case "trapezoid" -> org.apache.poi.sl.usermodel.ShapeType.TRAPEZOID;
            default -> {
                LogUtil.warn("parseShapeType 未知类型: {}，使用默认矩形", shapeType);
                yield org.apache.poi.sl.usermodel.ShapeType.RECT;
            }
        };
    }

    // --- 4. ppt_get_slide_count ---

    /**
     * 获取 PPT 的页数和每页标题列表。
     * 遍历所有幻灯片，提取每页的第一个文本形状作为标题。
     *
     * @param fileAbsolutePath PPT 文件的绝对路径
     * @return PPT 页数统计和每页标题列表
     */
    @Tool(name = "ppt_get_slide_count", description = "获取 PPT 的页数和每页标题列表。")
    public String pptGetSlideCount(@ToolParam(description = "PPT 文件的绝对路径") String fileAbsolutePath) {
        return execute("pptGetSlideCount", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            try (XMLSlideShow ppt = openSlideShow(path)) {
                List<XSLFSlide> slides = ppt.getSlides();
                StringBuilder sb = new StringBuilder();
                sb.append("PPT 文件: ")
                  .append(fileAbsolutePath)
                  .append("\n");
                sb.append("总幻灯片数: ")
                  .append(slides.size())
                  .append("\n");
                sb.append("========================================\n");

                for (int i = 0; i < slides.size(); i++) {
                    XSLFSlide slide = slides.get(i);
                    String slideTitle = extractSlideTitle(slide);
                    sb.append(String.format("幻灯片 %d: %s\n", i + 1, slideTitle.isEmpty() ? "（无标题）" : slideTitle));
                }

                sb.append("========================================\n");
                int titledSlides = 0;
                for (XSLFSlide slide : slides) {
                    if (!extractSlideTitle(slide).isEmpty()) {
                        titledSlides++;
                    }
                }
                sb.append("统计: 共 ")
                  .append(slides.size())
                  .append(" 页，其中 ")
                  .append(titledSlides)
                  .append(" 页有标题\n");

                LogUtil.info("pptGetSlideCount 完成，共 {} 页: {}", slides.size(), fileAbsolutePath);
                return sb.toString()
                         .trim();
            }
        });
    }

    /**
     * 提取幻灯片的标题文本（第一个文本形状的内容）
     */
    private String extractSlideTitle(XSLFSlide slide) {
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                String text = textShape.getText();
                if (text != null && !text.isBlank()) {
                    // 返回第一行作为标题
                    String[] lines = text.split("\\n");
                    String firstLine = lines[0].trim();
                    if (firstLine.length() > 60) {
                        firstLine = firstLine.substring(0, 57) + "...";
                    }
                    return firstLine;
                }
            }
        }
        return "";
    }
}
