package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.OperationType;
import com.gitgui.core.constant.RedLineCode;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 裸 --force push 规则单元测试（BR-26）
 * <p>验证 {@link ForcePushRule} 在不同推送场景下的命中判定，
 * 以及红线总开关关闭时 BLOCK 降级为 CONFIRM 的行为（BR-30）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
class ForcePushRuleTest {

    /** 被测规则依赖的设置服务 mock */
    private SettingsService settingsService;

    /** 被测规则实例 */
    private ForcePushRule rule;

    @BeforeEach
    void setUp() {
        // 构造 mock 设置服务，默认红线总开关开启
        settingsService = mock(SettingsService.class);
        when(settingsService.isRedLineEnabled()).thenReturn(true);
        rule = new ForcePushRule(settingsService);
    }

    /**
     * 裸 --force 推送应被阻断。
     * <p>命中条件：force=true 且 forceWithLease=false。</p>
     */
    @Test
    @DisplayName("裸 --force push 应返回 BLOCK")
    void shouldBlockBareForcePush() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .command("git push --force")
                .force(true)
                .forceWithLease(false)
                .build();

        RedLineResult result = rule.check(ctx);

        // 校验动作与规则代码
        assertEquals(RedLineResult.Action.BLOCK, result.getAction());
        assertEquals(RedLineCode.RED_FORCE_PUSH, result.getRuleCode());
        assertNotNull(result.getMessage());
    }

    /**
     * --force-with-lease 推送应放行。
     * <p>安全检查的强制推送不命中阻断条件。</p>
     */
    @Test
    @DisplayName("--force-with-lease 推送应返回 PASS")
    void shouldPassForceWithLease() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .command("git push --force-with-lease")
                .force(true)
                .forceWithLease(true)
                .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * 仅使用 force-with-lease（force=false）也应放行。
     */
    @Test
    @DisplayName("仅 forceWithLease=true 也应返回 PASS")
    void shouldPassWhenOnlyForceWithLease() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .force(false)
                .forceWithLease(true)
                .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * 普通推送（非 force）应放行。
     */
    @Test
    @DisplayName("非 force 推送应返回 PASS")
    void shouldPassNonForcePush() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .command("git push")
                .force(false)
                .forceWithLease(false)
                .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * 红线总开关关闭时，BLOCK 应降级为 CONFIRM（BR-30）。
     * <p>此时裸 --force 不再硬阻断，而是交由 UI 二次确认。</p>
     */
    @Test
    @DisplayName("红线关闭时 BLOCK 应降级为 CONFIRM")
    void shouldDowngradeToConfirmWhenRedLineDisabled() {
        // 红线总开关关闭
        when(settingsService.isRedLineEnabled()).thenReturn(false);

        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .force(true)
                .forceWithLease(false)
                .build();

        RedLineResult result = rule.check(ctx);

        // 降级为 CONFIRM，规则代码与提示信息保留
        assertEquals(RedLineResult.Action.CONFIRM, result.getAction());
        assertEquals(RedLineCode.RED_FORCE_PUSH, result.getRuleCode());
        assertNotNull(result.getMessage());
    }

    /**
     * 规则代码应返回 {@link RedLineCode#RED_FORCE_PUSH}。
     */
    @Test
    @DisplayName("ruleCode 应返回 RED_FORCE_PUSH")
    void shouldReturnCorrectRuleCode() {
        assertEquals(RedLineCode.RED_FORCE_PUSH.name(), rule.ruleCode());
    }
}
