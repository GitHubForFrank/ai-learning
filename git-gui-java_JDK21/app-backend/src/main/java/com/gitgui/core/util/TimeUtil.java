package com.gitgui.core.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具
 * <p>统一应用层日期时间格式化（ISO-8601 紧凑格式）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public final class TimeUtil {

    /** ISO-8601 紧凑格式：yyyyMMdd HH:mm:ss */
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtil() {
        // 工具类禁止实例化
    }

    /**
     * 获取当前时间格式化字符串。
     *
     * @return 当前时间字符串
     */
    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    /**
     * 格式化指定时间。
     *
     * @param time 时间
     * @return 格式化字符串
     */
    public static String format(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return time.format(FORMATTER);
    }

    /**
     * 解析时间字符串为 LocalDateTime。
     * <p>兼容 TimeUtil.format 输出与 SQLite datetime('now','localtime') 默认值（"yyyy-MM-dd HH:mm:ss"）。
     * 解析失败返回 null。</p>
     *
     * @param text 时间字符串
     * @return LocalDateTime；null 或解析失败返回 null
     */
    public static LocalDateTime parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, FORMATTER);
        } catch (Exception e) {
            // 兼容 SQLite 默认 datetime('now','localtime') 可能携带的毫秒/时区后缀，截取前 19 位再解析
            String trimmed = text.length() > 19 ? text.substring(0, 19) : text;
            try {
                return LocalDateTime.parse(trimmed, FORMATTER);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
