package com.gitgui.core.util;

import java.util.UUID;

/**
 * ID 生成工具
 * <p>统一应用层 UUID 主键生成策略，避免依赖数据库自增。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public final class IdUtil {

    private IdUtil() {
        // 工具类禁止实例化
    }

    /**
     * 生成新的 UUID 字符串（无连字符）。
     *
     * @return 32 位 UUID 字符串
     */
    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
