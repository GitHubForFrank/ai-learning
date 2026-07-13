package com.example.mcp.handler.tool;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.pojo.barcode.BarcodeGenerateRequest;
import com.example.mcp.util.LogUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 条形码工具实现，提供一维条形码（EAN-13、Code128、CODE39）的生成和解析功能。
 * 基于 Google ZXing 库，支持 PNG 格式输出。
 * 与 QrCodeHandler（二维码）互补，专注于一维条形码处理。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class BarcodeHandler extends BaseHandler {

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
     * 校验条形码内容是否符合格式要求
     */
    private String validateBarcodeContent(String content, BarcodeFormat format) {
        if (content == null || content.isBlank()) {
            return "错误: 条形码内容不能为空";
        }
        switch (format) {
            case EAN_13:
                if (!content.matches("\\d{12,13}")) {
                    return "错误: EAN-13 条形码内容必须是 12 或 13 位数字，当前输入: " + content;
                }
                break;
            case CODE_128:
                // Code128 支持所有 ASCII 字符
                if (content.length() > 80) {
                    LogUtil.warn("barcodeGenerate Code128 内容较长({}字符)，可能影响识别率", content.length());
                }
                break;
            case CODE_39:
                if (!content.matches("[A-Z0-9\\-. $/+%*]+")) {
                    return "错误: CODE39 仅支持大写字母 A-Z、数字 0-9 和特殊字符 - . $ / + % 空格";
                }
                break;
            default:
                break;
        }
        return null;
    }

    /**
     * 解析条形码类型字符串
     */
    private BarcodeFormat parseBarcodeType(String type) {
        if (type == null || type.isBlank()) {
            return BarcodeFormat.CODE_128;
        }
        return switch (type.toUpperCase()
                           .trim()) {
            case "EAN-13", "EAN13", "EAN_13" -> BarcodeFormat.EAN_13;
            case "CODE128", "CODE_128", "CODE-128" -> BarcodeFormat.CODE_128;
            case "CODE39", "CODE_39", "CODE-39" -> BarcodeFormat.CODE_39;
            default -> {
                LogUtil.warn("parseBarcodeType 未知类型: {}，使用默认 Code128", type);
                yield BarcodeFormat.CODE_128;
            }
        };
    }

    /**
     * 获取条形码类型的中文显示名称
     */
    private String getBarcodeTypeDisplayName(BarcodeFormat format) {
        return switch (format) {
            case EAN_13 -> "EAN-13";
            case CODE_128 -> "Code128";
            case CODE_39 -> "CODE39";
            default -> format.name();
        };
    }

    // --- 1. barcode_generate ---

    /**
     * 生成一维条形码图片，支持 EAN-13、Code128、CODE39 三种编码格式。
     * 输出为 PNG 格式图片，条形码下方显示编码文本。
     *
     * @param content    要编码到条形码中的内容
     * @param targetPath 输出图片文件的绝对路径（.png）
     * @param type       条形码类型: EAN-13, Code128, CODE39（默认 Code128）
     * @param width      条形码图片宽度（像素），默认 400
     * @param height     条形码图片高度（像素），默认 150
     * @param showText   是否在条形码下方显示文本内容（默认 true）
     * @return 生成结果消息
     */
    @Tool(name = "barcode_generate", description = "生成一维条形码图片（支持 EAN-13、Code128、CODE39），输出为 PNG 格式。")
    public String barcodeGenerate(BarcodeGenerateRequest request) {
        try {
            if (request.content() == null || request.content()
                                                    .isBlank()) {
                return "错误: 条形码内容不能为空";
            }
            if (request.targetPath() == null || request.targetPath()
                                                       .isBlank()) {
                return "错误: 输出路径不能为空";
            }

            BarcodeFormat format = parseBarcodeType(request.type());
            String typeDisplay = getBarcodeTypeDisplayName(format);

            // 校验内容格式
            String validationError = validateBarcodeContent(request.content(), format);
            if (validationError != null) {
                return validationError;
            }

            int w = (request.width() != null && request.width() > 0) ? request.width() : 400;
            int h = (request.height() != null && request.height() > 0) ? request.height() : 150;
            boolean showLabel = request.showText() == null || request.showText();

            Path output = Paths.get(request.targetPath());
            ensureParentDir(output);

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(request.content(), format, w, h, hints);

            // 生成包含文本标签的图片
            BufferedImage image;
            if (showLabel) {
                image = createBarcodeImageWithText(bitMatrix, request.content(), w, h);
            } else {
                image = MatrixToImageWriter.toBufferedImage(bitMatrix);
            }

            ImageIO.write(image, "PNG", output.toFile());

            LogUtil.info("barcodeGenerate 完成，类型: {}，内容: {}，输出: {}，尺寸: {}x{}", typeDisplay, request.content(), request.targetPath(), w, h);
            return String.format("条形码生成成功: %s（类型: %s，尺寸: %dx%d，内容: %s）", request.targetPath(), typeDisplay, w, h, request.content()
                                                                                                                                      .length() > 50 ?
                request.content()
                       .substring(0, 50) + "..." : request.content());
        } catch (WriterException e) {
            LogUtil.error("barcodeGenerate 条形码编码失败: {}", e.getMessage(), e);
            return "错误: 条形码编码失败 - " + e.getMessage() + "。请检查内容是否符合所选格式的要求。";
        } catch (IOException e) {
            LogUtil.error("barcodeGenerate 文件写入失败: {}", e.getMessage(), e);
            return "错误: 文件写入失败 - " + e.getMessage();
        } catch (Exception e) {
            LogUtil.error("barcodeGenerate 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    /**
     * 创建带文本标签的条形码图片
     */
    private BufferedImage createBarcodeImageWithText(BitMatrix bitMatrix, String text, int width, int height) {
        int textHeight = 30;
        int totalHeight = height + textHeight;
        BufferedImage image = new BufferedImage(width, totalHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = image.createGraphics();
        try {
            // 白色背景
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, totalHeight);

            // 绘制条形码
            BufferedImage barcodeImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            g2d.drawImage(barcodeImage, 0, 0, width, height, null);

            // 绘制文本
            g2d.setColor(Color.BLACK);
            Font font = new Font("Arial", Font.PLAIN, 14);
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int x = (width - textWidth) / 2;
            int y = height + (textHeight + fm.getAscent()) / 2 - 2;
            g2d.drawString(text, x, y);
        } finally {
            g2d.dispose();
        }
        return image;
    }

    // --- 2. barcode_read ---

    /**
     * 读取并解析条形码图片中的内容，支持一维条形码和二维码的自动识别。
     *
     * @param imagePath 条形码图片文件的绝对路径
     * @return 解码后的条形码内容
     */
    @Tool(name = "barcode_read", description = "读取并解析条形码图片中的内容。自动识别一维条形码和二维码。")
    public String barcodeRead(@ToolParam(description = "条形码图片文件的绝对路径") String imagePath) {
        try {
            if (imagePath == null || imagePath.isBlank()) {
                return "错误: 图片路径不能为空";
            }

            Path path = Paths.get(imagePath);
            if (!Files.exists(path)) {
                return "错误: 文件不存在 - " + imagePath;
            }

            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                return "错误: 无法读取图片文件，格式可能不受支持: " + imagePath;
            }

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));

            // 尝试多种解码方式
            Map<com.google.zxing.DecodeHintType, Object> hints = new HashMap<>();
            hints.put(com.google.zxing.DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(com.google.zxing.DecodeHintType.POSSIBLE_FORMATS,
                      java.util.List.of(BarcodeFormat.CODE_128, BarcodeFormat.EAN_13, BarcodeFormat.CODE_39, BarcodeFormat.QR_CODE,
                                        BarcodeFormat.EAN_8, BarcodeFormat.UPC_A, BarcodeFormat.ITF, BarcodeFormat.PDF_417,
                                        BarcodeFormat.DATA_MATRIX));

            MultiFormatReader reader = new MultiFormatReader();
            reader.setHints(hints);
            Result result = reader.decode(bitmap);
            String text = result.getText();
            String formatName = result.getBarcodeFormat()
                                      .name();

            LogUtil.info("barcodeRead 解析成功: {} -> {} (格式: {})", imagePath, text, formatName);
            return String.format("条形码解析结果:\n格式: %s\n内容: %s", formatName, text);
        } catch (com.google.zxing.NotFoundException e) {
            LogUtil.error("barcodeRead 未找到条形码: {}", e.getMessage());
            return "错误: 图片中未找到可识别的条形码或二维码 - " + imagePath;
        } catch (IOException e) {
            LogUtil.error("barcodeRead 文件读取失败: {}", e.getMessage(), e);
            return "错误: 文件读取失败 - " + e.getMessage();
        } catch (Exception e) {
            LogUtil.error("barcodeRead 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }
}
