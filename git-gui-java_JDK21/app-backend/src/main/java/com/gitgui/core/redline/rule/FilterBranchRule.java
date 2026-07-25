package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.redline.AbstractRedLineRule;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import com.google.inject.Inject;

/**
 * filter-branch / filter-repo 规则（CONFIRM，BR-29）
 * <p>重写历史，执行前弹窗确认。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class FilterBranchRule extends AbstractRedLineRule {

    @Inject
    public FilterBranchRule(SettingsService settingsService) {
        super(settingsService);
    }

    @Override
    protected RedLineResult doCheck(RedLineContext ctx) {
        // 命中条件：使用 filter-branch / filter-repo
        if (ctx.isFilterBranch()) {
            return RedLineResult.confirm(RedLineCode.RED_FILTER_BRANCH,
                    "filter-branch / filter-repo 将重写历史，可能删除他人提交，请确认是否继续");
        }
        return RedLineResult.pass();
    }

    @Override
    public String ruleCode() {
        return RedLineCode.RED_FILTER_BRANCH.name();
    }
}
