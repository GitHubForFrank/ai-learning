package com.gitgui.application.service;

import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.domain.service.ConflictResolveService;
import com.gitgui.domain.service.SettingsService;
import com.gitgui.infrastructure.cli.GitProcessBuilder;
import com.google.inject.Inject;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 冲突解决服务实现
 * <p>关联 BR：BR-24。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class ConflictResolveServiceImpl implements ConflictResolveService {

    private static final Logger log = LoggerFactory.getLogger(ConflictResolveServiceImpl.class);

    private final SettingsService settingsService;

    @Inject
    public ConflictResolveServiceImpl(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public void resolveMine(String repoPath, List<String> paths) {
        // 全部保留本地版本：git checkout --ours <paths>
        for (String path : paths) {
            GitProcessBuilder.execute(repoPath, List.of("checkout", "--ours", path), null);
        }
        markResolved(repoPath, paths);
        log.info("冲突解决（用本地版本）：{}", paths);
    }

    @Override
    public void resolveTheirs(String repoPath, List<String> paths) {
        // 全部保留远程版本：git checkout --theirs <paths>
        for (String path : paths) {
            GitProcessBuilder.execute(repoPath, List.of("checkout", "--theirs", path), null);
        }
        markResolved(repoPath, paths);
        log.info("冲突解决（用远程版本）：{}", paths);
    }

    @Override
    public void launchMergeTool(String repoPath, String path) {
        // PRD 4.15：优先调用外部 Merge 工具，未配置时使用内置合并器
        String mergeTool = settingsService.get("external.merge_tool");
        if (mergeTool == null || mergeTool.isBlank()) {
            throw new GitGuiException(ErrorCode.MERGE_TOOL_NOT_CONFIGURED, "未配置外部合并工具，请在 Settings 中配置 external.merge_tool");
        }
        GitProcessBuilder.execute(repoPath, List.of("mergetool", "--tool=" + mergeTool, path), null);
    }

    @Override
    public void markResolved(String repoPath, List<String> paths) {
        // 标记冲突已解决：git add <paths>
        for (String path : paths) {
            GitProcessBuilder.execute(repoPath, List.of("add", path), null);
        }
        log.info("标记冲突已解决：{}", paths);
    }
}
