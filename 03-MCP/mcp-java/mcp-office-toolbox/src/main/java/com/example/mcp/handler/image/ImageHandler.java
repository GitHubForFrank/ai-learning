package com.example.mcp.handler.image;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.util.FileValidateUtil;
import com.example.mcp.util.LogUtil;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 图片处理工具实现，提供图片压缩、缩放和格式转换功能。
 * 基于 JDK 内置 ImageIO，支持 PNG / JPG / BMP / WEBP 格式。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class ImageHandler extends BaseHandler {

    private static final String[] SUPPORTED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".bmp", ".webp"};

    /**
     * 根据文件扩展名获取 ImageIO 格式名称
     */
    private String getFormatName(String fileName) {
        String name = fileName.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "jpg";
        } else if (name.endsWith(".png")) {
            return "png";
        } else if (name.endsWith(".bmp")) {
            return "bmp";
        } else if (name.endsWith(".webp")) {
            return "webp";
        }
        return "png";
    }

    /**
     * 校验图片文件是否存在且格式受支持，委托给 {@link FileValidateUtil}。
     */
    private Path validateImageFile(String fileAbsolutePath) {
        return FileValidateUtil.validateFile(fileAbsolutePath, SUPPORTED_EXTENSIONS);
    }

    /**
     * 确保目标目录存在
     */
    private void ensureParentDir(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    /**
     * 生成默认目标路径（同目录下，文件名加后缀）
     */
    private String defaultTargetPath(Path sourcePath, String suffix, String extension) {
        String sourceName = sourcePath.getFileName()
                                      .toString();
        int dotIndex = sourceName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? sourceName.substring(0, dotIndex) : sourceName;
        return sourcePath.getParent()
                         .resolve(baseName + suffix + "." + extension)
                         .toString();
    }

    // --- 1. image_compress ---

    /**
     * 压缩图片，通过调整 JPEG 编码质量实现压缩，PNG/BMP 格式通过重新编码减小体积。
     * 输出格式默认与源文件相同，也可通过 targetPath 指定。
     *
     * @param fileAbsolutePath 图片文件的绝对路径
     * @param quality          压缩质量（0.0-1.0，默认 0.7），仅对 JPEG 输出有效
     * @param targetPath       输出文件路径（可选，默认在源文件同目录生成）
     * @return 压缩结果消息
     */
    @Tool(name = "image_compress", description = "压缩图片文件。通过调整编码质量减小文件体积，支持 PNG/JPG/BMP/WEBP 格式。")
    public String imageCompress(@ToolParam(description = "图片文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "压缩质量，0.0-1.0，默认 0.7", required = false) Float quality,
        @ToolParam(description = "输出文件路径（可选，默认在源文件同目录生成）", required = false) String targetPath) {
        return execute("image_compress", () -> {
            Path sourcePath = validateImageFile(fileAbsolutePath);
            float q = (quality != null) ? Math.max(0f, Math.min(1f, quality)) : 0.7f;
            String sourceFormat = getFormatName(sourcePath.getFileName()
                                                          .toString());

            String outputPath =
                (targetPath != null && !targetPath.isBlank()) ? targetPath : defaultTargetPath(sourcePath, "_compressed", sourceFormat);
            Path output = Paths.get(outputPath);
            ensureParentDir(output);

            String outputFormat = getFormatName(output.getFileName()
                                                      .toString());
            BufferedImage image = ImageIO.read(sourcePath.toFile());
            if (image == null) {
                return "错误: 无法读取图片文件，格式可能不受支持: " + fileAbsolutePath;
            }

            // 对于 JPEG 格式，使用压缩参数
            if ("jpg".equals(outputFormat) || "jpeg".equals(outputFormat)) {
                writeJpegWithQuality(image, output.toFile(), q);
            } else {
                ImageIO.write(image, outputFormat, output.toFile());
            }

            long sourceSize = Files.size(sourcePath);
            long targetSize = Files.size(output);
            String compressionRatio = String.format("%.1f%%", (1 - (double) targetSize / sourceSize) * 100);
            LogUtil.info("图片压缩完成: {} -> {}, 原始大小: {}B, 压缩后: {}B, 压缩率: {}", fileAbsolutePath, outputPath, sourceSize, targetSize,
                         compressionRatio);
            return String.format("图片压缩成功: %s (原始: %dB, 压缩后: %dB, 压缩率: %s)", outputPath, sourceSize, targetSize, compressionRatio);
        });
    }

    /**
     * 使用 JPEG 压缩质量参数写入图片
     */
    private void writeJpegWithQuality(BufferedImage image, File output, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg")
                                    .next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    // --- 2. image_resize ---

    /**
     * 缩放图片，支持按指定宽高或按比例缩放。
     * width 和 height 为 0 表示按原始比例自动计算。
     *
     * @param fileAbsolutePath 图片文件的绝对路径
     * @param width            目标宽度（像素，0 表示按比例自动计算）
     * @param height           目标高度（像素，0 表示按比例自动计算）
     * @param targetPath       输出文件路径（可选，默认在源文件同目录生成）
     * @return 缩放结果消息
     */
    @Tool(name = "image_resize", description = "缩放图片尺寸。支持按指定宽高或按比例缩放，支持 PNG/JPG/BMP/WEBP 格式。")
    public String imageResize(@ToolParam(description = "图片文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "目标宽度（像素，0 表示按比例自动计算）") int width,
        @ToolParam(description = "目标高度（像素，0 表示按比例自动计算）") int height,
        @ToolParam(description = "输出文件路径（可选，默认在源文件同目录生成）", required = false) String targetPath) {
        return execute("image_resize", () -> {
            Path sourcePath = validateImageFile(fileAbsolutePath);
            String sourceFormat = getFormatName(sourcePath.getFileName()
                                                          .toString());

            BufferedImage originalImage = ImageIO.read(sourcePath.toFile());
            if (originalImage == null) {
                return "错误: 无法读取图片文件，格式可能不受支持: " + fileAbsolutePath;
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // 计算目标尺寸
            int targetWidth = width;
            int targetHeight = height;
            if (targetWidth == 0 && targetHeight == 0) {
                targetWidth = originalWidth;
                targetHeight = originalHeight;
            } else if (targetWidth == 0) {
                targetWidth = (int) ((double) originalWidth * targetHeight / originalHeight);
            } else if (targetHeight == 0) {
                targetHeight = (int) ((double) originalHeight * targetWidth / originalWidth);
            }

            String outputPath = (targetPath != null && !targetPath.isBlank()) ? targetPath : defaultTargetPath(sourcePath, "_resized", sourceFormat);
            Path output = Paths.get(outputPath);
            ensureParentDir(output);

            String outputFormat = getFormatName(output.getFileName()
                                                      .toString());

            // 缩放图片
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight,
                                                           originalImage.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB
                                                               : originalImage.getType());
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();

            ImageIO.write(resizedImage, outputFormat, output.toFile());
            LogUtil.info("图片缩放完成: {} -> {}, 原始尺寸: {}x{}, 目标尺寸: {}x{}", fileAbsolutePath, outputPath, originalWidth, originalHeight,
                         targetWidth, targetHeight);
            return String.format("图片缩放成功: %s (%dx%d -> %dx%d)", outputPath, originalWidth, originalHeight, targetWidth, targetHeight);
        });
    }

    // --- 3. image_convert ---

    /**
     * 图片格式转换，将图片转换为指定格式。
     *
     * @param fileAbsolutePath 图片文件的绝对路径
     * @param targetFormat     目标格式（如 "png"、"jpg"、"webp"、"bmp"）
     * @param targetPath       输出文件路径（可选，默认在源文件同目录生成）
     * @return 转换结果消息
     */
    @Tool(name = "image_convert", description = "图片格式转换。将图片转换为指定格式，支持 PNG/JPG/BMP/WEBP。")
    public String imageConvert(@ToolParam(description = "图片文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "目标格式，如 png、jpg、webp、bmp") String targetFormat,
        @ToolParam(description = "输出文件路径（可选，默认在源文件同目录生成）", required = false) String targetPath) {
        return execute("image_convert", () -> {
            Path sourcePath = validateImageFile(fileAbsolutePath);
            String format = targetFormat.toLowerCase()
                                        .trim();

            // 校验目标格式
            boolean validFormat = false;
            for (String ext : SUPPORTED_EXTENSIONS) {
                if (ext.substring(1)
                       .equals(format)) {
                    validFormat = true;
                    break;
                }
            }
            if (!validFormat) {
                return "错误: 不支持的目标格式 '" + targetFormat + "'，仅支持 png / jpg / bmp / webp";
            }

            String outputPath = (targetPath != null && !targetPath.isBlank()) ? targetPath : defaultTargetPath(sourcePath, "_converted", format);
            Path output = Paths.get(outputPath);
            ensureParentDir(output);

            BufferedImage image = ImageIO.read(sourcePath.toFile());
            if (image == null) {
                return "错误: 无法读取图片文件，格式可能不受支持: " + fileAbsolutePath;
            }

            // 处理透明通道：JPG/BMP 不支持透明，需要填充白色背景
            if (("jpg".equals(format) || "jpeg".equals(format) || "bmp".equals(format)) && image.getColorModel()
                                                                                                .hasAlpha()) {
                BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = rgbImage.createGraphics();
                g2d.setColor(java.awt.Color.WHITE);
                g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
                g2d.drawImage(image, 0, 0, null);
                g2d.dispose();
                image = rgbImage;
            }

            boolean result = ImageIO.write(image, format, output.toFile());
            if (!result) {
                return "错误: 无法将图片转换为 " + format + " 格式，该格式可能不被当前 JDK 的 ImageIO 支持";
            }

            LogUtil.info("图片格式转换完成: {} -> {}, 目标格式: {}", fileAbsolutePath, outputPath, format);
            return String.format("图片格式转换成功: %s (格式: %s)", outputPath, format);
        });
    }
}