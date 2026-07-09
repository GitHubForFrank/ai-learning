package com.example.mcp.handler;

import com.example.mcp.util.FileValidateUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class PptHandler {

    private static final Logger log = LoggerFactory.getLogger(PptHandler.class);

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
    public String createPpt(@ToolParam(description = "Absolute path for the new PPT file") String fileAbsolutePath) {
        try {
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
        } catch (Exception e) {
            log.error("createPpt 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 2. add_ppt_slide ---

    /**
     * 新增幻灯片页面
     */
    @Tool(name = "add_ppt_slide", description = "向 PowerPoint 演示文稿添加新幻灯片。可选指定幻灯片标题。")
    public String addPptSlide(@ToolParam(description = "Absolute path to the PPT file") String fileAbsolutePath,
        @ToolParam(description = "Title text for the new slide (optional)", required = false) String title) {
        try {
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
        } catch (Exception e) {
            log.error("addPptSlide 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 3. delete_ppt_slide ---

    /**
     * 删除指定幻灯片
     */
    @Tool(name = "delete_ppt_slide", description = "从 PPT 演示文稿中删除指定幻灯片。幻灯片索引从 0 开始。")
    public String deletePptSlide(@ToolParam(description = "Absolute path to the PPT file") String fileAbsolutePath,
        @ToolParam(description = "Slide index to delete (0-based)") int slideIndex) {
        try {
            Path path = validatePptxFile(fileAbsolutePath);
            try (XMLSlideShow slideShow = openSlideShow(path)) {
                List<XSLFSlide> slides = slideShow.getSlides();
                if (slideIndex < 0 || slideIndex >= slides.size()) {
                    return String.format("错误：幻灯片索引 %d 超出范围，共 %d 张幻灯片", slideIndex, slides.size());
                }
                slideShow.removeSlide(slideIndex);
                saveSlideShow(slideShow, path);
                return "已删除第 " + (slideIndex + 1) + " 张幻灯片: " + fileAbsolutePath;
            }
        } catch (Exception e) {
            log.error("deletePptSlide 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 4. read_ppt_text ---

    /**
     * 读取每页幻灯片内全部文本内容
     */
    @Tool(name = "read_ppt_text", description = "读取 PPT 演示文稿中所有幻灯片的文本内容。按幻灯片组织返回文本。")
    public String readPptText(@ToolParam(description = "Absolute path to the PPT file") String fileAbsolutePath) {
        try {
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
        } catch (Exception e) {
            log.error("readPptText 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 5. modify_ppt_slide_text ---

    /**
     * 修改幻灯片页面文字内容
     */
    @Tool(name = "modify_ppt_slide_text", description = "修改 PPT 演示文稿指定幻灯片上的文本内容。在指定幻灯片上将所有旧文本替换为新文本。")
    public String modifyPptSlideText(@ToolParam(description = "Absolute path to the PPT file") String fileAbsolutePath,
        @ToolParam(description = "Slide index to modify (0-based)") int slideIndex, @ToolParam(description = "Text to be replaced") String oldText,
        @ToolParam(description = "New text to replace with") String newText) {
        try {
            Path path = validatePptxFile(fileAbsolutePath);
            try (XMLSlideShow slideShow = openSlideShow(path)) {
                List<XSLFSlide> slides = slideShow.getSlides();
                if (slideIndex < 0 || slideIndex >= slides.size()) {
                    return String.format("错误：幻灯片索引 %d 超出范围，共 %d 张幻灯片", slideIndex, slides.size());
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
        } catch (Exception e) {
            log.error("modifyPptSlideText 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 6. save_ppt ---

    /**
     * PPT 文件保存（另存为）
     */
    @Tool(name = "save_ppt_as", description = "将 PPT 演示文稿另存为新文件。将原始演示文稿复制到新位置。")
    public String savePptAs(@ToolParam(description = "Absolute path to the source PPT file") String sourcePath,
        @ToolParam(description = "Absolute path for the new PPT file") String targetPath) {
        try {
            Path source = validatePptxFile(sourcePath);
            Path target = Paths.get(targetPath);
            // 确保目标父目录存在
            Path targetParent = target.getParent();
            if (targetParent != null && !Files.exists(targetParent)) {
                Files.createDirectories(targetParent);
            }
            Files.copy(source, target);
            return "PPT 文件已另存为: " + targetPath;
        } catch (Exception e) {
            log.error("savePptAs 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }
}