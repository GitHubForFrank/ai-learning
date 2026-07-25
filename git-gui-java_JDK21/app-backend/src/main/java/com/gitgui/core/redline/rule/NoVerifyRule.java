package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.redline.AbstractRedLineRule;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import com.google.inject.Inject;

/**
 * --no-verify 跳过 hook 规则（BLOCK，BR-26）
 * <p>使用 --no-verify 跳过公司提交校验即阻断。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class NoVerifyRule extends AbstractRedLineRule {

    @Inject
    public NoVerifyRule(SettingsService settingsService) {
        super(settingsService);
    }

    @Override
    protected RedLineResult doCheck(RedLineContext ctx) {
        // 命中条件：使用 --no-verify
        if (ctx.isNoVerify()) {
            return RedLineResult.block(RedLineCode.RED_NO_VERIFY,
                    "禁止使用 --no-verify 跳过 hook 校验（绕过公司提交校验）");
        }
        return RedLineResult.pass();
    }

    @Override
    public String ruleCode() {
        return RedLineCode.RED_NO_VERIFY.name();
    }
}
