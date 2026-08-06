package com.gitgui.application.redline;

import com.gitgui.core.exception.RedLineBlockedException;
import com.gitgui.core.util.JsonUtil;
import com.gitgui.domain.model.AuditLog;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.CommandRedLineService;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 命令拦截器
 * <p>{@code GitOperationService} 所有写方法执行前由本拦截器调用 {@code CommandRedLineService.check(ctx)}。</p>
 * <p>处理三态结果：</p>
 * <ul>
 *   <li>PASS → 放行执行</li>
 *   <li>BLOCK → 抛 {@link RedLineBlockedException}，记录 audit_log（actionResult=BLOCKED）</li>
 *   <li>CONFIRM → 由 UI 弹窗，用户确认后回调执行并记录 audit_log（CONFIRMED），取消则记录 CANCELLED</li>
 * </ul>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class CommandInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CommandInterceptor.class);

    private final CommandRedLineService redLineService;

    @Inject
    public CommandInterceptor(CommandRedLineService redLineService) {
        this.redLineService = redLineService;
    }

    /**
     * 执行拦截校验。
     * <p>BLOCK 直接抛异常；CONFIRM 需 UI 层处理（本方法返回 CONFIRM 结果）；PASS 放行。</p>
     *
     * @param ctx 红线上下文
     * @return 校验结果（PASS 或 CONFIRM；BLOCK 时抛异常）
     * @throws RedLineBlockedException 命中阻断类红线
     */
    public RedLineResult intercept(RedLineContext ctx) {
        RedLineResult result = redLineService.check(ctx);
        switch (result.getAction()) {
            case PASS:
                return result;
            case BLOCK:
                // 记录审计日志（BLOCKED）
                recordAudit(ctx, result, "BLOCKED");
                // 防御性 null 检查：BLOCK 结果正常情况下 ruleCode 不会为 null，但避免极端场景 NPE
                String ruleCodeName = result.getRuleCode() == null ? "UNKNOWN" : result.getRuleCode()
                                                                                       .name();
                throw new RedLineBlockedException(ruleCodeName, result.getMessage());
            case CONFIRM:
                // 返回 CONFIRM 由 UI 弹窗，UI 确认后调用 onConfirm，取消调用 onCancel
                return result;
            default:
                return result;
        }
    }

    /**
     * 用户确认 CONFIRM 后调用（记录 CONFIRMED，放行执行）。
     *
     * @param ctx    上下文
     * @param result 原校验结果
     */
    public void onConfirm(RedLineContext ctx, RedLineResult result) {
        recordAudit(ctx, result, "CONFIRMED");
    }

    /**
     * 用户取消 CONFIRM 后调用（记录 CANCELLED）。
     *
     * @param ctx    上下文
     * @param result 原校验结果
     */
    public void onCancel(RedLineContext ctx, RedLineResult result) {
        recordAudit(ctx, result, "CANCELLED");
    }

    /**
     * 记录审计日志（BR-31）。
     *
     * @param ctx          上下文
     * @param result       校验结果
     * @param actionResult 处理结果（BLOCKED/CONFIRMED/CANCELLED）
     */
    private void recordAudit(RedLineContext ctx, RedLineResult result, String actionResult) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("command", ctx.getCommand());
        detail.put("branch", ctx.getBranch());
        detail.put("remoteUrl", ctx.getRemoteUrl());
        if (result.getDetail() != null) {
            detail.put("ruleDetail", result.getDetail());
        }
        AuditLog auditLog = AuditLog.builder()
                                    .ruleCode(result.getRuleCode() == null ? "UNKNOWN" : result.getRuleCode()
                                                                                               .name())
                                    .command(ctx.getCommand())
                                    .repoPath(ctx.getRepoPath() == null ? "" : ctx.getRepoPath())
                                    .branch(ctx.getBranch() == null ? "" : ctx.getBranch())
                                    .remoteUrl(ctx.getRemoteUrl() == null ? "" : ctx.getRemoteUrl())
                                    .action(result.getAction()
                                                  .name())
                                    .actionResult(actionResult)
                                    .detail(JsonUtil.toJson(detail))
                                    .build();
        redLineService.recordAudit(auditLog);
    }
}
