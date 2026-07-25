package com.gitgui.domain.model.request;

import lombok.Builder;
import lombok.Data;

/**
 * 推送请求
 * <p>对应 PRD 4.5.2 Push 对话框收集的参数。</p>
 * <p>遵循 BR-09（必选 Remote 与分支）、BR-10（推送前红线校验）、BR-11（默认隐藏裸 --force）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
public class PushRequest {

    /** 仓库路径 */
    private String repoPath;

    /** Remote 名称（BR-09 必选） */
    private String remote;

    /** 目标分支（BR-09 必选） */
    private String branch;

    /** 是否设置上游跟踪 */
    private boolean setUpstream;

    /** 是否 Force with lease（BR-11 默认仅暴露此选项） */
    private boolean forceWithLease;

    /** 是否裸 --force（BR-11 仅高级模式可见，仍走阻断红线） */
    private boolean force;

    /** 是否包含标签 */
    private boolean includeTags;

    /** 是否推送所有分支 */
    private boolean pushAllBranches;

    /** 是否推送所有标签 */
    private boolean pushAllTags;

    /** 临时目标 URL（不修改本地配置） */
    private String pushToUrl;

    /** 是否递归推送子模块 */
    private boolean recursiveSubmodules;

    /** 是否删除远程分支（push :branch） */
    private boolean deleteRemoteBranch;
}
