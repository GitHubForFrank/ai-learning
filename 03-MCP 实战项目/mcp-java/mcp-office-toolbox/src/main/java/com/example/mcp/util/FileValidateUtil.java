package com.example.mcp.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件校验工具类，提供通用的文件存在性检查和扩展名校验功能。
 * 供各 Handler 复用，避免重复代码。
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
public final class FileValidateUtil {

    private FileValidateUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 校验文件是否存在，不存在则抛出异常
     *
     * @param fileAbsolutePath 文件绝对路径
     * @return 解析后的 Path 对象
     * @throws IllegalArgumentException 文件不存在时抛出
     */
    public static Path validateExists(String fileAbsolutePath) {
        Path path = Paths.get(fileAbsolutePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("文件不存在: " + fileAbsolutePath);
        }
        return path;
    }

    /**
     * 校验文件是否存在且扩展名匹配（不区分大小写），不匹配则抛出异常
     *
     * @param fileAbsolutePath 文件绝对路径
     * @param extensions       允许的扩展名（如 ".xlsx", ".pdf"）
     * @return 解析后的 Path 对象
     * @throws IllegalArgumentException 文件不存在或扩展名不匹配时抛出
     */
    public static Path validateFile(String fileAbsolutePath, String... extensions) {
        Path path = validateExists(fileAbsolutePath);
        String name = path.getFileName().toString().toLowerCase();
        for (String ext : extensions) {
            if (name.endsWith(ext.toLowerCase())) {
                return path;
            }
        }
        throw new IllegalArgumentException("不支持的文件格式，仅支持 " + String.join(", ", extensions)
                + ": " + fileAbsolutePath);
    }

    /**
     * 校验文件是否存在且为指定扩展名，不存在则抛出异常
     *
     * @param fileAbsolutePath 文件绝对路径
     * @param extension        允许的扩展名（如 ".xlsx"）
     * @return 解析后的 Path 对象
     * @throws IllegalArgumentException 文件不存在或扩展名不匹配时抛出
     */
    public static Path validateFile(String fileAbsolutePath, String extension) {
        return validateFile(fileAbsolutePath, new String[] { extension });
    }
}