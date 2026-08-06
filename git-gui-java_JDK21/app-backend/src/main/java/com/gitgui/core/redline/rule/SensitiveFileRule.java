package com.gitgui.core.redline.rule;

import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.redline.AbstractRedLineRule;
import com.gitgui.core.util.JsonUtil;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.service.SettingsService;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * 推送含敏感信息文件规则（BLOCK，BR-26/BR-32）
 * <p>扫描暂存区文件名，命中内置或自定义敏感文件规则即阻断并提示加入 .gitignore。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class SensitiveFileRule extends AbstractRedLineRule {

    /**
     * @param settingsService 设置服务
     */
    @Inject
    public SensitiveFileRule(SettingsService settingsService) {
        super(settingsService);
    }

    @Override
    protected RedLineResult doCheck(RedLineContext ctx) {
        // 命中条件：Commit 或 Push 操作且暂存区含敏感文件
        if (ctx.getStagedFiles() == null || ctx.getStagedFiles()
                                               .isEmpty()) {
            return RedLineResult.pass();
        }
        var rules = settingsService.getSensitiveFileRules();
        List<String> hits = new ArrayList<>();
        for (String path : ctx.getStagedFiles()) {
            String desc = matchSensitiveFile(path, rules);
            if (desc != null) {
                hits.add(path + "（" + desc + "）");
            }
        }
        if (!hits.isEmpty()) {
            return RedLineResult.block(RedLineCode.RED_SENSITIVE_FILE, "推送含敏感信息文件，请移除并加入 .gitignore：" + String.join("、", hits))
                                .toBuilder()
                                .detail(JsonUtil.toJson(hits))
                                .build();
        }
        return RedLineResult.pass();
    }

    @Override
    public String ruleCode() {
        return RedLineCode.RED_SENSITIVE_FILE.name();
    }
}
