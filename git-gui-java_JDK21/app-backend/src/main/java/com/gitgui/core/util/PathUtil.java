package com.gitgui.core.util;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 路径安全工具
 * <p>提供仓库路径校验、{@code ..} 跨越防护、绝对路径规范化等能力。</p>
 * <p>遵循 BR-42 路径安全约束：仓库路径校验为绝对路径且存在 {@code .git}，拒绝 {@code ..} 穿越。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public final class PathUtil {

    private PathUtil() {
        // 工具类禁止实例化
    }

    /**
     * 校验路径是否为合法的 Git 仓库绝对路径。
     * <p>规则：</p>
     * <ol>
     *   <li>非空且非空白</li>
     *   <li>规范化后不含 {@code ..} 跨越</li>
     *   <li>是绝对路径</li>
     *   <li>目录存在</li>
     *   <li>包含 {@code .git} 子目录或文件（worktree 场景 .git 为文件）</li>
     * </ol>
     *
     * @param rawPath 原始路径字符串
     * @throws InvalidPathException 路径非法
     */
    public static void validateGitRepoPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new InvalidPathException("", "仓库路径不能为空");
        }
        Path normalized = Paths.get(rawPath)
                               .normalize();
        if (rawPath.contains("..") && normalized.toString()
                                                .contains("..")) {
            throw new InvalidPathException(rawPath, "仓库路径不允许包含 .. 穿越");
        }
        if (!normalized.isAbsolute()) {
            throw new InvalidPathException(rawPath, "仓库路径必须为绝对路径");
        }
        if (!Files.isDirectory(normalized)) {
            throw new InvalidPathException(rawPath, "仓库路径不存在或非目录");
        }
        Path gitDir = normalized.resolve(".git");
        if (!Files.exists(gitDir)) {
            throw new InvalidPathException(rawPath, "目录不是 Git 仓库（缺少 .git）");
        }
    }

    /**
     * 判断目录是否为 Git 仓库（不抛异常，仅返回布尔）。
     *
     * @param dirPath 目录路径
     * @return true 表示是 Git 仓库
     */
    public static boolean isGitRepository(String dirPath) {
        try {
            validateGitRepoPath(dirPath);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 规范化路径字符串为绝对路径。
     *
     * @param rawPath 原始路径
     * @return 规范化绝对路径字符串
     */
    public static String normalize(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }
        return Paths.get(rawPath)
                    .normalize()
                    .toAbsolutePath()
                    .toString();
    }
}
