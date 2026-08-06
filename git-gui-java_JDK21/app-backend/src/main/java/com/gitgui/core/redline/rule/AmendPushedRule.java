package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.redline.AbstractRedLineRule;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import com.google.inject.Inject;

/**
 * amend 已推送提交规则（CONFIRM，BR-29）
 * <p>对已推送的提交执行 amend 将篡改共享历史，执行前弹窗确认。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class AmendPushedRule extends AbstractRedLineRule {

    /**
     * @param settingsService 设置服务
     */
    @Inject
    public AmendPushedRule(SettingsService settingsService) {
        super(settingsService);
    }

    @Override
    protected RedLineResult doCheck(RedLineContext ctx) {
        // 命中条件：amend 操作且目标提交已推送
        if (ctx.isAmend() && ctx.isPushed()) {
            return RedLineResult.confirm(RedLineCode.RED_AMEND_PUSHED, "amend 已推送的提交将篡改共享历史，可能导致他人冲突，请确认是否继续");
        }
        return RedLineResult.pass();
    }

    @Override
    public String ruleCode() {
        return RedLineCode.RED_AMEND_PUSHED.name();
    }
}
