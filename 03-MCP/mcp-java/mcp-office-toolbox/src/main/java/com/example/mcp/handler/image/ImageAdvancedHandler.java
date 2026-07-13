package com.example.mcp.handler.image;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.pojo.image.ImageCropRequest;
import com.example.mcp.pojo.image.ImageTextRequest;
import com.example.mcp.pojo.image.ImageWatermarkRequest;
import com.example.mcp.util.LogUtil;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 高级图片处理工具实现，提供裁剪、旋转、水印、信息获取、拼接、文字标注和 Base64 编解码功能。
 * 基于 JDK 21 内置 javax.imageio 和 java.awt，支持 PNG / JPG / BMP / WEBP 格式。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class ImageAdvancedHandler extends BaseHandler {

    private static final String[] SUPPORTED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".bmp", ".webp"};

    /**
     * 校验图片文件是否存在且格式受支持
     *
     * @param fileAbsolutePath 图片文件绝对路径
     * @return 解析后的 Path 对象
     * @throws IllegalArgumentException 文件不存在或格式不支持时抛出
     */
    private Path validateImageFile(String fileAbsolutePath) {
        Path path = Paths.get(fileAbsolutePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("文件不存在: " + fileAbsolutePath);
        }
        String name = path.getFileName()
                          .toString()
                          .toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return path;
            }
        }
        throw new IllegalArgumentException("不支持的图片格式，仅支持 PNG/JPG/BMP/WEBP: " + fileAbsolutePath);
    }

    /**
     * 根据文件扩展名获取 ImageIO 格式名称
     *
     * @param fileName 文件名
     * @return ImageIO 格式名称
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
     * 确保目标文件父目录存在
     *
     * @param path 目标路径
     * @throws IOException 创建目录失败时抛出
     */
    private void ensureParentDir(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    /**
     * 生成默认目标路径（同目录下，文件名加后缀）
     *
     * @param sourcePath 源文件路径
     * @param suffix     文件名后缀
     * @param extension  文件扩展名
     * @return 默认目标路径字符串
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

    // --- 1. image_crop ---

    /**
     * 裁剪图片，指定矩形区域裁剪。
     *
     * @param fileAbsolutePath 图片文件的绝对路径
     * @param x                裁剪区域起始 X 坐标（像素）
     * @param y                裁剪区域起始 Y 坐标（像素）
     * @param width            裁剪区域宽度（像素）
     * @param height           裁剪区域高度（像素）
     * @param targetPath       输出文件路径（可选，默认在源文件同目录生成）
     * @return 裁剪结果消息
     */
    @Tool(name = "image_crop", description = "裁剪图片。指定 x、y、width、height 裁剪矩形区域，支持 PNG/JPG/BMP/WEBP 格式。")
    public String imageCrop(@ToolParam(description = "图片裁剪请求参数") ImageCropRequest request) {
        return execute("image_crop", () -> {
            Path sourcePath = validateImageFile(request.fileAbsolutePath());
            String sourceFormat = getFormatName(sourcePath.getFileName()
                                                          .toString());

            BufferedImage original = ImageIO.read(sourcePath.toFile());
            if (original == null) {
                return "错误: 无法读取图片文件，格式可能不受支持: " + request.fileAbsolutePath();
            }

            int originalWidth = original.getWidth();
            int originalHeight = original.getHeight();

            // 参数校验
            if (request.x() < 0 || request.y() < 0 || request.width() <= 0 || request.height() <= 0) {
                return "错误: 裁剪参数无效，x、y 必须 >= 0，width、height 必须 > 0";
            }
            if (request.x() + request.width() > originalWidth || request.y() + request.height() > originalHeight) {
                return String.format("错误: 裁剪区域超出图片范围，图片尺寸: %dx%d，裁剪区域: (%d,%d,%d,%d)", originalWidth, originalHeight, request.x(),
                                     request.y(), request.width(), request.height());
            }

            BufferedImage cropped = original.getSubimage(request.x(), request.y(), request.width(), request.height());

            String outputPath = (request.targetPath() != null && !request.targetPath()
                                                                         .isBlank()) ? request.targetPath()
                : defaultTargetPath(sourcePath, "_cropped", sourceFormat);
            Path output = Paths.get(outputPath);
            ensureParentDir(output);

            String outputFormat = getFormatName(output.getFileName()
                                                      .toString());
            ImageIO.write(cropped, outputFormat, output.toFile());

            LogUtil.info("图片裁剪完成: {} -> {}, 裁剪区域: ({},{},{},{})", request.fileAbsolutePath(), outputPath, request.x(), request.y(),
                         request.width(), request.height());
            return String.format("图片裁剪成功: %s (%dx%d -> %dx%d)", outputPath, originalWidth, originalHeight, request.width(), request.height());
        });
    }

    // --- 2. image_rotate ---

    /**
     * 旋转图片，支持 90/180/270 度旋转。
     *
     * @param fileAbsolutePath 图片文件的绝对路径
     * @param angle            旋转角度，支持 90/180/270
     * @param targetPath       输出文件路径（可选，默认在源文件同目录生成）
     * @return 旋转结果消息
     */
    @Tool(name = "image_rotate", description = "旋转图片。支持 90 度、180 度、270 度旋转，支持 PNG/JPG/BMP/WEBP 格式。")
    public String imageRotate(@ToolParam(description = "图片文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "旋转角度，支持 90/180/270") int angle,
        @ToolParam(description = "输出文件路径（可选，默认在源文件同目录生成）", required = false) String targetPath) {
        return execute("image_rotate", () -> {
            if (angle != 90 && angle != 180 && angle != 270) {
                return "错误: 旋转角度必须是 90、180 或 270，当前值: " + angle;
            }

            Path sourcePath = validateImageFile(fileAbsolutePath);
            String sourceFormat = getFormatName(sourcePath.getFileName()
                                                          .toString());

            BufferedImage original = ImageIO.read(sourcePath.toFile());
            if (original == null) {
                return "错误: 无法读取图片文件，格式可能不受支持: " + fileAbsolutePath;
            }

            int originalWidth = original.getWidth();
            int originalHeight = original.getHeight();

            // 计算旋转后的尺寸
            int newWidth = (angle == 180) ? originalWidth : originalHeight;
            int newHeight = (angle == 180) ? originalHeight : originalWidth;

            BufferedImage rotated = new BufferedImage(newWidth, newHeight,
                                                      original.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB
                                                          : original.getType());
            Graphics2D g2d = rotated.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // 设置旋转变换
            double radians = Math.toRadians(angle);
            if (angle == 90) {
                g2d.translate(newWidth, 0);
            } else if (angle == 180) {
                g2d.translate(newWidth, newHeight);
            } else {
                g2d.translate(0, newHeight);
            }
            g2d.rotate(radians);
            g2d.drawImage(original, 0, 0, null);
            g2d.dispose();

            String outputPath =
                (targetPath != null && !targetPath.isBlank()) ? targetPath : defaultTargetPath(sourcePath, "_rotated_" + angle, sourceFormat);
            Path output = Paths.get(outputPath);
            ensureParentDir(output);

            String outputFormat = getFormatName(output.getFileName()
                                                      .toString());
            ImageIO.write(rotated, outputFormat, output.toFile());

            LogUtil.info("图片旋转完成: {} -> {}, 角度: {}°", fileAbsolutePath, outputPath, angle);
            return String.format("图片旋转成功: %s (旋转 %d°, %dx%d -> %dx%d)", outputPath, angle, originalWidth, originalHeight, newWidth,
                                 newHeight);
        });
    }

    // --- 3. image_watermark ---

    /**
     * 为图片添加文字水印，支持指定位置、透明度、字体大小。
     *
     * @param fileAbsolutePath 图片文件的绝对路径
     * @param text             水印文字
     * @param position         水印位置（topLeft/topCenter/topRight/centerLeft/center/centerRight/bottomLeft/bottomCenter/bottomRight）
     * @param opacity          透明度（0.0-1.0，默认 0.5）
     * @param fontSize         字体大小（默认 36）
     * @param targetPath       输出文件路径（可选，默认在源文件同目录生成）
     * @return 水印结果消息
     */
    @Tool(name = "image_watermark", description = "为图片添加文字水印。支持自定义位置、透明度和字体大小，支持 PNG/JPG/BMP/WEBP 格式。")
    public String imageWatermark(@ToolParam(description = "图片水印请求参数") ImageWatermarkRequest request) {
        return execute("image_watermark", () -> {
            Path sourcePath = validateImageFile(request.fileAbsolutePath());
            String sourceFormat = getFormatName(sourcePath.getFileName()
                                                          .toString());

            BufferedImage original = ImageIO.read(sourcePath.toFile());
            if (original == null) {
                return "错误: 无法读取图片文件，格式可能不受支持: " + request.fileAbsolutePath();
            }

            float alpha = (request.opacity() != null) ? Math.max(0f, Math.min(1f, request.opacity())) : 0.5f;
            int fs = (request.fontSize() != null && request.fontSize() > 0) ? request.fontSize() : 36;
            String pos = (request.position() != null && !request.position()
                                                                .isBlank()) ? request.position()
                                                                                     .toLowerCase() : "center";

            int imgWidth = original.getWidth();
            int imgHeight = original.getHeight();

            BufferedImage watermarked = new BufferedImage(imgWidth, imgHeight,
                                                          original.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB
                                                              : original.getType());
            Graphics2D g2d = watermarked.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 先绘制原图
            g2d.drawImage(original, 0, 0, null);

            // 设置水印样式
            g2d.setFont(new Font("Microsoft YaHei", Font.BOLD, fs));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(request.text());
            int textHeight = fm.getHeight();

            // 计算水印位置
            int margin = 20;
            int x, y;
            switch (pos) {
                case "topleft":
                    x = margin;
                    y = textHeight + margin;
                    break;
                case "topcenter":
                    x = (imgWidth - textWidth) / 2;
                    y = textHeight + margin;
                    break;
                case "topright":
                    x = imgWidth - textWidth - margin;
                    y = textHeight + margin;
                    break;
                case "centerleft":
                    x = margin;
                    y = (imgHeight + textHeight) / 2;
                    break;
                case "center":
                    x = (imgWidth - textWidth) / 2;
                    y = (imgHeight + textHeight) / 2;
                    break;
                case "centerright":
                    x = imgWidth - textWidth - margin;
                    y = (imgHeight + textHeight) / 2;
                    break;
                case "bottomleft":
                    x = margin;
                    y = imgHeight - margin;
                    break;
                case "bottomcenter":
                    x = (imgWidth - textWidth) / 2;
                    y = imgHeight - margin;
                    break;
                case "bottomright":
                    x = imgWidth - textWidth - margin;
                    y = imgHeight - margin;
                    break;
                default:
                    x = (imgWidth - textWidth) / 2;
                    y = (imgHeight + textHeight) / 2;
                    break;
            }

            // 设置透明度
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.setColor(new Color(255, 255, 255, (int) (alpha * 255)));
            g2d.drawString(request.text(), x, y);
            g2d.dispose();

            String outputPath = (request.targetPath() != null && !request.targetPath()
                                                                         .isBlank()) ? request.targetPath()
                : defaultTargetPath(sourcePath, "_watermark", sourceFormat);
            Path output = Paths.get(outputPath);
            ensureParentDir(output);

            String outputFormat = getFormatName(output.getFileName()
                                                      .toString());
            ImageIO.write(watermarked, outputFormat, output.toFile());

            LogUtil.info("图片水印添加完成: {} -> {}, 文字: {}, 位置: {}", request.fileAbsolutePath(), outputPath, request.text(), pos);
            return String.format("图片水印添加成功: %s (水印: \"%s\", 位置: %s)", outputPath, request.text(), pos);
        });
    }

    // --- 4. image_info ---

    /**
     * 获取图片元信息，包括宽度、高度、格式、文件大小、DPI 等。
     *
     * @param fileAbsolutePath 图片文件的绝对路径
     * @return 图片元信息
     */
    @Tool(name = "image_info", description = "获取图片元信息。包括宽度、高度、格式、文件大小、DPI 等，支持 PNG/JPG/BMP/WEBP 格式。")
    public String imageInfo(@ToolParam(description = "图片文件的绝对路径") String fileAbsolutePath) {
        return execute("image_info", () -> {
            Path path = validateImageFile(fileAbsolutePath);
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                return "错误: 无法读取图片文件，格式可能不受支持: " + fileAbsolutePath;
            }

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("文件路径", fileAbsolutePath);
            info.put("文件名", path.getFileName()
                                   .toString());
            info.put("文件大小", Files.size(path) + " 字节");
            info.put("格式", getFormatName(path.getFileName()
                                               .toString()).toUpperCase());
            info.put("宽度", image.getWidth() + " 像素");
            info.put("高度", image.getHeight() + " 像素");
            info.put("颜色模型", image.getColorModel()
                                      .toString());
            info.put("色彩类型", getColorTypeName(image.getType()));
            info.put("是否含透明通道", image.getColorModel()
                                            .hasAlpha());

            StringBuilder sb = new StringBuilder();
            sb.append("图片元信息：\n");
            for (Map.Entry<String, Object> entry : info.entrySet()) {
                sb.append("  ")
                  .append(entry.getKey())
                  .append(": ")
                  .append(entry.getValue())
                  .append("\n");
            }

            LogUtil.info("获取图片信息完成: {}", fileAbsolutePath);
            return sb.toString()
                     .trim();
        });
    }

    /**
     * 获取 BufferedImage 类型名称
     *
     * @param type BufferedImage 类型常量
     * @return 类型名称
     */
    private String getColorTypeName(int type) {
        return switch (type) {
            case BufferedImage.TYPE_INT_RGB -> "RGB";
            case BufferedImage.TYPE_INT_ARGB -> "ARGB (含透明通道)";
            case BufferedImage.TYPE_INT_BGR -> "BGR";
            case BufferedImage.TYPE_3BYTE_BGR -> "3字节 BGR";
            case BufferedImage.TYPE_4BYTE_ABGR -> "4字节 ABGR";
            case BufferedImage.TYPE_BYTE_GRAY -> "灰度";
            case BufferedImage.TYPE_BYTE_BINARY -> "二值";
            case BufferedImage.TYPE_USHORT_GRAY -> "16位灰度";
            default -> "其他 (" + type + ")";
        };
    }

    // --- 5. image_concat ---

    /**
     * 拼接多张图片，支持水平或垂直拼接。
     *
     * @param sourceFilePaths 图片文件绝对路径列表，逗号分隔
     * @param direction       拼接方向（horizontal/vertical）
     * @param targetPath      输出文件路径
     * @return 拼接结果消息
     */
    @Tool(name = "image_concat", description = "拼接多张图片。支持水平拼接或垂直拼接，支持 PNG/JPG/BMP/WEBP 格式。")
    public String imageConcat(@ToolParam(description = "图片文件绝对路径列表，逗号分隔") String sourceFilePaths,
        @ToolParam(description = "拼接方向，horizontal（水平）或 vertical（垂直）") String direction,
        @ToolParam(description = "输出文件路径") String targetPath) {
        return execute("image_concat", () -> {
            String[] paths = sourceFilePaths.split(",");
            if (paths.length < 2) {
                return "错误: 至少需要两张图片才能拼接";
            }

            boolean isHorizontal = "horizontal".equalsIgnoreCase(direction != null ? direction.trim() : "horizontal");
            if (!isHorizontal && !"vertical".equalsIgnoreCase(direction.trim())) {
                return "错误: 拼接方向必须是 horizontal 或 vertical，当前值: " + direction;
            }

            List<BufferedImage> images = new ArrayList<>();
            for (String p : paths) {
                String trimmed = p.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                Path imgPath = validateImageFile(trimmed);
                BufferedImage img = ImageIO.read(imgPath.toFile());
                if (img == null) {
                    return "错误: 无法读取图片文件: " + trimmed;
                }
                images.add(img);
            }

            if (images.size() < 2) {
                return "错误: 至少需要两张有效图片才能拼接";
            }

            // 计算拼接后尺寸
            int totalWidth, totalHeight;
            if (isHorizontal) {
                totalWidth = images.stream()
                                   .mapToInt(BufferedImage::getWidth)
                                   .sum();
                totalHeight = images.stream()
                                    .mapToInt(BufferedImage::getHeight)
                                    .max()
                                    .orElse(0);
            } else {
                totalWidth = images.stream()
                                   .mapToInt(BufferedImage::getWidth)
                                   .max()
                                   .orElse(0);
                totalHeight = images.stream()
                                    .mapToInt(BufferedImage::getHeight)
                                    .sum();
            }

            BufferedImage result = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = result.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int offset = 0;
            for (BufferedImage img : images) {
                if (isHorizontal) {
                    int y = (totalHeight - img.getHeight()) / 2; // 垂直居中
                    g2d.drawImage(img, offset, y, null);
                    offset += img.getWidth();
                } else {
                    int x = (totalWidth - img.getWidth()) / 2; // 水平居中
                    g2d.drawImage(img, x, offset, null);
                    offset += img.getHeight();
                }
            }
            g2d.dispose();

            Path output = Paths.get(targetPath);
            ensureParentDir(output);
            String outputFormat = getFormatName(output.getFileName()
                                                      .toString());
            ImageIO.write(result, outputFormat, output.toFile());

            LogUtil.info("图片拼接完成: {} 张图片 -> {}, 方向: {}", images.size(), targetPath, isHorizontal ? "水平" : "垂直");
            return String.format("图片拼接成功: %s (%d 张图片, %s拼接, %dx%d)", targetPath, images.size(), isHorizontal ? "水平" : "垂直", totalWidth,
                                 totalHeight);
        });
    }

    // --- 6. image_add_text ---

    /**
     * 在图片上添加文字标注。
     *
     * @param fileAbsolutePath 图片文件的绝对路径
     * @param text             要添加的文字
     * @param x                文字起始 X 坐标（像素）
     * @param y                文字起始 Y 坐标（像素）
     * @param fontSize         字体大小（默认 24）
     * @param color            文字颜色（如 red/black/white/blue，默认 black）
     * @param targetPath       输出文件路径（可选，默认在源文件同目录生成）
     * @return 添加文字结果消息
     */
    @Tool(name = "image_add_text", description = "在图片上添加文字标注。支持自定义位置、字体大小和颜色，支持 PNG/JPG/BMP/WEBP 格式。")
    public String imageAddText(@ToolParam(description = "图片添加文字请求参数") ImageTextRequest request) {
        return execute("image_add_text", () -> {
            Path sourcePath = validateImageFile(request.fileAbsolutePath());
            String sourceFormat = getFormatName(sourcePath.getFileName()
                                                          .toString());

            BufferedImage original = ImageIO.read(sourcePath.toFile());
            if (original == null) {
                return "错误: 无法读取图片文件，格式可能不受支持: " + request.fileAbsolutePath();
            }

            int fs = (request.fontSize() != null && request.fontSize() > 0) ? request.fontSize() : 24;
            Color textColor = parseColor(request.color());

            BufferedImage result = new BufferedImage(original.getWidth(), original.getHeight(),
                                                     original.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB
                                                         : original.getType());
            Graphics2D g2d = result.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 先绘制原图
            g2d.drawImage(original, 0, 0, null);

            // 绘制文字
            g2d.setFont(new Font("Microsoft YaHei", Font.PLAIN, fs));
            g2d.setColor(textColor);
            g2d.drawString(request.text(), request.x(), request.y());
            g2d.dispose();

            String outputPath = (request.targetPath() != null && !request.targetPath()
                                                                         .isBlank()) ? request.targetPath()
                : defaultTargetPath(sourcePath, "_text", sourceFormat);
            Path output = Paths.get(outputPath);
            ensureParentDir(output);

            String outputFormat = getFormatName(output.getFileName()
                                                      .toString());
            ImageIO.write(result, outputFormat, output.toFile());

            LogUtil.info("图片文字标注完成: {} -> {}, 文字: {}, 位置: ({},{})", request.fileAbsolutePath(), outputPath, request.text(), request.x(),
                         request.y());
            return String.format("图片文字标注成功: %s (文字: \"%s\", 位置: (%d,%d))", outputPath, request.text(), request.x(), request.y());
        });
    }

    /**
     * 解析颜色名称字符串为 Color 对象
     *
     * @param colorName 颜色名称
     * @return Color 对象
     */
    private Color parseColor(String colorName) {
        if (colorName == null || colorName.isBlank()) {
            return Color.BLACK;
        }
        return switch (colorName.toLowerCase()
                                .trim()) {
            case "red" -> Color.RED;
            case "white" -> Color.WHITE;
            case "blue" -> Color.BLUE;
            case "green" -> Color.GREEN;
            case "yellow" -> Color.YELLOW;
            case "orange" -> Color.ORANGE;
            case "gray", "grey" -> Color.GRAY;
            case "cyan" -> Color.CYAN;
            case "magenta" -> Color.MAGENTA;
            case "pink" -> Color.PINK;
            default -> Color.BLACK;
        };
    }

    // --- 7. image_to_base64 ---

    /**
     * 将图片文件转换为 Base64 编码字符串。
     *
     * @param fileAbsolutePath 图片文件的绝对路径
     * @param includeDataUri   是否包含 data URI 前缀（默认 true），如 "data:image/png;base64,..."
     * @return Base64 编码字符串
     */
    @Tool(name = "image_to_base64", description = "图片文件转 Base64 编码字符串。可选择是否包含 data URI 前缀，支持 PNG/JPG/BMP/WEBP 格式。")
    public String imageToBase64(@ToolParam(description = "图片文件的绝对路径") String fileAbsolutePath,
        @ToolParam(description = "是否包含 data URI 前缀，默认 true", required = false) Boolean includeDataUri) {
        return execute("image_to_base64", () -> {
            Path path = validateImageFile(fileAbsolutePath);
            byte[] bytes = Files.readAllBytes(path);
            String base64 = Base64.getEncoder()
                                  .encodeToString(bytes);

            boolean withPrefix = (includeDataUri == null || includeDataUri);
            String format = getFormatName(path.getFileName()
                                              .toString());
            // WEBP 的 MIME 类型
            String mimeType = "webp".equals(format) ? "image/webp" : "image/" + format;

            LogUtil.info("图片转 Base64 完成: {}, 大小: {} 字节", fileAbsolutePath, bytes.length);

            if (withPrefix) {
                return "data:" + mimeType + ";base64," + base64;
            }
            return base64;
        });
    }

    // --- 8. base64_to_image ---

    /**
     * 将 Base64 编码字符串转换为图片文件。
     *
     * @param base64String Base64 编码字符串（可包含 data URI 前缀）
     * @param targetPath   输出图片文件路径
     * @return 转换结果消息
     */
    @Tool(name = "base64_to_image", description = "Base64 编码字符串转图片文件。支持带或不带 data URI 前缀的 Base64 字符串。")
    public String base64ToImage(@ToolParam(description = "Base64 编码字符串（可包含 data URI 前缀）") String base64String,
        @ToolParam(description = "输出图片文件路径") String targetPath) {
        return execute("base64_to_image", () -> {
            if (base64String == null || base64String.isBlank()) {
                return "错误: Base64 字符串不能为空";
            }

            String pureBase64 = base64String;
            // 去除可能的 data URI 前缀
            if (base64String.contains(",")) {
                String[] parts = base64String.split(",", 2);
                if (parts.length == 2) {
                    pureBase64 = parts[1].trim();
                }
            }

            byte[] bytes;
            try {
                bytes = Base64.getDecoder()
                              .decode(pureBase64);
            } catch (IllegalArgumentException e) {
                return "错误: Base64 解码失败，字符串格式不正确: " + e.getMessage();
            }

            Path output = Paths.get(targetPath);
            ensureParentDir(output);
            Files.write(output, bytes);

            LogUtil.info("Base64 转图片完成: {}, 大小: {} 字节", targetPath, bytes.length);
            return "Base64 转图片成功: " + targetPath + " (" + bytes.length + " 字节)";
        });
    }
}
