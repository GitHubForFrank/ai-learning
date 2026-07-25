package com.gitgui.core.util;

/**
 * 通配符匹配工具
 * <p>支持 {@code *} 与 {@code ?} 通配符，用于保护分支匹配（BR-27）与远程白名单匹配（BR-28）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public final class WildcardUtil {

    private WildcardUtil() {
        // 工具类禁止实例化
    }

    /**
     * 通配符匹配。
     * <p>支持 {@code *} 匹配任意长度字符（含空），{@code ?} 匹配单个字符。</p>
     *
     * @param pattern 含通配符的模式
     * @param text    待匹配文本
     * @return true 表示匹配
     */
    public static boolean match(String pattern, String text) {
        if (pattern == null || text == null) {
            return false;
        }
        if (pattern.isEmpty()) {
            return text.isEmpty();
        }
        return matchInternal(pattern, 0, text, 0);
    }

    /**
     * 内部递归匹配实现。
     */
    private static boolean matchInternal(String pattern, int pIdx, String text, int tIdx) {
        while (true) {
            if (pIdx == pattern.length()) {
                return tIdx == text.length();
            }
            char p = pattern.charAt(pIdx);
            if (p == '*') {
                // 跳过连续星号
                while (pIdx < pattern.length() && pattern.charAt(pIdx) == '*') {
                    pIdx++;
                }
                if (pIdx == pattern.length()) {
                    return true;
                }
                while (tIdx <= text.length()) {
                    if (matchInternal(pattern, pIdx, text, tIdx)) {
                        return true;
                    }
                    if (tIdx == text.length()) {
                        return false;
                    }
                    tIdx++;
                }
                return false;
            } else if (p == '?') {
                if (tIdx == text.length()) {
                    return false;
                }
                pIdx++;
                tIdx++;
            } else {
                if (tIdx == text.length() || pattern.charAt(pIdx) != text.charAt(tIdx)) {
                    return false;
                }
                pIdx++;
                tIdx++;
            }
        }
    }
}
