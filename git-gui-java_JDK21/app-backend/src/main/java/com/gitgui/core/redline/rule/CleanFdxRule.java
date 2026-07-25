package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.redline.AbstractRedLineRule;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import com.google.inject.Inject;

/**
 * clean -fdx 规则（CONFIRM，BR-29）
 * <p>含忽略文件的清理将删除 .gitignore 内的配置/密钥，执行前弹窗确认。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class CleanFdxRule extends AbstractRedLineRule {

    @Inject
    public CleanFdxRule(SettingsService settingsService) {
        super(settingsService);
    }

    @Override
    protected RedLineResult doCheck(RedLineContext ctx) {
        // 命中条件：clean -fdx（含忽略文件）
        if (ctx.isCleanIncludeIgnored()) {
            return RedLineResult.confirm(RedLineCode.RED_CLEAN_FDX,
                    "clean -fdx 将删除 .gitignore 内的配置/密钥文件，请确认是否继续");
        }
        return RedLineResult.pass();
    }

    @Override
    public String ruleCode() {
        return RedLineCode.RED_CLEAN_FDX.name();
    }
}
