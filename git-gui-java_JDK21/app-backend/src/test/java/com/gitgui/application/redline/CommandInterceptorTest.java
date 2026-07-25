package com.gitgui.application.redline;

import com.gitgui.core.constant.OperationType;
import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.exception.RedLineBlockedException;
import com.gitgui.domain.model.AuditLog;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.CommandRedLineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 命令拦截器单元测试（BR-26~BR-31）
 * <p>验证 {@link CommandInterceptor} 对三态结果（PASS/CONFIRM/BLOCK）的处理：
 * PASS 放行、CONFIRM 返回交由 UI 处理、BLOCK 抛 {@link RedLineBlockedException} 并记录审计日志。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
class CommandInterceptorTest {

    /** 被测拦截器依赖的红线服务 mock */
    private CommandRedLineService redLineService;

    /** 被测拦截器实例 */
    private CommandInterceptor interceptor;

    @BeforeEach
    void setUp() {
        redLineService = mock(CommandRedLineService.class);
        interceptor = new CommandInterceptor(redLineService);
    }

    /**
     * PASS 结果应直接返回，且不记录审计日志。
     */
    @Test
    @DisplayName("PASS 结果应直接返回且不记录审计")
    void shouldReturnPassDirectly() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .command("git push")
                .build();
        RedLineResult passResult = RedLineResult.pass();
        when(redLineService.check(ctx)).thenReturn(passResult);

        RedLineResult result = interceptor.intercept(ctx);

        assertEquals(RedLineResult.Action.PASS, result.getAction());
        // PASS 不应记录审计日志
        verify(redLineService, never()).recordAudit(any());
    }

    /**
     * BLOCK 结果应抛出 {@link RedLineBlockedException}，并记录 BLOCKED 审计日志。
     */
    @Test
    @DisplayName("BLOCK 结果应抛出 RedLineBlockedException 并记录 BLOCKED 审计")
    void shouldThrowOnBlock() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .command("git push --force")
                .branch("main")
                .repoPath("/repo")
                .remoteUrl("https://github.com/org/repo.git")
                .build();
        RedLineResult blockResult = RedLineResult.block(RedLineCode.RED_FORCE_PUSH,
                "禁止使用裸 --force 推送");
        when(redLineService.check(ctx)).thenReturn(blockResult);

        // 应抛出阻断异常
        RedLineBlockedException ex = assertThrows(RedLineBlockedException.class,
                () -> interceptor.intercept(ctx));
        assertEquals(RedLineCode.RED_FORCE_PUSH.name(), ex.getRuleCode());

        // 应记录 BLOCKED 审计日志
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(redLineService).recordAudit(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals(RedLineCode.RED_FORCE_PUSH.name(), audit.getRuleCode());
        assertEquals("git push --force", audit.getCommand());
        assertEquals("main", audit.getBranch());
        assertEquals("/repo", audit.getRepoPath());
        assertEquals("https://github.com/org/repo.git", audit.getRemoteUrl());
        assertEquals(RedLineResult.Action.BLOCK.name(), audit.getAction());
        assertEquals("BLOCKED", audit.getActionResult());
    }

    /**
     * CONFIRM 结果应返回交由 UI 处理，且不记录审计日志（审计在 onConfirm/onCancel 时记录）。
     */
    @Test
    @DisplayName("CONFIRM 结果应返回交由 UI 处理且不记录审计")
    void shouldReturnConfirmForUiHandling() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.RESET)
                .command("git reset --hard HEAD~1")
                .build();
        RedLineResult confirmResult = RedLineResult.confirm(RedLineCode.RED_RESET_HARD,
                "硬重置将丢失本地修改");
        when(redLineService.check(ctx)).thenReturn(confirmResult);

        RedLineResult result = interceptor.intercept(ctx);

        assertEquals(RedLineResult.Action.CONFIRM, result.getAction());
        assertEquals(RedLineCode.RED_RESET_HARD, result.getRuleCode());
        // CONFIRM 在 intercept 阶段不应记录审计
        verify(redLineService, never()).recordAudit(any());
    }

    /**
     * 用户确认 CONFIRM 后应记录 CONFIRMED 审计日志。
     */
    @Test
    @DisplayName("onConfirm 应记录 CONFIRMED 审计")
    void shouldRecordConfirmedOnConfirm() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.RESET)
                .command("git reset --hard HEAD~1")
                .branch("main")
                .build();
        RedLineResult confirmResult = RedLineResult.confirm(RedLineCode.RED_RESET_HARD,
                "硬重置将丢失本地修改");

        interceptor.onConfirm(ctx, confirmResult);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(redLineService).recordAudit(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals(RedLineCode.RED_RESET_HARD.name(), audit.getRuleCode());
        assertEquals(RedLineResult.Action.CONFIRM.name(), audit.getAction());
        assertEquals("CONFIRMED", audit.getActionResult());
    }

    /**
     * 用户取消 CONFIRM 后应记录 CANCELLED 审计日志。
     */
    @Test
    @DisplayName("onCancel 应记录 CANCELLED 审计")
    void shouldRecordCancelledOnCancel() {
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.RESET)
                .command("git reset --hard HEAD~1")
                .build();
        RedLineResult confirmResult = RedLineResult.confirm(RedLineCode.RED_RESET_HARD,
                "硬重置将丢失本地修改");

        interceptor.onCancel(ctx, confirmResult);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(redLineService).recordAudit(captor.capture());
        AuditLog audit = captor.getValue();
        assertEquals(RedLineCode.RED_RESET_HARD.name(), audit.getRuleCode());
        assertEquals(RedLineResult.Action.CONFIRM.name(), audit.getAction());
        assertEquals("CANCELLED", audit.getActionResult());
    }
}
