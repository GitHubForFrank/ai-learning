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
 * 推送含敏感信息文件规则单元测试（BR-26/BR-32）
 * <p>验证 {@link SensitiveFileRule} 对暂存区文件的敏感信息扫描行为，
 * 覆盖 .env、*.pem 等内置规则以及空暂存区场景。</p>
 * <p>注意：本测试与领域模型 {@code com.gitgui.domain.model.SensitiveFileRule} 同名，
 * 故领域模型在代码中以全限定名引用以避免冲突。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
class SensitiveFileRuleTest {

    /**
     * 被测规则依赖的设置服务 mock
     */
    private SettingsService settingsService;

    /**
     * 被测规则实例
     */
    private SensitiveFileRule rule;

    @BeforeEach
    void setUp() {
        settingsService = mock(SettingsService.class);
        when(settingsService.isRedLineEnabled()).thenReturn(true);
        // 内置默认敏感文件规则集（BR-32），使用全限定名避免与规则类同名冲突
        when(settingsService.getSensitiveFileRules()).thenReturn(List.of(com.gitgui.domain.model.SensitiveFileRule.builder()
                                                                                                                  .pattern("\\.env$")
                                                                                                                  .description("环境变量文件")
                                                                                                                  .build(),
                                                                         com.gitgui.domain.model.SensitiveFileRule.builder()
                                                                                                                  .pattern("\\.pem$")
                                                                                                                  .description("PEM 私钥文件")
                                                                                                                  .build(),
                                                                         com.gitgui.domain.model.SensitiveFileRule.builder()
                                                                                                                  .pattern("credentials")
                                                                                                                  .description("凭据文件")
                                                                                                                  .build(),
                                                                         com.gitgui.domain.model.SensitiveFileRule.builder()
                                                                                                                  .pattern("id_rsa")
                                                                                                                  .description("SSH 私钥")
                                                                                                                  .build()));
        rule = new SensitiveFileRule(settingsService);
    }

    /**
     * 暂存区含 .env 文件应被阻断。
     */
    @Test
    @DisplayName("提交 .env 文件应返回 BLOCK")
    void shouldBlockEnvFile() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.COMMIT)
                                           .stagedFiles(List.of(".env"))
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.BLOCK, result.getAction());
        assertEquals(RedLineCode.RED_SENSITIVE_FILE, result.getRuleCode());
        assertNotNull(result.getMessage());
        // 详情 JSON 应包含命中文件路径
        assertNotNull(result.getDetail());
    }

    /**
     * 暂存区仅含普通文件应放行。
     */
    @Test
    @DisplayName("提交普通文件应返回 PASS")
    void shouldPassNormalFile() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.COMMIT)
                                           .stagedFiles(List.of("README.md", "src/Main.java"))
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * 暂存区含 *.pem 文件应被阻断。
     */
    @Test
    @DisplayName("提交 *.pem 文件应返回 BLOCK")
    void shouldBlockPemFile() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.PUSH)
                                           .stagedFiles(List.of("src/main/resources/server.pem"))
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.BLOCK, result.getAction());
        assertEquals(RedLineCode.RED_SENSITIVE_FILE, result.getRuleCode());
    }

    /**
     * 暂存区为空应放行。
     */
    @Test
    @DisplayName("暂存区为空应返回 PASS")
    void shouldPassEmptyStagedFiles() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.COMMIT)
                                           .stagedFiles(List.of())
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * 暂存区为 null 应放行。
     */
    @Test
    @DisplayName("stagedFiles 为 null 应返回 PASS")
    void shouldPassNullStagedFiles() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.COMMIT)
                                           .stagedFiles(null)
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
    }

    /**
     * 暂存区同时含敏感文件与普通文件应被阻断（部分命中即拦截）。
     */
    @Test
    @DisplayName("暂存区混合文件含敏感文件应返回 BLOCK")
    void shouldBlockWhenMixedFilesContainSensitive() {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.COMMIT)
                                           .stagedFiles(List.of("README.md", "config/.env", "src/App.java"))
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.BLOCK, result.getAction());
        assertEquals(RedLineCode.RED_SENSITIVE_FILE, result.getRuleCode());
    }

    /**
     * 红线总开关关闭时，BLOCK 应降级为 CONFIRM（BR-30）。
     */
    @Test
    @DisplayName("红线关闭时敏感文件 BLOCK 应降级为 CONFIRM")
    void shouldDowngradeToConfirmWhenRedLineDisabled() {
        when(settingsService.isRedLineEnabled()).thenReturn(false);

        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.COMMIT)
                                           .stagedFiles(List.of(".env"))
                                           .build();

        RedLineResult result = rule.check(ctx);

        assertEquals(RedLineResult.Action.CONFIRM, result.getAction());
        assertEquals(RedLineCode.RED_SENSITIVE_FILE, result.getRuleCode());
    }

    /**
     * 规则代码应返回 {@link RedLineCode#RED_SENSITIVE_FILE}。
     */
    @Test
    @DisplayName("ruleCode 应返回 RED_SENSITIVE_FILE")
    void shouldReturnCorrectRuleCode() {
        assertEquals(RedLineCode.RED_SENSITIVE_FILE.name(), rule.ruleCode());
    }
}
