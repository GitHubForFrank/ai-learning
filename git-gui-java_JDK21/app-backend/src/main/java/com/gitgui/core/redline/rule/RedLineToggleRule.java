package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.redline.AbstractRedLineRule;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import com.google.inject.Inject;

/**
 * 红线开关切换规则（CONFIRM，BR-30）
 * <p>关闭/开启红线总开关本身写入 audit_log 并弹窗确认。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class RedLineToggleRule extends AbstractRedLineRule {

    @Inject
    public RedLineToggleRule(SettingsService settingsService) {
        super(settingsService);
    }

    @Override
    protected RedLineResult doCheck(RedLineContext ctx) {
        // 命中条件：切换红线总开关（通过 command 标识）
        if ("RED_LINE_TOGGLE".equalsIgnoreCase(ctx.getCommand())) {
            return RedLineResult.confirm(RedLineCode.RED_LINE_TOGGLE,
                    "切换命令红线总开关将影响所有阻断/确认行为，请确认是否继续");
        }
        return RedLineResult.pass();
    }

    @Override
    public String ruleCode() {
        return RedLineCode.RED_LINE_TOGGLE.name();
    }
}
