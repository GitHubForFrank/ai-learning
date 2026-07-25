package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.redline.AbstractRedLineRule;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import com.google.inject.Inject;

/**
 * 裸 --force push 规则（BLOCK，BR-26）
 * <p>命中即拦截阻断，引导改用 {@code --force-with-lease}。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class ForcePushRule extends AbstractRedLineRule {

    @Inject
    public ForcePushRule(SettingsService settingsService) {
        super(settingsService);
    }

    @Override
    protected RedLineResult doCheck(RedLineContext ctx) {
        // 命中条件：推送操作且使用裸 --force（非 force-with-lease）
        if (ctx.isForce() && !ctx.isForceWithLease()) {
            return RedLineResult.block(RedLineCode.RED_FORCE_PUSH,
                    "禁止使用裸 --force 推送（覆盖远程历史），请改用 --force-with-lease（安全检查的强制推送）");
        }
        return RedLineResult.pass();
    }

    @Override
    public String ruleCode() {
        return RedLineCode.RED_FORCE_PUSH.name();
    }
}
