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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 推送到非授权远程规则单元测试（BR-26/BR-28）
 * <p>验证 {@link RemoteWhitelistRule} 对远程白名单（含域名通配）的命中判定，
 * 覆盖 https/ssh 协议、空白名单不限制、非授权远程阻断等场景。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
class RemoteWhitelistRuleTest {

    /**
     * 被测规则依赖的设置服务 mock
     */
    private SettingsService settingsService;

    /**
     * 被测规则实例
     */
    private RemoteWhitelistRule rule;

    @BeforeEach
    void setUp() {
        settingsService = mock(SettingsService.class);
        when(settingsService.isRedLineEnabled()).thenReturn(true);
        // 默认白名单：仅允许 github.com 与 gitlab.com
        when(settingsService.getRemoteWhitelist()).thenReturn(List.of("github.com", "gitlab.com"));
        rule = new RemoteWhitelistRule(settingsService);
    }

    /**
     * 推送到白名单内的主机应放行（https 协议）。
     */
    @Test
    @DisplayName("推送到白名单主机应返回 PASS")
    void shouldPassWhitelistedHost() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.PUSH)
                                           .remoteUrl("https://github.com/org/repo.git")
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * 推送到非白名单主机应被阻断。
     */
    @Test
    @DisplayName("推送到非白名单主机应返回 BLOCK")
    void shouldBlockNonWhitelistedHost() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.PUSH)
                                           .remoteUrl("https://evil.com/repo.git")
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.BLOCK, result.getAction());
        assertEquals(RedLineCode.RED_REMOTE_WHITELIST, result.getRuleCode());
        assertNotNull(result.getMessage());
    }

    /**
     * SSH 协议（git@host:path）也应正确提取 host 进行白名单匹配。
     */
    @Test
    @DisplayName("SSH 协议推送到白名单主机应返回 PASS")
    void shouldPassSshWhitelistedHost() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.PUSH)
                                           .remoteUrl("git@github.com:org/repo.git")
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * SSH 协议推送到非白名单主机应被阻断。
     */
    @Test
    @DisplayName("SSH 协议推送到非白名单主机应返回 BLOCK")
    void shouldBlockSshNonWhitelistedHost() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.PUSH)
                                           .remoteUrl("git@evil.com:org/repo.git")
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.BLOCK, result.getAction());
        assertEquals(RedLineCode.RED_REMOTE_WHITELIST, result.getRuleCode());
    }

    /**
     * 空白名单表示不限制，应放行（BR-28）。
     */
    @Test
    @DisplayName("空白名单应不限制，返回 PASS")
    void shouldPassWhenWhitelistEmpty() {
        when(settingsService.getRemoteWhitelist()).thenReturn(List.of());

        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.PUSH)
                                           .remoteUrl("https://anywhere.com/repo.git")
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * null 白名单应视为不限制，放行。
     */
    @Test
    @DisplayName("null 白名单应返回 PASS")
    void shouldPassWhenWhitelistNull() {
        when(settingsService.getRemoteWhitelist()).thenReturn(null);

        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.PUSH)
                                           .remoteUrl("https://anywhere.com/repo.git")
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * remoteUrl 为空应放行。
     */
    @Test
    @DisplayName("remoteUrl 为空应返回 PASS")
    void shouldPassWhenRemoteUrlBlank() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.PUSH)
                                           .remoteUrl("")
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * remoteUrl 为 null 应放行。
     */
    @Test
    @DisplayName("remoteUrl 为 null 应返回 PASS")
    void shouldPassWhenRemoteUrlNull() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.PUSH)
                                           .remoteUrl(null)
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * 完整 URL 出现在白名单时也应放行（白名单支持全 URL 匹配）。
     */
    @Test
    @DisplayName("白名单含完整 URL 时应返回 PASS")
    void shouldPassWhenWhitelistContainsFullUrl() {
        when(settingsService.getRemoteWhitelist()).thenReturn(List.of("https://github.com/org/repo.git"));

        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.PUSH)
                                           .remoteUrl("https://github.com/org/repo.git")
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * 红线总开关关闭时，BLOCK 应降级为 CONFIRM（BR-30）。
     */
    @Test
    @DisplayName("红线关闭时非授权远程 BLOCK 应降级为 CONFIRM")
    void shouldDowngradeToConfirmWhenRedLineDisabled() {
        when(settingsService.isRedLineEnabled()).thenReturn(false);

        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.PUSH)
                                           .remoteUrl("https://evil.com/repo.git")
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.CONFIRM, result.getAction());
        assertEquals(RedLineCode.RED_REMOTE_WHITELIST, result.getRuleCode());
    }

    /**
     * 规则代码应返回 {@link RedLineCode#RED_REMOTE_WHITELIST}。
     */
    @Test
    @DisplayName("ruleCode 应返回 RED_REMOTE_WHITELIST")
    void shouldReturnCorrectRuleCode() {
        assertEquals(RedLineCode.RED_REMOTE_WHITELIST.name(), rule.ruleCode());
    }
}
