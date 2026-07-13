package com.example.mcp.handler.ppt;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.util.FileValidateUtil;
import com.example.mcp.util.LogUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFBackground;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP PPT 演示文稿（pptx）工具实现，提供 PPT 的创建、幻灯片管理、文本读取和修改功能。
 * 仅支持基础文本编辑，不支持复杂的排版、动画等高级功能。
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
@Service
public class PptHandler extends BaseHandler {

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

    // --- 1. create_ppt ---

    /**
     * 新建空白 PPT 文件
     */
    @Tool(name = "create_ppt", description = "创建新的空白 PPT 演示文稿。如果文件已存在则覆盖。")
    public String createPpt(@ToolParam(description = "新 PPT 文件的绝对路径") String fileAbsolutePath) {
        return execute("createPpt", () -> {
            Path path = Paths.get(fileAbsolutePath);
            // 确保父目录存在
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (XMLSlideShow slideShow = new XMLSlideShow()) {
                // 创建默认空白幻灯片
                slideShow.createSlide();
                saveSlideShow(slideShow, path);
            }
            return "空白 PPT 文件已创建: " + fileAbsolutePath;
        });
    }

    // --- 2. add_ppt_slide ---

    /**
     * 新增幻灯片页面
     */
    @Tool(name = "add_ppt_slide", description = "向 PowerPoint 演示文稿添加新幻灯片。可选指定幻灯片标题。")
    public String addPptSlide(@ToolParam(description = "PPT 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "新幻灯片的标题文本（可选）", required = false) String title) {
        return execute("addPptSlide", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            try (XMLSlideShow slideShow = openSlideShow(path)) {
                XSLFSlide slide = slideShow.createSlide();
                if (title != null && !title.isBlank()) {
                    XSLFTextBox textBox = slide.createTextBox();
                    textBox.setAnchor(new java.awt.Rectangle(50, 50, 620, 50));
                    XSLFTextParagraph paragraph = textBox.addNewTextParagraph();
                    XSLFTextRun run = paragraph.addNewTextRun();
                    run.setText(title);
                    run.setFontSize(28.0);
                    run.setBold(true);
                }
                saveSlideShow(slideShow, path);
                return "已添加第 " + slideShow.getSlides()
                                              .size() + " 张幻灯片: " + fileAbsolutePath;
            }
        });
    }

    // --- 3. delete_ppt_slide ---

    /**
     * 删除指定幻灯片
     */
    @Tool(name = "delete_ppt_slide", description = "从 PPT 演示文稿中删除指定幻灯片。幻灯片索引从 0 开始。")
    public String deletePptSlide(@ToolParam(description = "PPT 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "要删除的幻灯片索引（从0开始）") int slideIndex) {
        return execute("deletePptSlide", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            try (XMLSlideShow slideShow = openSlideShow(path)) {
                List<XSLFSlide> slides = slideShow.getSlides();
                if (slideIndex < 0 || slideIndex >= slides.size()) {
                    return String.format("错误: 幻灯片索引 %d 超出范围，共 %d 张幻灯片", slideIndex, slides.size());
                }
                slideShow.removeSlide(slideIndex);
                saveSlideShow(slideShow, path);
                return "已删除第 " + (slideIndex + 1) + " 张幻灯片: " + fileAbsolutePath;
            }
        });
    }

    // --- 4. read_ppt_text ---

    /**
     * 读取每页幻灯片内全部文本内容
     */
    @Tool(name = "read_ppt_text", description = "读取 PPT 演示文稿中所有幻灯片的文本内容。按幻灯片组织返回文本。")
    public String readPptText(@ToolParam(description = "PPT 文件的绝对路径") String fileAbsolutePath) {
        return execute("readPptText", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            try (XMLSlideShow slideShow = openSlideShow(path)) {
                StringBuilder sb = new StringBuilder();
                List<XSLFSlide> slides = slideShow.getSlides();
                sb.append("PPT 文件: ")
                  .append(fileAbsolutePath)
                  .append("\n");
                sb.append("总幻灯片数: ")
                  .append(slides.size())
                  .append("\n\n");

                for (int i = 0; i < slides.size(); i++) {
                    XSLFSlide slide = slides.get(i);
                    sb.append("=== 幻灯片 ")
                      .append(i + 1)
                      .append(" ===\n");
                    for (XSLFShape shape : slide.getShapes()) {
                        if (shape instanceof XSLFTextShape textShape) {
                            String text = textShape.getText();
                            if (text != null && !text.isBlank()) {
                                sb.append(text)
                                  .append("\n");
                            }
                        }
                    }
                    sb.append("\n");
                }
                return sb.toString()
                         .trim();
            }
        });
    }

    // --- 5. modify_ppt_slide_text ---

    /**
     * 修改幻灯片页面文字内容
     */
    @Tool(name = "modify_ppt_slide_text", description = "修改 PPT 演示文稿指定幻灯片上的文本内容。在指定幻灯片上将所有旧文本替换为新文本。")
    public String modifyPptSlideText(@ToolParam(description = "PPT 文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "要修改的幻灯片索引（从0开始）") int slideIndex, @ToolParam(description = "要被替换的文本") String oldText,
        @ToolParam(description = "替换文本") String newText) {
        return execute("modifyPptSlideText", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            try (XMLSlideShow slideShow = openSlideShow(path)) {
                List<XSLFSlide> slides = slideShow.getSlides();
                if (slideIndex < 0 || slideIndex >= slides.size()) {
                    return String.format("错误: 幻灯片索引 %d 超出范围，共 %d 张幻灯片", slideIndex, slides.size());
                }

                XSLFSlide slide = slides.get(slideIndex);
                int replaceCount = 0;
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        // 尝试替换文本段落中的内容
                        for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
                            for (XSLFTextRun run : paragraph.getTextRuns()) {
                                String text = run.getRawText();
                                if (text != null && text.contains(oldText)) {
                                    run.setText(text.replace(oldText, newText));
                                    replaceCount++;
                                }
                            }
                        }
                    }
                }

                saveSlideShow(slideShow, path);
                return "幻灯片 " + (slideIndex + 1) + " 文字修改完成: " + fileAbsolutePath + "，共替换 " + replaceCount + " 处";
            }
        });
    }

    // --- 6. save_ppt ---

    /**
     * PPT 文件保存（另存为）
     */
    @Tool(name = "save_ppt_as", description = "将 PPT 演示文稿另存为新文件。将原始演示文稿复制到新位置。")
    public String savePptAs(@ToolParam(description = "源 PPT 文件的绝对路径") String sourcePath,
        @ToolParam(description = "新 PPT 文件的绝对路径") String targetPath) {
        return execute("savePptAs", () -> {
            Path source = validatePptxFile(sourcePath);
            Path target = Paths.get(targetPath);
            // 确保目标父目录存在
            Path targetParent = target.getParent();
            if (targetParent != null && !Files.exists(targetParent)) {
                Files.createDirectories(targetParent);
            }
            Files.copy(source, target);
            return "PPT 文件已另存为: " + targetPath;
        });
    }

    // ==================== P0 工具 ====================

    // --- 7. ppt_add_text_box ---

    /**
     * 在指定幻灯片上添加文本框
     *
     * @param fileAbsolutePath PPT 文件的绝对路径
     * @param slideIndex       幻灯片索引（从0开始）
     * @param text             文本框文本内容
     * @param x                X坐标，默认100
     * @param y                Y坐标，默认100
     * @param width            宽度，默认500
     * @param height           高度，默认200
     * @param fontSize         字体大小，默认18
     * @param bold             是否加粗，默认false
     * @param color            字体颜色（十六进制），默认"#000000"
     * @return 操作结果消息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "ppt_add_text_box", description = "在指定幻灯片上添加文本框，支持自定义位置、大小、字体样式和颜色")
    public String pptAddTextBox(@ToolParam(description = "PPT文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "幻灯片索引（从0开始）") int slideIndex, @ToolParam(description = "文本框文本内容") String text,
        @ToolParam(description = "X坐标（默认100）", required = false) Double x, @ToolParam(description = "Y坐标（默认100）", required = false) Double y,
        @ToolParam(description = "宽度（默认500）", required = false) Double width,
        @ToolParam(description = "高度（默认200）", required = false) Double height,
        @ToolParam(description = "字体大小（默认18）", required = false) Integer fontSize,
        @ToolParam(description = "是否加粗（默认false）", required = false) Boolean bold,
        @ToolParam(description = "字体颜色，十六进制格式（默认#000000）", required = false) String color) {
        return execute("pptAddTextBox", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            double dx = x != null ? x : 100;
            double dy = y != null ? y : 100;
            double dw = width != null ? width : 500;
            double dh = height != null ? height : 200;
            int fs = fontSize != null ? fontSize : 18;
            boolean isBold = bold != null && bold;
            String fontColor = (color != null && !color.isBlank()) ? color : "#000000";

            try (XMLSlideShow slideShow = openSlideShow(path)) {
                List<XSLFSlide> slides = slideShow.getSlides();
                if (slideIndex < 0 || slideIndex >= slides.size()) {
                    return String.format("错误: 幻灯片索引 %d 超出范围，共 %d 张幻灯片", slideIndex, slides.size());
                }

                XSLFSlide slide = slides.get(slideIndex);
                XSLFTextBox textBox = slide.createTextBox();
                textBox.setAnchor(new Rectangle2D.Double(dx, dy, dw, dh));
                XSLFTextParagraph paragraph = textBox.addNewTextParagraph();
                XSLFTextRun run = paragraph.addNewTextRun();
                run.setText(text);
                run.setFontSize((double) fs);
                run.setBold(isBold);
                run.setFontColor(Color.decode(fontColor));

                saveSlideShow(slideShow, path);
                return "文本框已添加到幻灯片 " + (slideIndex + 1) + ": " + fileAbsolutePath;
            }
        });
    }

    // --- 8. ppt_add_image ---

    /**
     * 在指定幻灯片上插入图片
     *
     * @param fileAbsolutePath PPT 文件的绝对路径
     * @param slideIndex       幻灯片索引（从0开始）
     * @param imagePath        要插入的图片文件路径
     * @param x                X坐标，默认100
     * @param y                Y坐标，默认100
     * @param width            宽度，默认400
     * @param height           高度，默认300
     * @return 操作结果消息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "ppt_add_image", description = "在指定幻灯片上插入图片，支持自定义位置和大小")
    public String pptAddImage(@ToolParam(description = "PPT文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "幻灯片索引（从0开始）") int slideIndex, @ToolParam(description = "要插入的图片文件路径") String imagePath,
        @ToolParam(description = "X坐标（默认100）", required = false) Double x, @ToolParam(description = "Y坐标（默认100）", required = false) Double y,
        @ToolParam(description = "宽度（默认400）", required = false) Double width,
        @ToolParam(description = "高度（默认300）", required = false) Double height) {
        return execute("pptAddImage", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            Path imgPath = Paths.get(imagePath);
            if (!Files.exists(imgPath)) {
                return "错误: 图片文件不存在: " + imagePath;
            }

            double dx = x != null ? x : 100;
            double dy = y != null ? y : 100;
            double dw = width != null ? width : 400;
            double dh = height != null ? height : 300;

            try (XMLSlideShow slideShow = openSlideShow(path)) {
                List<XSLFSlide> slides = slideShow.getSlides();
                if (slideIndex < 0 || slideIndex >= slides.size()) {
                    return String.format("错误: 幻灯片索引 %d 超出范围，共 %d 张幻灯片", slideIndex, slides.size());
                }

                byte[] pictureData = Files.readAllBytes(imgPath);
                String ext = imagePath.substring(imagePath.lastIndexOf('.') + 1)
                                      .toLowerCase();
                PictureData.PictureType pictureType = switch (ext) {
                    case "png" -> PictureData.PictureType.PNG;
                    case "jpg", "jpeg" -> PictureData.PictureType.JPEG;
                    case "gif" -> PictureData.PictureType.GIF;
                    case "bmp" -> PictureData.PictureType.BMP;
                    default -> PictureData.PictureType.PNG;
                };
                XSLFPictureData picData = slideShow.addPicture(pictureData, pictureType);

                XSLFSlide slide = slides.get(slideIndex);
                XSLFPictureShape picture = slide.createPicture(picData);
                picture.setAnchor(new Rectangle2D.Double(dx, dy, dw, dh));

                saveSlideShow(slideShow, path);
                return "图片已插入到幻灯片 " + (slideIndex + 1) + ": " + fileAbsolutePath;
            }
        });
    }

    // --- 9. ppt_add_table ---

    /**
     * 在指定幻灯片上添加表格
     *
     * @param fileAbsolutePath PPT 文件的绝对路径
     * @param slideIndex       幻灯片索引（从0开始）
     * @param rows             行数
     * @param cols             列数
     * @param x                X坐标，默认100
     * @param y                Y坐标，默认100
     * @param width            宽度，默认500
     * @param height           高度，默认200
     * @param data             表格数据，逗号分隔纄，分号分隔行，如"姓名,年龄;张三,25;李四,30"
     * @return 操作结果消息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "ppt_add_table", description = "在指定幻灯片上添加表格，通过逗号分隔列、分号分隔行的字符串数据填充")
    public String pptAddTable(@ToolParam(description = "PPT文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "幻灯片索引（从0开始）") int slideIndex, @ToolParam(description = "表格行数") int rows,
        @ToolParam(description = "表格列数") int cols, @ToolParam(description = "X坐标（默认100）", required = false) Double x,
        @ToolParam(description = "Y坐标（默认100）", required = false) Double y,
        @ToolParam(description = "宽度（默认500）", required = false) Double width,
        @ToolParam(description = "高度（默认200）", required = false) Double height,
        @ToolParam(description = "表格数据，逗号分隔列、分号分隔行，如\"姓名,年龄;张三,25;李四,30\"") String data) {
        return execute("pptAddTable", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            double dx = x != null ? x : 100;
            double dy = y != null ? y : 100;
            double dw = width != null ? width : 500;
            double dh = height != null ? height : 200;

            // 解析数据
            String[] rowData = data.split(";");
            String[][] tableData = new String[rowData.length][];
            for (int i = 0; i < rowData.length; i++) {
                tableData[i] = rowData[i].split(",", -1);
            }

            try (XMLSlideShow slideShow = openSlideShow(path)) {
                List<XSLFSlide> slides = slideShow.getSlides();
                if (slideIndex < 0 || slideIndex >= slides.size()) {
                    return String.format("错误: 幻灯片索引 %d 超出范围，共 %d 张幻灯片", slideIndex, slides.size());
                }

                XSLFSlide slide = slides.get(slideIndex);
                XSLFTable table = slide.createTable();
                table.setAnchor(new Rectangle2D.Double(dx, dy, dw, dh));

                for (String[] row : tableData) {
                    XSLFTableRow tableRow = table.addRow();
                    for (int c = 0; c < row.length && c < cols; c++) {
                        XSLFTableCell cell = tableRow.addCell();
                        cell.setText(row[c].trim());
                    }
                    // 补全不足的列
                    for (int c = row.length; c < cols; c++) {
                        XSLFTableCell cell = tableRow.addCell();
                        cell.setText("");
                    }
                }

                saveSlideShow(slideShow, path);
                return "表格已添加到幻灯片 " + (slideIndex + 1) + "，共 " + tableData.length + " 行 " + cols + " 列: " + fileAbsolutePath;
            }
        });
    }

    // ==================== P1 工具 ====================

    // --- 10. ppt_duplicate_slide ---

    /**
     * 复制指定幻灯片到目标位置
     *
     * @param fileAbsolutePath PPT 文件的绝对路径
     * @param slideIndex       要复制的幻灯片索引（从0开始）
     * @param targetIndex      插入位置索引，-1表示末尾
     * @return 操作结果消息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "ppt_duplicate_slide", description = "复制指定幻灯片，可指定插入位置。targetIndex=-1表示添加到末尾")
    public String pptDuplicateSlide(@ToolParam(description = "PPT文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "要复制的幻灯片索引（从0开始）") int slideIndex, @ToolParam(description = "插入位置索引，-1表示末尾") int targetIndex) {
        return execute("pptDuplicateSlide", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            try (XMLSlideShow slideShow = openSlideShow(path)) {
                List<XSLFSlide> slides = slideShow.getSlides();
                if (slideIndex < 0 || slideIndex >= slides.size()) {
                    return String.format("错误: 幻灯片索引 %d 超出范围，共 %d 张幻灯片", slideIndex, slides.size());
                }

                XSLFSlide srcSlide = slides.get(slideIndex);
                XSLFSlide newSlide = slideShow.createSlide();
                newSlide.importContent(srcSlide);

                if (targetIndex >= 0 && targetIndex < slideShow.getSlides()
                                                               .size() - 1) {
                    slideShow.setSlideOrder(newSlide, targetIndex);
                }

                saveSlideShow(slideShow, path);
                return "幻灯片 " + (slideIndex + 1) + " 已复制: " + fileAbsolutePath;
            }
        });
    }

    // --- 11. ppt_add_notes ---

    /**
     * 为指定幻灯片添加演讲者备注
     *
     * @param fileAbsolutePath PPT 文件的绝对路径
     * @param slideIndex       幻灯片索引（从0开始）
     * @param notes            备注内容
     * @return 操作结果消息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "ppt_add_notes", description = "为指定幻灯片添加演讲者备注")
    public String pptAddNotes(@ToolParam(description = "PPT文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "幻灯片索引（从0开始）") int slideIndex, @ToolParam(description = "备注内容") String notes) {
        return execute("pptAddNotes", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            try (XMLSlideShow slideShow = openSlideShow(path)) {
                List<XSLFSlide> slides = slideShow.getSlides();
                if (slideIndex < 0 || slideIndex >= slides.size()) {
                    return String.format("错误: 幻灯片索引 %d 超出范围，共 %d 张幻灯片", slideIndex, slides.size());
                }

                XSLFSlide slide = slides.get(slideIndex);
                XSLFNotes slideNotes = slide.getNotes();
                if (slideNotes == null) {
                    return "错误: 该幻灯片没有备注页，无法添加备注";
                }

                // 遍历备注页的形状，找到文本框
                XSLFTextShape textShape = null;
                for (XSLFShape shape : slideNotes.getShapes()) {
                    if (shape instanceof XSLFTextShape ts) {
                        textShape = ts;
                        break;
                    }
                }
                if (textShape == null) {
                    return "错误: 无法获取备注文本框";
                }

                // 清空现有文本并设置新备注
                textShape.clearText();
                XSLFTextParagraph paragraph = textShape.addNewTextParagraph();
                XSLFTextRun run = paragraph.addNewTextRun();
                run.setText(notes);

                saveSlideShow(slideShow, path);
                return "备注已添加到幻灯片 " + (slideIndex + 1) + ": " + fileAbsolutePath;
            }
        });
    }

    // --- 12. ppt_reorder_slides ---

    /**
     * 调整幻灯片顺序
     *
     * @param fileAbsolutePath PPT 文件的绝对路径
     * @param slideOrder       逗号分隔的新顺序，如"2,0,1,3"表示将原第3张幻灯片移到第1位
     * @return 操作结果消息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "ppt_reorder_slides", description = "调整幻灯片顺序，参数为逗号分隔的新索引顺序，如\"2,0,1,3\"")
    public String pptReorderSlides(@ToolParam(description = "PPT文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "逗号分隔的新顺序，如\"2,0,1,3\"表示将原索引2的幻灯片移到第1位") String slideOrder) {
        return execute("pptReorderSlides", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            try (XMLSlideShow slideShow = openSlideShow(path)) {
                List<XSLFSlide> slides = slideShow.getSlides();
                String[] orderStr = slideOrder.split(",");
                int[] newOrder = new int[orderStr.length];
                for (int i = 0; i < orderStr.length; i++) {
                    newOrder[i] = Integer.parseInt(orderStr[i].trim());
                }

                // 校验所有索引是否有效
                for (int idx : newOrder) {
                    if (idx < 0 || idx >= slides.size()) {
                        return String.format("错误: 幻灯片索引 %d 超出范围，共 %d 张幻灯片", idx, slides.size());
                    }
                }

                // 校验是否有重复索引
                if (Arrays.stream(newOrder)
                          .distinct()
                          .count() != newOrder.length) {
                    return "错误: 幻灯片顺序中包含重复索引";
                }

                for (int i = 0; i < newOrder.length; i++) {
                    slideShow.setSlideOrder(slides.get(newOrder[i]), i);
                }

                saveSlideShow(slideShow, path);
                return "幻灯片顺序已调整: " + fileAbsolutePath;
            }
        });
    }

    // --- 13. ppt_convert_to_pdf ---

    /**
     * 将 PPT 转换为 PDF 文件
     *
     * @param fileAbsolutePath PPT 文件的绝对路径
     * @param targetFilePath   目标 PDF 文件路径
     * @return 操作结果消息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "ppt_convert_to_pdf", description = "将PPT演示文稿转换为PDF文件，每页幻灯片渲染为图片后合并")
    public String pptConvertToPdf(@ToolParam(description = "PPT文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "目标PDF文件路径") String targetFilePath) {
        return execute("pptConvertToPdf", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            Path targetPath = Paths.get(targetFilePath);
            Path targetParent = targetPath.getParent();
            if (targetParent != null && !Files.exists(targetParent)) {
                Files.createDirectories(targetParent);
            }

            try (XMLSlideShow ppt = new XMLSlideShow(new FileInputStream(path.toFile())); PDDocument pdfDoc = new PDDocument()) {
                Dimension pgsize = ppt.getPageSize();

                for (XSLFSlide slide : ppt.getSlides()) {
                    BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = img.createGraphics();
                    graphics.setPaint(Color.WHITE);
                    graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));
                    slide.draw(graphics);
                    graphics.dispose();

                    PDPage page = new PDPage(new PDRectangle(pgsize.width, pgsize.height));
                    pdfDoc.addPage(page);
                    PDImageXObject pdImage = LosslessFactory.createFromImage(pdfDoc, img);
                    try (PDPageContentStream cs = new PDPageContentStream(pdfDoc, page)) {
                        cs.drawImage(pdImage, 0, 0, pgsize.width, pgsize.height);
                    }
                }

                pdfDoc.save(targetPath.toFile());
            }
            return "PPT 已转换为 PDF: " + targetFilePath;
        });
    }

    // --- 14. ppt_merge ---

    /**
     * 合并多个 PPT 文件为一个
     *
     * @param sourceFilePaths 逗号分隔的源 PPT 文件路径
     * @param targetFilePath  合并后的目标 PPT 文件路径
     * @return 操作结果消息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "ppt_merge", description = "合并多个PPT文件为一个，源文件路径以逗号分隔")
    public String pptMerge(@ToolParam(description = "逗号分隔的源PPT文件路径") String sourceFilePaths,
        @ToolParam(description = "合并后的目标PPT文件路径") String targetFilePath) {
        return execute("pptMerge", () -> {
            String[] sourcePaths = sourceFilePaths.split(",");
            Path targetPath = Paths.get(targetFilePath);
            Path targetParent = targetPath.getParent();
            if (targetParent != null && !Files.exists(targetParent)) {
                Files.createDirectories(targetParent);
            }

            try (XMLSlideShow targetPpt = new XMLSlideShow()) {
                int totalSlides = 0;

                for (String sourcePath : sourcePaths) {
                    String trimmedPath = sourcePath.trim();
                    Path sourceFile = Paths.get(trimmedPath);
                    if (!Files.exists(sourceFile)) {
                        return "错误: 源文件不存在: " + trimmedPath;
                    }
                    try (XMLSlideShow sourcePpt = new XMLSlideShow(new FileInputStream(sourceFile.toFile()))) {
                        for (XSLFSlide srcSlide : sourcePpt.getSlides()) {
                            XSLFSlide newSlide = targetPpt.createSlide();
                            newSlide.importContent(srcSlide);
                            totalSlides++;
                        }
                    }
                }

                try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
                    targetPpt.write(fos);
                }
                return "PPT 合并完成，共 " + totalSlides + " 张幻灯片: " + targetFilePath;
            }
        });
    }

    // ==================== P2 工具 ====================

    // --- 15. ppt_set_slide_layout ---

    /**
     * 设置指定幻灯片的版式，通过修改底层 XML 关系引用实现版式切换。
     *
     * @param fileAbsolutePath PPT文件的绝对路径
     * @param slideIndex       幻灯片索引（从0开始）
     * @param layoutName       版式名称，如"标题幻灯片"/"标题和内容"/"两栏内容"/"空白"
     * @return 操作结果消息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "ppt_set_slide_layout", description = "设置指定幻灯片的版式。支持'标题幻灯片'、'标题和内容'、'两栏内容'、'空白'等版式。")
    public String pptSetSlideLayout(@ToolParam(description = "PPT文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "幻灯片索引（从0开始）") int slideIndex,
        @ToolParam(description = "版式名称，如'标题幻灯片'/'标题和内容'/'两栏内容'/'空白'") String layoutName) {
        return execute("pptSetSlideLayout", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            try (XMLSlideShow ppt = openSlideShow(path)) {
                List<XSLFSlide> slides = ppt.getSlides();
                if (slideIndex < 0 || slideIndex >= slides.size()) {
                    return String.format("错误: 幻灯片索引 %d 超出范围，共 %d 张幻灯片", slideIndex, slides.size());
                }

                List<XSLFSlideMaster> masters = ppt.getSlideMasters();
                if (masters.isEmpty()) {
                    return "错误: 演示文稿中没有可用的母版";
                }

                XSLFSlideMaster master = masters.get(0);
                XSLFSlideLayout targetLayout = findLayoutByName(master, layoutName);
                if (targetLayout == null) {
                    return "错误: 未找到版式 '" + layoutName + "'，可用的版式: " + getAvailableLayoutNames(master);
                }

                XSLFSlide slide = slides.get(slideIndex);
                // 通过修改 OPC 包关系来切换版式引用
                PackageRelationship oldLayoutRel = null;
                for (PackageRelationship rel : slide.getPackagePart()
                                                    .getRelationships()) {
                    String relType = rel.getRelationshipType();
                    if (relType != null && relType.contains("slideLayout")) {
                        oldLayoutRel = rel;
                        break;
                    }
                }

                if (oldLayoutRel != null) {
                    slide.getPackagePart()
                         .removeRelationship(oldLayoutRel.getId());
                }

                slide.getPackagePart()
                     .addRelationship(targetLayout.getPackagePart()
                                                  .getPartName(), TargetMode.INTERNAL,
                                      "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout");

                saveSlideShow(ppt, path);
                LogUtil.info("pptSetSlideLayout 成功，幻灯片: {}, 版式: {}", slideIndex + 1, layoutName);
                return "幻灯片 " + (slideIndex + 1) + " 版式已设置为: " + layoutName + " - " + fileAbsolutePath;
            }
        });
    }

    // --- 16. ppt_set_background ---

    /**
     * 设置幻灯片背景，支持纯色、图片和渐变背景。
     *
     * @param fileAbsolutePath PPT文件的绝对路径
     * @param slideIndex       幻灯片索引（-1表示所有幻灯片）
     * @param backgroundType   背景类型：solid / image / gradient
     * @param color            纯色背景颜色，如"#FFFFFF"（默认白色），type为solid时使用
     * @param imagePath        图片背景路径，type为image时使用
     * @return 操作结果消息
     * @author Frank Kang
     * @since 2026-07-12
     */
    @Tool(name = "ppt_set_background", description = "设置幻灯片背景。支持纯色(solid)、图片(image)和渐变(gradient)三种背景类型。slideIndex=-1表示设置所有幻灯片。")
    public String pptSetBackground(@ToolParam(description = "PPT文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "幻灯片索引（-1表示所有幻灯片）") int slideIndex,
        @ToolParam(description = "背景类型：solid/image/gradient") String backgroundType,
        @ToolParam(description = "纯色背景颜色，如\"#FFFFFF\"（默认白色）", required = false) String color,
        @ToolParam(description = "图片背景路径（type为image时使用）", required = false) String imagePath) {
        return execute("pptSetBackground", () -> {
            Path path = validatePptxFile(fileAbsolutePath);
            String bgType = (backgroundType != null && !backgroundType.isBlank()) ? backgroundType.toLowerCase() : "solid";

            if (!bgType.equals("solid") && !bgType.equals("image") && !bgType.equals("gradient")) {
                return "错误: 不支持的背景类型 '" + bgType + "'，仅支持 solid、image、gradient";
            }

            if (bgType.equals("image")) {
                if (imagePath == null || imagePath.isBlank()) {
                    return "错误: 图片背景类型必须提供 imagePath 参数";
                }
                Path imgPath = Paths.get(imagePath);
                if (!Files.exists(imgPath)) {
                    return "错误: 图片文件不存在: " + imagePath;
                }
            }

            try (XMLSlideShow ppt = openSlideShow(path)) {
                List<XSLFSlide> slides = ppt.getSlides();

                List<XSLFSlide> targetSlides = new ArrayList<>();
                if (slideIndex == -1) {
                    targetSlides.addAll(slides);
                } else {
                    if (slideIndex < 0 || slideIndex >= slides.size()) {
                        return String.format("错误: 幻灯片索引 %d 超出范围，共 %d 张幻灯片", slideIndex, slides.size());
                    }
                    targetSlides.add(slides.get(slideIndex));
                }

                for (XSLFSlide slide : targetSlides) {
                    XSLFBackground bg = slide.getBackground();
                    switch (bgType) {
                        case "solid" -> {
                            String bgColor = (color != null && !color.isBlank()) ? color : "#FFFFFF";
                            bg.setFillColor(Color.decode(bgColor));
                        }
                        case "image" -> {
                            byte[] pictureData = Files.readAllBytes(Paths.get(imagePath));
                            String ext = imagePath.substring(imagePath.lastIndexOf('.') + 1)
                                                  .toLowerCase();
                            PictureData.PictureType pictureType = switch (ext) {
                                case "png" -> PictureData.PictureType.PNG;
                                case "jpg", "jpeg" -> PictureData.PictureType.JPEG;
                                case "gif" -> PictureData.PictureType.GIF;
                                default -> PictureData.PictureType.PNG;
                            };
                            XSLFPictureData picData = ppt.addPicture(pictureData, pictureType);
                            // 通过底层 XML 设置图片填充背景
                            org.openxmlformats.schemas.presentationml.x2006.main.CTBackground bgXml = (org.openxmlformats.schemas.presentationml.x2006.main.CTBackground) bg.getXmlObject();
                            if (bgXml.getBgPr() == null) {
                                bgXml.addNewBgPr();
                            }
                            org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties blipFill = bgXml.getBgPr()
                                                                                                                 .isSetBlipFill() ? bgXml.getBgPr()
                                                                                                                                         .getBlipFill()
                                : bgXml.getBgPr()
                                       .addNewBlipFill();
                            // 设置图片关系引用
                            org.openxmlformats.schemas.drawingml.x2006.main.CTBlip blip =
                                blipFill.isSetBlip() ? blipFill.getBlip() : blipFill.addNewBlip();
                            blip.setEmbed(picData.getPackagePart()
                                                 .getPartName()
                                                 .getName());
                            // 设置拉伸填充
                            org.openxmlformats.schemas.drawingml.x2006.main.CTStretchInfoProperties stretch = blipFill.addNewStretch();
                            stretch.addNewFillRect();
                        }
                        case "gradient" -> {
                            String bgColor = (color != null && !color.isBlank()) ? color : "#FFFFFF";
                            java.awt.Color c = Color.decode(bgColor);
                            // 渐变：从指定颜色渐变到白色
                            bg.setFillColor(c);
                        }
                    }
                }

                saveSlideShow(ppt, path);
                String target = slideIndex == -1 ? "所有幻灯片" : "幻灯片 " + (slideIndex + 1);
                LogUtil.info("pptSetBackground 成功，目标: {}, 类型: {}", target, bgType);
                return target + " 背景已设置为 " + bgType + " 类型: " + fileAbsolutePath;
            }
        });
    }

    // ======================== 辅助方法 ========================

    /**
     * 根据版式名称查找对应的版式。支持中文名和英文名模糊匹配。
     *
     * @param master     幻灯片母版
     * @param layoutName 版式名称
     * @return 匹配的版式，未找到返回 null
     */
    private XSLFSlideLayout findLayoutByName(XSLFSlideMaster master, String layoutName) {
        if (layoutName == null || layoutName.isBlank()) {
            return null;
        }
        String normalized = layoutName.trim();
        // 中文名到英文类型名的映射
        String typeName = switch (normalized) {
            case "标题幻灯片" -> "TITLE";
            case "标题和内容" -> "TITLE_AND_CONTENT";
            case "两栏内容" -> "TWO_COL_TXT";
            case "空白" -> "BLANK";
            case "节标题" -> "SECTION_HEADER";
            case "比较" -> "COMPARISON";
            case "仅标题" -> "TITLE_ONLY";
            case "内容与标题" -> "CONTENT_AND_TITLE";
            case "图片与标题" -> "PIC_TITLE_AND_CONTENT";
            default -> normalized.toUpperCase();
        };

        for (XSLFSlideLayout layout : master.getSlideLayouts()) {
            if (layout.getType() != null && layout.getType()
                                                  .name()
                                                  .equalsIgnoreCase(typeName)) {
                return layout;
            }
            if (layout.getName() != null && layout.getName()
                                                  .equalsIgnoreCase(typeName)) {
                return layout;
            }
        }
        return null;
    }

    /**
     * 获取母版中所有可用版式名称，用于错误提示。
     *
     * @param master 幻灯片母版
     * @return 逗号分隔的版式名称列表
     */
    private String getAvailableLayoutNames(XSLFSlideMaster master) {
        StringBuilder sb = new StringBuilder();
        for (XSLFSlideLayout layout : master.getSlideLayouts()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            String name = layout.getName();
            String type = layout.getType() != null ? layout.getType()
                                                           .name() : "UNKNOWN";
            sb.append(name != null ? name : type);
        }
        return sb.toString();
    }
}