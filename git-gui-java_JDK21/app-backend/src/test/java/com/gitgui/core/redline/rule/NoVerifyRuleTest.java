package com.gitgui.core.redline.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gitgui.core.constant.OperationType;
import com.gitgui.core.constant.RedLineCode;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * --no-verify 跳过 hook 规则单元测试（BR-26）
 * <p>验证 {@link NoVerifyRule} 对 --no-verify 的拦截与正常提交的放行行为。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
class NoVerifyRuleTest {

    /**
     * 被测规则依赖的设置服务 mock
     */
    private SettingsService settingsService;

    /**
     * 被测规则实例
     */
    private NoVerifyRule rule;

    @BeforeEach
    void setUp() {
        settingsService = mock(SettingsService.class);
        when(settingsService.isRedLineEnabled()).thenReturn(true);
        rule = new NoVerifyRule(settingsService);
    }

    /**
     * 使用 --no-verify 应被阻断。
     */
    @Test
    @DisplayName("--no-verify 提交应返回 BLOCK")
    void shouldBlockNoVerify() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.COMMIT)
                                           .command("git commit --no-verify -m msg")
                                           .noVerify(true)
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.BLOCK, result.getAction());
        assertEquals(RedLineCode.RED_NO_VERIFY, result.getRuleCode());
        assertNotNull(result.getMessage());
    }

    /**
     * 正常提交（未使用 --no-verify）应放行。
     */
    @Test
    @DisplayName("正常提交应返回 PASS")
    void shouldPassNormalCommit() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.COMMIT)
                                           .command("git commit -m msg")
                                           .noVerify(false)
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * 红线总开关关闭时，BLOCK 应降级为 CONFIRM（BR-30）。
     */
    @Test
    @DisplayName("红线关闭时 --no-verify BLOCK 应降级为 CONFIRM")
    void shouldDowngradeToConfirmWhenRedLineDisabled() {
        when(settingsService.isRedLineEnabled()).thenReturn(false);

        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.COMMIT)
                                           .noVerify(true)
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.CONFIRM, result.getAction());
        assertEquals(RedLineCode.RED_NO_VERIFY, result.getRuleCode());
    }

    /**
     * 规则代码应返回 {@link RedLineCode#RED_NO_VERIFY}。
     */
    @Test
    @DisplayName("ruleCode 应返回 RED_NO_VERIFY")
    void shouldReturnCorrectRuleCode() {
        assertEquals(RedLineCode.RED_NO_VERIFY.name(), rule.ruleCode());
    }
}
