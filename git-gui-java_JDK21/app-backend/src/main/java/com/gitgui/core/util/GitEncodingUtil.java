package com.gitgui.core.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Git 编码工具
 * <p>统一 Git 命令行调用的 UTF-8 编码与 {@code core.quotepath=false} 设置，避免 Windows 中文路径乱码。</p>
 * <p>遵循 BR-42：所有 Git 命令行调用强制 UTF-8 编码并设置 core.quotepath=false。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public final class GitEncodingUtil {

    /**
     * 强制编码名称
     */
    public static final String ENCODING = "UTF-8";

    /**
     * Git 环境变量：强制 locale 为 UTF-8
     */
    public static final String ENV_LANG = "C";

    private GitEncodingUtil() {
        // 工具类禁止实例化
    }

    /**
     * 构造 Git CLI 必需的环境变量集合。
     * <p>包含 LANG/LC_ALL 等 locale 变量，确保输出按 UTF-8 编码。</p>
     *
     * @return 环境变量 Map（不可变视图）
     */
    public static Map<String, String> gitEnvironment() {
        Map<String, String> env = new HashMap<>();
        // 强制 locale 为 C，避免系统默认编码影响 Git 输出
        env.put("LANG", ENV_LANG);
        env.put("LC_ALL", ENV_LANG);
        env.put("GIT_TERMINAL_PROMPT", "0");
        return env;
    }
}
