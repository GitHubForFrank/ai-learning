package com.example.mcp.handler.tool;

import com.example.mcp.handler.BaseHandler;

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
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 二维码工具实现，提供二维码生成和解析功能。
 * 基于 Google ZXing 库，支持 PNG/JPG 格式输出。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class QrCodeHandler extends BaseHandler {

    /**
     * 确保目标目录存在
     */
    private void ensureParentDir(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    // --- 1. qr_code_generate ---

    /**
     * 生成二维码图片，将指定文本内容编码为二维码并保存为图片文件。
     *
     * @param content    要编码到二维码中的文本内容
     * @param targetPath 输出图片文件的路径
     * @param width      二维码图片宽度（像素），默认 300
     * @param height     二维码图片高度（像素），默认 300
     * @param format     输出图片格式，支持 png/jpg，默认 png
     * @return 生成结果消息
     */
    @Tool(name = "qr_code_generate", description = "生成二维码图片。将文本内容编码为二维码并保存为图片文件，支持 PNG/JPG 格式。")
    public String qrCodeGenerate(@ToolParam(description = "要编码到二维码中的文本内容") String content,
        @ToolParam(description = "输出图片文件的路径") String targetPath,
        @ToolParam(description = "二维码图片宽度（像素），默认 300", required = false) Integer width,
        @ToolParam(description = "二维码图片高度（像素），默认 300", required = false) Integer height,
        @ToolParam(description = "输出图片格式，支持 png/jpg，默认 png", required = false) String format) {
        try {
            if (content == null || content.isBlank()) {
                return "错误: 二维码内容不能为空";
            }
            if (targetPath == null || targetPath.isBlank()) {
                return "错误: 输出路径不能为空";
            }

            int w = (width != null && width > 0) ? width : 300;
            int h = (height != null && height > 0) ? height : 300;
            String fmt = (format != null && !format.isBlank()) ? format.toLowerCase()
                                                                       .trim() : "png";
            if (!"png".equals(fmt) && !"jpg".equals(fmt) && !"jpeg".equals(fmt)) {
                return "错误: 不支持的图片格式 '" + format + "'，仅支持 png / jpg";
            }

            Path output = Path.of(targetPath);
            ensureParentDir(output);

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, w, h, hints);
            MatrixToImageWriter.writeToPath(bitMatrix, fmt, output);

            LogUtil.info("二维码生成成功: {} -> {}, 尺寸: {}x{}, 格式: {}", content, targetPath, w, h, fmt);
            return String.format("二维码生成成功: %s (尺寸: %dx%d, 格式: %s, 内容: %s)", targetPath, w, h, fmt,
                                 content.length() > 50 ? content.substring(0, 50) + "..." : content);
        } catch (WriterException e) {
            LogUtil.error("qrCodeGenerate 二维码编码失败: {}", e.getMessage(), e);
            return "错误: 二维码编码失败 - " + e.getMessage();
        } catch (IOException e) {
            LogUtil.error("qrCodeGenerate 文件写入失败: {}", e.getMessage(), e);
            return "错误: 文件写入失败 - " + e.getMessage();
        } catch (Exception e) {
            LogUtil.error("qrCodeGenerate 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 2. qr_code_read ---

    /**
     * 读取并解码二维码图片，返回二维码中包含的文本内容。
     *
     * @param imagePath 二维码图片文件的路径
     * @return 解码后的文本内容
     */
    @Tool(name = "qr_code_read", description = "读取并解码二维码图片。从图片中提取二维码包含的文本内容。")
    public String qrCodeRead(@ToolParam(description = "二维码图片文件的路径") String imagePath) {
        try {
            if (imagePath == null || imagePath.isBlank()) {
                return "错误: 图片路径不能为空";
            }

            Path path = Path.of(imagePath);
            if (!Files.exists(path)) {
                return "错误: 文件不存在 - " + imagePath;
            }

            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                return "错误: 无法读取图片文件，格式可能不受支持: " + imagePath;
            }

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            Result result = new MultiFormatReader().decode(bitmap);
            String text = result.getText();

            LogUtil.info("二维码解析成功: {} -> {}", imagePath, text);
            return text;
        } catch (com.google.zxing.NotFoundException e) {
            LogUtil.error("qrCodeRead 未找到二维码: {}", e.getMessage());
            return "错误: 图片中未找到二维码 - " + imagePath;
        } catch (IOException e) {
            LogUtil.error("qrCodeRead 文件读取失败: {}", e.getMessage(), e);
            return "错误: 文件读取失败 - " + e.getMessage();
        } catch (Exception e) {
            LogUtil.error("qrCodeRead 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }
}