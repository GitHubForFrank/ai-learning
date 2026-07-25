package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.redline.AbstractRedLineRule;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import com.google.inject.Inject;

/**
 * 向保护分支 force push 规则（BLOCK，BR-26/BR-27）
 * <p>保护分支清单支持通配符（如 release/*），默认含 main/master/develop。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class ProtectedBranchRule extends AbstractRedLineRule {

    @Inject
    public ProtectedBranchRule(SettingsService settingsService) {
        super(settingsService);
    }

    @Override
    protected RedLineResult doCheck(RedLineContext ctx) {
        // 命中条件：force push（含 force-with-lease）到保护分支
        if ((ctx.isForce() || ctx.isForceWithLease()) && ctx.getBranch() != null) {
            var protectedBranches = settingsService.getProtectedBranches();
            if (matchesProtected(ctx.getBranch(), protectedBranches)) {
                return RedLineResult.block(RedLineCode.RED_PROTECTED_BRANCH,
                        "禁止向保护分支 " + ctx.getBranch() + " 强制推送（篡改主干历史）");
            }
        }
        return RedLineResult.pass();
    }

    @Override
    public String ruleCode() {
        return RedLineCode.RED_PROTECTED_BRANCH.name();
    }
}
