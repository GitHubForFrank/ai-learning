package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.image.ImageHandler;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ImageHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class ImageHandlerTest {

    @TempDir
    Path tempDir;

    @Autowired
    private ImageHandler imageHandler;

    private Path pngFile;
    private Path nonImageFile;

    @BeforeEach
    void setUp() throws Exception {
        // 创建测试 PNG 图片
        BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        pngFile = tempDir.resolve("test.png");
        ImageIO.write(image, "png", pngFile.toFile());

        // 创建非图片文件
        nonImageFile = tempDir.resolve("test.txt");
        Files.writeString(nonImageFile, "This is not an image");
    }

    // ======================== image_compress ========================

    @Test
    void testImageCompressWithValidPng() {
        String result = imageHandler.imageCompress(pngFile.toString(), 0.5f, null);
        assertTrue(result.contains("图片压缩成功"));
        assertTrue(result.contains("原始:"));
        assertTrue(result.contains("压缩后:"));
        assertTrue(result.contains("压缩率:"));
    }

    @Test
    void testImageCompressWithDefaultQuality() {
        String result = imageHandler.imageCompress(pngFile.toString(), null, null);
        assertTrue(result.contains("图片压缩成功"));
    }

    @Test
    void testImageCompressWithCustomTargetPath() {
        Path targetPath = tempDir.resolve("compressed.png");
        String result = imageHandler.imageCompress(pngFile.toString(), 0.8f, targetPath.toString());
        assertTrue(result.contains("图片压缩成功"));
        assertTrue(Files.exists(targetPath));
    }

    @Test
    void testImageCompressWithNonExistentFile() {
        String nonExistentPath = tempDir.resolve("nonexistent.png")
                                        .toString();
        String result = imageHandler.imageCompress(nonExistentPath, 0.7f, null);
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("文件不存在"));
    }

    // ======================== image_resize ========================

    @Test
    void testImageResizeWithValidPng() {
        String result = imageHandler.imageResize(pngFile.toString(), 100, 50, null);
        assertTrue(result.contains("图片缩放成功"));
        assertTrue(result.contains("200x100 -> 100x50"));
    }

    @Test
    void testImageResizeWithZeroWidth() {
        String result = imageHandler.imageResize(pngFile.toString(), 0, 50, null);
        assertTrue(result.contains("图片缩放成功"));
    }

    @Test
    void testImageResizeWithZeroHeight() {
        String result = imageHandler.imageResize(pngFile.toString(), 100, 0, null);
        assertTrue(result.contains("图片缩放成功"));
    }

    @Test
    void testImageResizeWithCustomTargetPath() {
        Path targetPath = tempDir.resolve("resized.png");
        String result = imageHandler.imageResize(pngFile.toString(), 80, 40, targetPath.toString());
        assertTrue(result.contains("图片缩放成功"));
        assertTrue(Files.exists(targetPath));
    }

    @Test
    void testImageResizeWithNonExistentFile() {
        String nonExistentPath = tempDir.resolve("nonexistent.png")
                                        .toString();
        String result = imageHandler.imageResize(nonExistentPath, 100, 100, null);
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("文件不存在"));
    }

    // ======================== image_convert ========================

    @Test
    void testImageConvertPngToJpg() {
        String result = imageHandler.imageConvert(pngFile.toString(), "jpg", null);
        assertTrue(result.contains("图片格式转换成功"));
        assertTrue(result.contains("格式: jpg"));
    }

    @Test
    void testImageConvertPngToBmp() {
        String result = imageHandler.imageConvert(pngFile.toString(), "bmp", null);
        assertTrue(result.contains("图片格式转换成功"));
        assertTrue(result.contains("格式: bmp"));
    }

    @Test
    void testImageConvertWithCustomTargetPath() {
        Path targetPath = tempDir.resolve("converted.jpg");
        String result = imageHandler.imageConvert(pngFile.toString(), "jpg", targetPath.toString());
        assertTrue(result.contains("图片格式转换成功"));
        assertTrue(Files.exists(targetPath));
    }

    @Test
    void testImageConvertWithInvalidFormat() {
        String result = imageHandler.imageConvert(pngFile.toString(), "tiff", null);
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("不支持的目标格式"));
    }

    @Test
    void testImageConvertWithUnsupportedSourceFormat() {
        String result = imageHandler.imageConvert(nonImageFile.toString(), "jpg", null);
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("不支持的文件格式"));
    }

    @Test
    void testImageConvertWithNonExistentFile() {
        String nonExistentPath = tempDir.resolve("nonexistent.png")
                                        .toString();
        String result = imageHandler.imageConvert(nonExistentPath, "jpg", null);
        assertTrue(result.contains("错误"));
        assertTrue(result.contains("文件不存在"));
    }

    // ======================== 边界条件 ========================

    @Test
    void testImageCompressQualityOutOfRange() {
        String result = imageHandler.imageCompress(pngFile.toString(), 2.0f, null);
        assertTrue(result.contains("图片压缩成功"));
    }

    @Test
    void testImageCompressQualityNegative() {
        String result = imageHandler.imageCompress(pngFile.toString(), -0.5f, null);
        assertTrue(result.contains("图片压缩成功"));
    }
}