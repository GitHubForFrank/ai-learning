package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.image.ImageAdvancedHandler;
import com.example.mcp.pojo.image.ImageCropRequest;
import com.example.mcp.pojo.image.ImageTextRequest;
import com.example.mcp.pojo.image.ImageWatermarkRequest;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ImageAdvancedHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class ImageAdvancedHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private ImageAdvancedHandler imageAdvancedHandler;

    private Path createTestImage() throws Exception {
        Path imgPath = tempDir.resolve("test.png");
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(img, "PNG", imgPath.toFile());
        return imgPath;
    }

    @Test
    void testImageCrop() throws Exception {
        Path img = createTestImage();
        ImageCropRequest req = new ImageCropRequest(img.toString(), 10, 10, 50, 50, tempDir.resolve("cropped.png")
                                                                                           .toString());
        String result = imageAdvancedHandler.imageCrop(req);
        assertNotNull(result);
    }

    @Test
    void testImageRotate() throws Exception {
        Path img = createTestImage();
        String result = imageAdvancedHandler.imageRotate(img.toString(), 90, null);
        assertNotNull(result);
    }

    @Test
    void testImageInfo() throws Exception {
        Path img = createTestImage();
        String result = imageAdvancedHandler.imageInfo(img.toString());
        assertNotNull(result);
    }

    @Test
    void testImageToBase64() throws Exception {
        Path img = createTestImage();
        String result = imageAdvancedHandler.imageToBase64(img.toString(), false);
        assertNotNull(result);
        assertTrue(result.length() > 10);
    }

    @Test
    void testBase64ToImage() throws Exception {
        Path img = createTestImage();
        String b64 = imageAdvancedHandler.imageToBase64(img.toString(), false);
        String result = imageAdvancedHandler.base64ToImage(b64, tempDir.resolve("output.png")
                                                                       .toString());
        assertNotNull(result);
    }

    @Test
    void testImageWatermark() throws Exception {
        Path img = createTestImage();
        ImageWatermarkRequest req = new ImageWatermarkRequest(img.toString(), "水印", "center", 0.5f, 20, tempDir.resolve("watermarked.png")
                                                                                                                 .toString());
        String result = imageAdvancedHandler.imageWatermark(req);
        assertNotNull(result);
    }

    @Test
    void testImageAddText() throws Exception {
        Path img = createTestImage();
        ImageTextRequest req = new ImageTextRequest(img.toString(), "测试文字", 10, 80, 18, "red", tempDir.resolve("texted.png")
                                                                                                          .toString());
        String result = imageAdvancedHandler.imageAddText(req);
        assertNotNull(result);
    }
}
