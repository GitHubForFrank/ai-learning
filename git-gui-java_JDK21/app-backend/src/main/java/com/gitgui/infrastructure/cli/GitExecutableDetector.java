package com.gitgui.infrastructure.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Git 可执行文件检测器
 * <p>遵循 BR-41：启动时检测本地 Git 可执行文件，未安装时降级为纯 JGit 模式。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class GitExecutableDetector {

    private static final Logger log = LoggerFactory.getLogger(GitExecutableDetector.class);

    /**
     * 检测本地 Git 可执行文件路径。
     * <p>优先级：用户配置 → PATH 查找 → 常见安装路径。</p>
     *
     * @param configuredPath 用户在 Settings 中配置的路径（可空）
     * @return Git 可执行文件绝对路径，未找到返回 null
     */
    public static String detect(String configuredPath) {
        // 1. 用户配置优先
        if (configuredPath != null && !configuredPath.isBlank()) {
            if (Files.isExecutable(Paths.get(configuredPath))) {
                log.info("Git 可执行文件（用户配置）：{}", configuredPath);
                return configuredPath;
            }
            log.warn("用户配置的 Git 路径不可用：{}", configuredPath);
        }
        // 2. 从 PATH 查找
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String osName = System.getProperty("os.name", "").toLowerCase();
            String executable = osName.contains("win") ? "git.exe" : "git";
            for (String dir : pathEnv.split(File.pathSeparator)) {
                try {
                    Path candidate = Paths.get(dir, executable);
                    if (Files.isExecutable(candidate)) {
                        log.info("Git 可执行文件（PATH）：{}", candidate);
                        return candidate.toAbsolutePath().toString();
                    }
                } catch (RuntimeException e) {
                    // 路径异常跳过
                }
            }
        }
        // 3. 常见安装路径
        String[] commonPaths = isWindows() ? new String[]{
                "C:\\Program Files\\Git\\bin\\git.exe",
                "C:\\Program Files (x86)\\Git\\bin\\git.exe"
        } : new String[]{
                "/usr/bin/git", "/usr/local/bin/git", "/opt/homebrew/bin/git"
        };
        for (String p : commonPaths) {
            if (Files.isExecutable(Paths.get(p))) {
                log.info("Git 可执行文件（常见路径）：{}", p);
                return p;
            }
        }
        log.warn("未检测到本地 Git 可执行文件，降级为纯 JGit 模式（BR-41）");
        return null;
    }

    /**
     * 是否 Windows 平台。
     */
    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
