package com.example.mcp.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.handler.tool.CryptoHandler;
import com.example.mcp.pojo.crypto.PasswordGenerateRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * CryptoHandler 单元测试
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@SpringBootTest
class CryptoHandlerTest {

    @TempDir
    Path tempDir;
    @Autowired
    private CryptoHandler cryptoHandler;

    @Test
    void testCryptoAesEncryptDecrypt() {
        String encrypted = cryptoHandler.cryptoAesEncrypt("测试数据", "MTIzNDU2Nzg5MDEyMzQ1Ng==", false);
        assertNotNull(encrypted);
        String decrypted = cryptoHandler.cryptoAesDecrypt(encrypted, "MTIzNDU2Nzg5MDEyMzQ1Ng==", false);
        assertTrue(decrypted.contains("测试数据"));
    }

    @Test
    void testCryptoHashFile() throws Exception {
        Path f = tempDir.resolve("test.txt");
        Files.writeString(f, "hello");
        String result = cryptoHandler.cryptoHashFile(f.toString(), "MD5", null);
        assertNotNull(result);
    }

    @Test
    void testCryptoPasswordGenerate() {
        PasswordGenerateRequest req = new PasswordGenerateRequest(16, true, true, true, true);
        String result = cryptoHandler.cryptoPasswordGenerate(req);
        assertNotNull(result);
        assertTrue(result.length() >= 16);
    }

    @Test
    void testCryptoBase64() {
        String encoded = cryptoHandler.cryptoBase64("Hello World", "encode", "text", null);
        assertNotNull(encoded);
        String decoded = cryptoHandler.cryptoBase64(encoded, "decode", "text", null);
        assertTrue(decoded.contains("Hello"));
    }
}
