package com.gitgui.core.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 通配符匹配工具单元测试（BR-27/BR-28）
 * <p>验证 {@link WildcardUtil} 对 {@code *} 与 {@code ?} 通配符、
 * 精确匹配、空模式及 null 输入的处理行为。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
class WildcardUtilTest {

    /**
     * 精确匹配应返回 true。
     */
    @Test
    @DisplayName("精确匹配应返回 true")
    void shouldMatchExact() {
        assertTrue(WildcardUtil.match("main", "main"));
        assertTrue(WildcardUtil.match("develop", "develop"));
    }

    /**
     * 精确匹配不一致应返回 false。
     */
    @Test
    @DisplayName("精确匹配不一致应返回 false")
    void shouldNotMatchDifferentExact() {
        assertFalse(WildcardUtil.match("main", "master"));
        assertFalse(WildcardUtil.match("main", "main2"));
    }

    /**
     * * 通配符应匹配任意长度字符（含空）。
     */
    @Test
    @DisplayName("* 通配符应匹配任意长度字符")
    void shouldMatchStarWildcard() {
        // release/* 匹配 release/v1.0
        assertTrue(WildcardUtil.match("release/*", "release/v1.0"));
        // release/* 匹配 release/（空后缀）
        assertTrue(WildcardUtil.match("release/*", "release/"));
        // 单独 * 匹配任意字符串
        assertTrue(WildcardUtil.match("*", "anything"));
        assertTrue(WildcardUtil.match("*", ""));
    }

    /**
     * * 通配符前缀不一致应返回 false。
     */
    @Test
    @DisplayName("* 通配符前缀不一致应返回 false")
    void shouldNotMatchStarWildcardWithDifferentPrefix() {
        assertFalse(WildcardUtil.match("release/*", "main"));
        assertFalse(WildcardUtil.match("release/*", "feature/release/v1"));
    }

    /**
     * ? 通配符应仅匹配单个字符。
     */
    @Test
    @DisplayName("? 通配符应匹配单个字符")
    void shouldMatchQuestionWildcard() {
        // v? 匹配 v1
        assertTrue(WildcardUtil.match("v?", "v1"));
        // v? 不匹配 v12（? 仅匹配一个字符）
        assertFalse(WildcardUtil.match("v?", "v12"));
        // v? 不匹配 v（? 必须消耗一个字符）
        assertFalse(WildcardUtil.match("v?", "v"));
    }

    /**
     * 组合通配符匹配。
     */
    @Test
    @DisplayName("组合 * 与 ? 通配符应正确匹配")
    void shouldMatchCombinedWildcards() {
        // *.? 匹配 a.b（* 匹配 a，? 匹配 b）
        assertTrue(WildcardUtil.match("*.?", "a.b"));
        // v?-release 匹配 v1-release
        assertTrue(WildcardUtil.match("v?-release", "v1-release"));
        // 多个连续 * 等价于单个 *
        assertTrue(WildcardUtil.match("a**b", "axxxb"));
    }

    /**
     * null 输入应返回 false。
     */
    @Test
    @DisplayName("null 输入应返回 false")
    void shouldReturnFalseForNullInputs() {
        assertFalse(WildcardUtil.match(null, "main"));
        assertFalse(WildcardUtil.match("main", null));
        assertFalse(WildcardUtil.match(null, null));
    }

    /**
     * 空模式应仅在文本也为空时返回 true。
     */
    @Test
    @DisplayName("空模式仅匹配空文本")
    void shouldHandleEmptyPattern() {
        // 空模式匹配空文本
        assertTrue(WildcardUtil.match("", ""));
        // 空模式不匹配非空文本
        assertFalse(WildcardUtil.match("", "a"));
    }

    /**
     * 空文本与非空模式的匹配行为。
     */
    @Test
    @DisplayName("空文本与非空模式匹配行为")
    void shouldHandleEmptyText() {
        // 模式为 * 匹配空文本
        assertTrue(WildcardUtil.match("*", ""));
        // 普通模式不匹配空文本
        assertFalse(WildcardUtil.match("a", ""));
        // ? 不匹配空文本
        assertFalse(WildcardUtil.match("?", ""));
    }
}
