package com.example.mcp.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 路径操作工具类，提供通用的路径解析、父目录创建等功能。
 * 供各 Handler 复用，避免重复代码。
 *
 * @author Frank Kang
 * @since 2026-07-09
 */
public final class PathUtil {

    private PathUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 将路径字符串解析为绝对路径并规范化
     *
     * @param filePath 文件路径字符串
     * @return 解析后的绝对路径
     */
    public static Path resolvePath(String filePath) {
        return Paths.get(filePath).toAbsolutePath().normalize();
    }

    /**
     * 确保文件的父目录存在，不存在则递归创建
     *
     * @param filePath 文件路径
     * @throws IOException 创建目录失败时抛出
     */
    public static void ensureParentDirectory(Path filePath) throws IOException {
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    /**
     * 确保文件的父目录存在，不存在则递归创建（接受字符串路径）
     *
     * @param filePath 文件路径字符串
     * @throws IOException 创建目录失败时抛出
     */
    public static void ensureParentDirectory(String filePath) throws IOException {
        ensureParentDirectory(resolvePath(filePath));
    }
}