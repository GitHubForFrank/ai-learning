package com.gitgui.domain.service;

import java.util.List;

/**
 * 冲突解决服务接口
 * <p>关联 BR：BR-24。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface ConflictResolveService {

    /**
     * 用本地版本解决冲突。
     *
     * @param repoPath 仓库路径
     * @param paths    冲突文件列表
     */
    void resolveMine(String repoPath, List<String> paths);

    /**
     * 用远程版本解决冲突。
     *
     * @param repoPath 仓库路径
     * @param paths    冲突文件列表
     */
    void resolveTheirs(String repoPath, List<String> paths);

    /**
     * 调用外部 Merge 工具。
     *
     * @param repoPath 仓库路径
     * @param path     冲突文件路径
     * @throws com.gitgui.core.exception.GitGuiException MERGE_TOOL_NOT_CONFIGURED 未配置
     */
    void launchMergeTool(String repoPath, String path);

    /**
     * 标记冲突已解决。
     *
     * @param repoPath 仓库路径
     * @param paths    文件列表
     */
    void markResolved(String repoPath, List<String> paths);
}
