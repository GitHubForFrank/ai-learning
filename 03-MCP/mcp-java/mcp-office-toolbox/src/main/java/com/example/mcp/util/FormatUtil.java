package com.example.mcp.util;

/**
 * 通用格式化工具类，提供字节数、运行时间等格式化方法。
 *
 * @author Frank Kang
 * @since 2026-07-13
 */
public final class FormatUtil {

    private static final long KB = 1024L;
    private static final long MB = 1024L * KB;
    private static final long GB = 1024L * MB;
    private static final long TB = 1024L * GB;

    private FormatUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 格式化字节数为可读字符串（如 "1.50 GB"、"512.00 MB"）。
     *
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    public static String formatBytes(long bytes) {
        if (bytes >= TB) {
            return String.format("%.2f TB", (double) bytes / TB);
        } else if (bytes >= GB) {
            return String.format("%.2f GB", (double) bytes / GB);
        } else if (bytes >= MB) {
            return String.format("%.2f MB", (double) bytes / MB);
        } else if (bytes >= KB) {
            return String.format("%.2f KB", (double) bytes / KB);
        } else {
            return bytes + " 字节";
        }
    }

    /**
     * 格式化运行时间为可读字符串（如 "2 天 3 小时 5 分钟 10 秒"）。
     *
     * @param uptimeMillis 运行时间毫秒数
     * @return 格式化后的字符串
     */
    public static String formatUptime(long uptimeMillis) {
        long days = uptimeMillis / (24 * 60 * 60 * 1000L);
        long hours = (uptimeMillis % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L);
        long minutes = (uptimeMillis % (60 * 60 * 1000L)) / (60 * 1000L);
        long seconds = (uptimeMillis % (60 * 1000L)) / 1000L;
        if (days > 0) {
            return String.format("%d 天 %d 小时 %d 分钟 %d 秒", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%d 小时 %d 分钟 %d 秒", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d 分钟 %d 秒", minutes, seconds);
        } else {
            return seconds + " 秒";
        }
    }
}