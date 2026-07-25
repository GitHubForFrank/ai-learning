package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.redline.AbstractRedLineRule;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import com.google.inject.Inject;

/**
 * rebase 已推送提交规则（CONFIRM，BR-29/BR-30）
 * <p>对已推送的提交执行 rebase 将篡改共享历史，执行前弹窗确认。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class RebasePushedRule extends AbstractRedLineRule {

    @Inject
    public RebasePushedRule(SettingsService settingsService) {
        super(settingsService);
    }

    @Override
    protected RedLineResult doCheck(RedLineContext ctx) {
        // 命中条件：rebase 操作且目标提交已推送
        if (ctx.getOperation() == com.gitgui.core.constant.OperationType.REBASE && ctx.isPushed()) {
            return RedLineResult.confirm(RedLineCode.RED_REBASE_PUSHED,
                    "rebase 已推送的提交将篡改共享历史，可能导致他人冲突，请确认是否继续");
        }
        return RedLineResult.pass();
    }

    @Override
    public String ruleCode() {
        return RedLineCode.RED_REBASE_PUSHED.name();
    }
}
