package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.redline.AbstractRedLineRule;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import com.google.inject.Inject;

/**
 * reset --hard 规则（CONFIRM，BR-29）
 * <p>执行前弹窗确认，提示将丢失的文件清单。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class ResetHardRule extends AbstractRedLineRule {

    @Inject
    public ResetHardRule(SettingsService settingsService) {
        super(settingsService);
    }

    @Override
    protected RedLineResult doCheck(RedLineContext ctx) {
        // 命中条件：reset --hard 模式
        if ("HARD".equalsIgnoreCase(ctx.getResetMode())) {
            return RedLineResult.confirm(RedLineCode.RED_RESET_HARD,
                    "reset --hard 将丢失本地未提交修改，请确认是否继续");
        }
        return RedLineResult.pass();
    }

    @Override
    public String ruleCode() {
        return RedLineCode.RED_RESET_HARD.name();
    }
}
