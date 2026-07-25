package com.gitgui.domain.model.request;

import lombok.Builder;
import lombok.Data;

/**
 * 拉取请求
 * <p>对应 PRD 4.5.1 Pull 对话框收集的参数。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
public class PullRequest {

    /** 仓库路径 */
    private String repoPath;

    /** Remote 名称（BR-09 必选） */
    private String remote;

    /** 远程分支（BR-09 必选） */
    private String branch;

    /** 是否 AutoStash */
    private boolean autoStash;

    /** 是否变基而非合并 */
    private boolean rebaseInsteadOfMerge;

    /** 是否拉取标签 */
    private boolean fetchTags;

    /** 是否拉取所有分支 */
    private boolean allBranches;

    /** 是否递归更新子模块 */
    private boolean updateSubmodules;

    /** 是否预演 */
    private boolean dryRun;
}
