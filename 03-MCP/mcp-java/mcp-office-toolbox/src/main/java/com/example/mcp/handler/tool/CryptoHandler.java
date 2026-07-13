package com.example.mcp.handler.tool;

import com.example.mcp.handler.BaseHandler;

import com.example.mcp.pojo.crypto.PasswordGenerateRequest;
import com.example.mcp.util.LogUtil;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP 加密解密工具实现，提供 AES 对称加解密、文件哈希计算、安全密码生成和 Base64 编解码功能。
 * 基于 JDK 21 javax.crypto 和 java.security 实现。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Service
public class CryptoHandler extends BaseHandler {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_CBC_PADDING = "AES/CBC/PKCS5Padding";
    private static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";
    private static final int AES_IV_SIZE = 16; // 128 bits
    private static final int GCM_IV_SIZE = 12; // 96 bits for GCM
    private static final int GCM_TAG_SIZE = 128; // bits

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIALS = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    /**
     * 校验文件是否存在
     *
     * @param fileAbsolutePath 文件绝对路径
     * @return 解析后的 Path 对象
     * @throws IllegalArgumentException 文件不存在时抛出
     */
    private Path validateFileExists(String fileAbsolutePath) {
        Path path = Paths.get(fileAbsolutePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("文件不存在: " + fileAbsolutePath);
        }
        return path;
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

    // --- 1. crypto_aes_encrypt ---

    /**
     * AES 对称加密，支持 AES-128/AES-256，输出 Base64 编码的密文。
     * 使用 CBC 模式 + PKCS5Padding，自动生成随机 IV 并拼接在密文前面。
     *
     * @param plainText 明文字符串
     * @param secretKey 密钥（Base64 编码，16字节为AES-128，32字节为AES-256）
     * @param useGcm    是否使用 GCM 模式（默认 false，使用 CBC 模式）
     * @return Base64 编码的密文（IV + 密文）
     */
    @Tool(name = "crypto_aes_encrypt", description = "AES 对称加密。支持 AES-128/AES-256，输出 Base64 编码的密文。支持 CBC 和 GCM 模式。")
    public String cryptoAesEncrypt(@ToolParam(description = "明文字符串") String plainText,
        @ToolParam(description = "密钥（Base64 编码，16字节为AES-128，32字节为AES-256）") String secretKey,
        @ToolParam(description = "是否使用 GCM 模式，默认 false（CBC 模式）", required = false) Boolean useGcm) {
        try {
            if (plainText == null || plainText.isEmpty()) {
                return "错误: 明文不能为空";
            }
            if (secretKey == null || secretKey.isBlank()) {
                return "错误: 密钥不能为空";
            }

            byte[] keyBytes = Base64.getDecoder()
                                    .decode(secretKey);
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                return String.format("错误: 密钥长度无效 (%d 字节)，AES 密钥必须为 16、24 或 32 字节（Base64 编码前）", keyBytes.length);
            }

            boolean gcm = (useGcm != null && useGcm);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[gcm ? GCM_IV_SIZE : AES_IV_SIZE];
            random.nextBytes(iv);

            Cipher cipher;
            byte[] encrypted;
            if (gcm) {
                cipher = Cipher.getInstance(AES_GCM_NOPADDING);
                GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_SIZE, iv);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
                encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            } else {
                cipher = Cipher.getInstance(AES_CBC_PADDING);
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
                encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            }

            // IV + 密文合并
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            String result = Base64.getEncoder()
                                  .encodeToString(combined);
            LogUtil.info("AES 加密成功，模式: {}, 密钥长度: {} bits", gcm ? "GCM" : "CBC", keyBytes.length * 8);
            return result;
        } catch (Exception e) {
            LogUtil.error("cryptoAesEncrypt 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 2. crypto_aes_decrypt ---

    /**
     * AES 对称解密，解密由 crypto_aes_encrypt 生成的密文。
     *
     * @param cipherText Base64 编码的密文（IV + 密文）
     * @param secretKey  密钥（Base64 编码，与加密时相同）
     * @param useGcm     是否使用 GCM 模式（需与加密时一致）
     * @return 解密后的明文字符串
     */
    @Tool(name = "crypto_aes_decrypt", description = "AES 对称解密。解密由 crypto_aes_encrypt 生成的 Base64 密文，支持 CBC 和 GCM 模式。")
    public String cryptoAesDecrypt(@ToolParam(description = "Base64 编码的密文") String cipherText,
        @ToolParam(description = "密钥（Base64 编码，需与加密时相同）") String secretKey,
        @ToolParam(description = "是否使用 GCM 模式（需与加密时一致），默认 false", required = false) Boolean useGcm) {
        try {
            if (cipherText == null || cipherText.isBlank()) {
                return "错误: 密文不能为空";
            }
            if (secretKey == null || secretKey.isBlank()) {
                return "错误: 密钥不能为空";
            }

            byte[] keyBytes = Base64.getDecoder()
                                    .decode(secretKey);
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                return String.format("错误: 密钥长度无效 (%d 字节)，AES 密钥必须为 16、24 或 32 字节（Base64 编码前）", keyBytes.length);
            }

            boolean gcm = (useGcm != null && useGcm);
            byte[] combined = Base64.getDecoder()
                                    .decode(cipherText);
            int ivSize = gcm ? GCM_IV_SIZE : AES_IV_SIZE;

            if (combined.length < ivSize) {
                return "错误: 密文长度不足，无法提取 IV";
            }

            // 提取 IV
            byte[] iv = new byte[ivSize];
            System.arraycopy(combined, 0, iv, 0, ivSize);

            // 提取密文
            byte[] encrypted = new byte[combined.length - ivSize];
            System.arraycopy(combined, ivSize, encrypted, 0, encrypted.length);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);
            Cipher cipher;
            byte[] decrypted;

            if (gcm) {
                cipher = Cipher.getInstance(AES_GCM_NOPADDING);
                GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_SIZE, iv);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            } else {
                cipher = Cipher.getInstance(AES_CBC_PADDING);
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            }

            decrypted = cipher.doFinal(encrypted);
            String result = new String(decrypted, "UTF-8");

            LogUtil.info("AES 解密成功，模式: {}", gcm ? "GCM" : "CBC");
            return result;
        } catch (Exception e) {
            LogUtil.error("cryptoAesDecrypt 失败: {}", e.getMessage(), e);
            return "错误: 解密失败，" + e.getMessage();
        }
    }

    // --- 3. crypto_hash_file ---

    /**
     * 计算文件哈希值，支持 MD5/SHA-1/SHA-256/SHA-512。
     *
     * @param fileAbsolutePath 文件绝对路径
     * @param algorithm        哈希算法（MD5/SHA-1/SHA-256/SHA-512）
     * @param outputFile       输出文件路径（可选，指定则将哈希值写入文件）
     * @return 哈希值字符串
     */
    @Tool(name = "crypto_hash_file", description = "计算文件哈希值。支持 MD5、SHA-1、SHA-256、SHA-512，可选择将结果写入文件。")
    public String cryptoHashFile(@ToolParam(description = "文件绝对路径") String fileAbsolutePath,
        @ToolParam(description = "哈希算法，可选值: MD5/SHA-1/SHA-256/SHA-512") String algorithm,
        @ToolParam(description = "输出文件路径（可选，指定则将哈希值写入文件）", required = false) String outputFile) {
        try {
            Path path = validateFileExists(fileAbsolutePath);
            String algo = (algorithm != null && !algorithm.isBlank()) ? algorithm.toUpperCase(Locale.ROOT) : "SHA-256";

            // 校验算法名称
            String[] validAlgos = {"MD5", "SHA-1", "SHA-256", "SHA-512"};
            boolean valid = false;
            for (String va : validAlgos) {
                if (va.equals(algo)) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                return "错误: 不支持的哈希算法 '" + algorithm + "'，支持: MD5、SHA-1、SHA-256、SHA-512";
            }

            MessageDigest md = MessageDigest.getInstance(algo);
            try (InputStream is = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }

            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            String hash = sb.toString();
            long fileSize = Files.size(path);

            // 如果指定了输出文件，写入哈希值
            if (outputFile != null && !outputFile.isBlank()) {
                Path outPath = Paths.get(outputFile);
                ensureParentDir(outPath);
                try (BufferedWriter writer = Files.newBufferedWriter(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write("文件: " + fileAbsolutePath);
                    writer.newLine();
                    writer.write("算法: " + algo);
                    writer.newLine();
                    writer.write("哈希: " + hash);
                    writer.newLine();
                    writer.write("文件大小: " + fileSize + " 字节");
                    writer.newLine();
                }
                LogUtil.info("文件哈希计算完成并写入: {}, 算法: {}, 哈希: {}", fileAbsolutePath, algo, hash);
                return String.format("文件哈希计算完成\n算法: %s\n哈希: %s\n文件大小: %d 字节\n已写入: %s", algo, hash, fileSize, outputFile);
            }

            LogUtil.info("文件哈希计算完成: {}, 算法: {}, 哈希: {}", fileAbsolutePath, algo, hash);
            return String.format("文件哈希计算完成\n算法: %s\n哈希: %s\n文件大小: %d 字节", algo, hash, fileSize);
        } catch (Exception e) {
            LogUtil.error("cryptoHashFile 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 4. crypto_password_generate ---

    /**
     * 生成安全随机密码，可指定长度和字符类型。
     *
     * @param length           密码长度（默认 16）
     * @param includeUppercase 是否包含大写字母（默认 true）
     * @param includeLowercase 是否包含小写字母（默认 true）
     * @param includeDigits    是否包含数字（默认 true）
     * @param includeSpecials  是否包含特殊符号（默认 true）
     * @return 生成的随机密码
     */
    @Tool(name = "crypto_password_generate", description = "生成安全随机密码。可指定长度和字符类型（大小写字母+数字+特殊符号）。")
    public String cryptoPasswordGenerate(PasswordGenerateRequest request) {
        try {
            int len = (request.length() != null && request.length() > 0) ? request.length() : 16;
            if (len < 4) {
                return "错误: 密码长度不能小于 4";
            }

            boolean upper = (request.includeUppercase() == null || request.includeUppercase());
            boolean lower = (request.includeLowercase() == null || request.includeLowercase());
            boolean digit = (request.includeDigits() == null || request.includeDigits());
            boolean special = (request.includeSpecials() == null || request.includeSpecials());

            StringBuilder charPool = new StringBuilder();
            if (upper) {
                charPool.append(UPPERCASE);
            }
            if (lower) {
                charPool.append(LOWERCASE);
            }
            if (digit) {
                charPool.append(DIGITS);
            }
            if (special) {
                charPool.append(SPECIALS);
            }

            if (charPool.length() == 0) {
                return "错误: 至少需要选择一种字符类型";
            }

            SecureRandom random = new SecureRandom();
            StringBuilder password = new StringBuilder(len);

            // 确保每种选中的字符类型至少出现一次
            if (upper) {
                password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
            }
            if (lower) {
                password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
            }
            if (digit) {
                password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
            }
            if (special) {
                password.append(SPECIALS.charAt(random.nextInt(SPECIALS.length())));
            }

            // 填充剩余长度
            for (int i = password.length(); i < len; i++) {
                password.append(charPool.charAt(random.nextInt(charPool.length())));
            }

            // 随机打乱密码字符顺序
            char[] chars = password.toString()
                                   .toCharArray();
            for (int i = chars.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                char temp = chars[i];
                chars[i] = chars[j];
                chars[j] = temp;
            }

            String result = new String(chars);
            LogUtil.info("安全密码生成成功，长度: {}", len);
            return result;
        } catch (Exception e) {
            LogUtil.error("cryptoPasswordGenerate 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }

    // --- 5. crypto_base64 ---

    /**
     * Base64 编解码，支持文本和文件内容的编解码。
     *
     * @param input      输入内容（文本字符串或文件路径，根据 mode 决定）
     * @param mode       操作模式（encode/decode）
     * @param inputType  输入类型（text/file），text 表示 input 是文本，file 表示 input 是文件路径
     * @param outputFile 输出文件路径（可选，指定则将结果写入文件）
     * @return 编解码结果
     */
    @Tool(name = "crypto_base64", description = "Base64 编解码。支持文本和文件内容的 Base64 编码和解码，可选择将结果写入文件。")
    public String cryptoBase64(@ToolParam(description = "输入内容（文本字符串或文件路径，根据 inputType 决定）") String input,
        @ToolParam(description = "操作模式，encode（编码）或 decode（解码）") String mode,
        @ToolParam(description = "输入类型，text（文本）或 file（文件路径），默认 text", required = false) String inputType,
        @ToolParam(description = "输出文件路径（可选，指定则将结果写入文件）", required = false) String outputFile) {
        try {
            if (input == null || input.isBlank()) {
                return "错误: 输入内容不能为空";
            }
            if (mode == null || mode.isBlank()) {
                return "错误: 操作模式不能为空，请指定 encode 或 decode";
            }

            boolean isEncode = "encode".equalsIgnoreCase(mode.trim());
            boolean isDecode = "decode".equalsIgnoreCase(mode.trim());

            if (!isEncode && !isDecode) {
                return "错误: 操作模式必须是 encode 或 decode，当前值: " + mode;
            }

            boolean isFile = "file".equalsIgnoreCase(inputType != null ? inputType.trim() : "text");

            byte[] inputBytes;
            if (isFile) {
                Path filePath = validateFileExists(input);
                inputBytes = Files.readAllBytes(filePath);
            } else {
                inputBytes = input.getBytes("UTF-8");
            }

            String result;
            if (isEncode) {
                result = Base64.getEncoder()
                               .encodeToString(inputBytes);
            } else {
                try {
                    byte[] decoded = Base64.getDecoder()
                                           .decode(input.trim()
                                                        .replaceAll("\\s", ""));
                    if (outputFile != null && !outputFile.isBlank()) {
                        // 解码到文件
                        Path outPath = Paths.get(outputFile);
                        ensureParentDir(outPath);
                        Files.write(outPath, decoded);
                        LogUtil.info("Base64 解码完成，写入文件: {}, 大小: {} 字节", outputFile, decoded.length);
                        return "Base64 解码成功，已写入文件: " + outputFile + " (" + decoded.length + " 字节)";
                    }
                    // 尝试作为文本返回
                    result = new String(decoded, "UTF-8");
                } catch (IllegalArgumentException e) {
                    return "错误: Base64 解码失败，输入格式不正确: " + e.getMessage();
                }
            }

            // 如果指定了输出文件，写入结果
            if (outputFile != null && !outputFile.isBlank() && isEncode) {
                Path outPath = Paths.get(outputFile);
                ensureParentDir(outPath);
                Files.writeString(outPath, result);
                LogUtil.info("Base64 编码完成，写入文件: {}", outputFile);
                return "Base64 编码成功，已写入文件: " + outputFile;
            }

            LogUtil.info("Base64 {} 成功", isEncode ? "编码" : "解码");
            return result;
        } catch (Exception e) {
            LogUtil.error("cryptoBase64 失败: {}", e.getMessage(), e);
            return "错误: " + e.getMessage();
        }
    }
}
