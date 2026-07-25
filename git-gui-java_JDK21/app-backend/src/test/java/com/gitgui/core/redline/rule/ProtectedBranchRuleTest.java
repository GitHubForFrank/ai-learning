package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.OperationType;
import com.gitgui.core.constant.RedLineCode;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 向保护分支 force push 规则单元测试（BR-26/BR-27）
 * <p>验证 {@link ProtectedBranchRule} 对保护分支清单（含通配符）的命中判定，
 * 以及 force/force-with-lease 两种强制推送的拦截行为。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
class ProtectedBranchRuleTest {

    /** 被测规则依赖的设置服务 mock */
    private SettingsService settingsService;

    /** 被测规则实例 */
    private ProtectedBranchRule rule;

    @BeforeEach
    void setUp() {
        settingsService = mock(SettingsService.class);
        when(settingsService.isRedLineEnabled()).thenReturn(true);
        // 默认保护分支清单：main/master/develop + release/* 通配符
        when(settingsService.getProtectedBranches())
                .thenReturn(List.of("main", "master", "develop", "release/*"));
        rule = new ProtectedBranchRule(settingsService);
    }

    /**
     * force push 到 main 分支应被阻断。
     */
    @Test
    @DisplayName("force push 到 main 分支应返回 BLOCK")
    void shouldBlockForcePushToMain() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .branch("main")
                .force(true)
                .forceWithLease(false)
                .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.BLOCK, result.getAction());
        assertEquals(RedLineCode.RED_PROTECTED_BRANCH, result.getRuleCode());
        assertNotNull(result.getMessage());
    }

    /**
     * force push 到非保护分支应放行。
     */
    @Test
    @DisplayName("force push 到非保护分支应返回 PASS")
    void shouldPassForcePushToNonProtectedBranch() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .branch("feature/new-api")
                .force(true)
                .forceWithLease(false)
                .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * force push 到 release/* 通配符匹配的分支应被阻断。
     */
    @Test
    @DisplayName("force push 到 release/v1.0（匹配 release/*）应返回 BLOCK")
    void shouldBlockForcePushToReleaseWildcard() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .branch("release/v1.0")
                .force(true)
                .forceWithLease(false)
                .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.BLOCK, result.getAction());
        assertEquals(RedLineCode.RED_PROTECTED_BRANCH, result.getRuleCode());
    }

    /**
     * 普通推送（非 force）到保护分支应放行。
     * <p>规则仅拦截强制推送，正常推送不在拦截范围。</p>
     */
    @Test
    @DisplayName("普通推送（非 force）到保护分支应返回 PASS")
    void shouldPassNormalPushToProtectedBranch() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .branch("main")
                .force(false)
                .forceWithLease(false)
                .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * force-with-lease 推送到保护分支同样应被阻断。
     * <p>规则命中条件为 force 或 forceWithLease 之一为 true。</p>
     */
    @Test
    @DisplayName("force-with-lease 到 main 分支应返回 BLOCK")
    void shouldBlockForceWithLeaseToProtectedBranch() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .branch("main")
                .force(false)
                .forceWithLease(true)
                .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.BLOCK, result.getAction());
        assertEquals(RedLineCode.RED_PROTECTED_BRANCH, result.getRuleCode());
    }

    /**
     * 分支名带 refs/heads/ 前缀也应正确匹配（前缀会被去除）。
     */
    @Test
    @DisplayName("refs/heads/main 前缀应被去除并正确匹配")
    void shouldMatchAfterStrippingRefsHeadsPrefix() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .branch("refs/heads/develop")
                .force(true)
                .forceWithLease(false)
                .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.BLOCK, result.getAction());
    }

    /**
     * 分支为 null 时不命中，应放行。
     */
    @Test
    @DisplayName("branch 为 null 时应返回 PASS")
    void shouldPassWhenBranchIsNull() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .branch(null)
                .force(true)
                .forceWithLease(false)
                .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * 红线总开关关闭时，BLOCK 应降级为 CONFIRM（BR-30）。
     */
    @Test
    @DisplayName("红线关闭时保护分支 BLOCK 应降级为 CONFIRM")
    void shouldDowngradeToConfirmWhenRedLineDisabled() {
        when(settingsService.isRedLineEnabled()).thenReturn(false);

        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .branch("main")
                .force(true)
                .forceWithLease(false)
                .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.CONFIRM, result.getAction());
        assertEquals(RedLineCode.RED_PROTECTED_BRANCH, result.getRuleCode());
    }

    /**
     * 规则代码应返回 {@link RedLineCode#RED_PROTECTED_BRANCH}。
     */
    @Test
    @DisplayName("ruleCode 应返回 RED_PROTECTED_BRANCH")
    void shouldReturnCorrectRuleCode() {
        assertEquals(RedLineCode.RED_PROTECTED_BRANCH.name(), rule.ruleCode());
    }
}
