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
 * 推送超大文件规则（CONFIRM，BR-29）
 * <p>非 LFS 文件超过阈值（默认 50MB）时弹窗确认，建议走 LFS。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class LargeFileRule extends AbstractRedLineRule {

    @Inject
    public LargeFileRule(SettingsService settingsService) {
        super(settingsService);
    }

    @Override
    protected RedLineResult doCheck(RedLineContext ctx) {
        // 命中条件：暂存区含超大文件（非 LFS）
        if (ctx.getStagedFilesWithSize() == null || ctx.getStagedFilesWithSize().isEmpty()) {
            return RedLineResult.pass();
        }
        int thresholdMb = settingsService.getLargeFileThresholdMb();
        long thresholdBytes = thresholdMb * 1024L * 1024L;
        List<String> hits = new ArrayList<>();
        for (RedLineContext.StagedFile file : ctx.getStagedFilesWithSize()) {
            if (file.getSize() > thresholdBytes) {
                long sizeMb = file.getSize() / (1024L * 1024L);
                hits.add(file.getPath() + "（" + sizeMb + "MB）");
            }
        }
        if (!hits.isEmpty()) {
            return RedLineResult.confirm(RedLineCode.RED_LARGE_FILE,
                    "推送超大文件（> " + thresholdMb + "MB，非 LFS），建议走 LFS：" + String.join("、", hits))
                    .toBuilder()
                    .detail(JsonUtil.toJson(hits))
                    .build();
        }
        return RedLineResult.pass();
    }

    @Override
    public String ruleCode() {
        return RedLineCode.RED_LARGE_FILE.name();
    }
}
