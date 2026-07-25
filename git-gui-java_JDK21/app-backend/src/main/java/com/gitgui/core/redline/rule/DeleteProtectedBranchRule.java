package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.redline.AbstractRedLineRule;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import com.google.inject.Inject;

/**
 * push 删除保护分支规则（BLOCK，BR-26）
 * <p>通过 push :branch 删除保护分支时阻断。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class DeleteProtectedBranchRule extends AbstractRedLineRule {

    @Inject
    public DeleteProtectedBranchRule(SettingsService settingsService) {
        super(settingsService);
    }

    @Override
    protected RedLineResult doCheck(RedLineContext ctx) {
        // 命中条件：删除远程分支且目标分支为保护分支
        if (ctx.isDeleteRemoteBranch() && ctx.getBranch() != null) {
            var protectedBranches = settingsService.getProtectedBranches();
            if (matchesProtected(ctx.getBranch(), protectedBranches)) {
                return RedLineResult.block(RedLineCode.RED_DELETE_PROTECTED_BRANCH,
                        "禁止通过 push :branch 删除保护分支 " + ctx.getBranch() + "（误删主干）");
            }
        }
        return RedLineResult.pass();
    }

    @Override
    public String ruleCode() {
        return RedLineCode.RED_DELETE_PROTECTED_BRANCH.name();
    }
}
