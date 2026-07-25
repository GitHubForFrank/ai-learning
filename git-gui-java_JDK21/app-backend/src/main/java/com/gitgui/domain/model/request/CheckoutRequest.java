package com.gitgui.domain.model.request;

import lombok.Builder;
import lombok.Data;

/**
 * 切换（Checkout）请求
 * <p>对应 PRD 4.6.1，参照 TortoiseGit Switch/Checkout 对话框设计：</p>
 * <ul>
 *   <li>目标支持分支 / 标签 / commit 三种类型（{@link TargetType}）</li>
 *   <li>选项覆盖：创建新分支、强制切换、设置跟踪分支、覆盖已有分支、merge</li>
 * </ul>
 *
 * <p><b>refName 优先级最高：</b>当用户从 {@link com.gitgui.ui.dialog.BrowseReferencesDialog} 选中某个
 * ref（如远程分支 {@code refs/remotes/origin/feature/x}）时，UI 层会把完整 ref 路径写入
 * {@code refName} 字段，Service 层优先按 refName 执行 checkout，避免剥离前缀后无法解析为 ref。</p>
 *
 * @author FrankKang
 * @since 2026-07-24
 */
@Data
@Builder
public class CheckoutRequest {

    /**
     * 目标类型（分支 / 标签 / commit）
     */
    public enum TargetType {
        BRANCH,
        TAG,
        COMMIT
    }

    /** 仓库路径 */
    private String repoPath;

    /** 目标类型：BRANCH / TAG / COMMIT */
    private TargetType targetType;

    /**
     * 完整 ref 名（含 {@code refs/heads/} / {@code refs/remotes/<remote>/} / {@code refs/tags/} 前缀）。
     * <p>优先级最高：当 UI 层从 refs 浏览对话框选中某个 ref 时，携带完整路径传给 Service 层，</p>
     * <p>避免剥离前缀后无法被 JGit 解析（典型场景：远程分支 {@code refs/remotes/origin/feature/x}）。</p>
     */
    private String refName;

    /** 目标分支名（如 main / develop / feature/login），targetType=BRANCH 时必填 */
    private String branch;

    /** 目标 tag 名（如 v1.0.0），targetType=TAG 时必填 */
    private String tag;

    /** 目标 commit 哈希（完整或短哈希），targetType=COMMIT 时必填 */
    private String commitId;

    /** 创建新分支名（create=true 时必填，等价于 git checkout -b） */
    private String newBranch;

    /** 是否创建新分支（与 newBranch 配对使用） */
    private boolean create;

    /** 是否强制切换（丢弃工作区修改，等价于 git checkout -f） */
    private boolean force;

    /** 是否建立跟踪分支（远程 → 本地，等价于 git checkout --track origin/x） */
    private boolean track;

    /** 是否覆盖已存在的同名分支（git checkout -B） */
    private boolean overrideExisting;

    /** 切换后是否自动 merge（与 force 互斥时一般用于「拉取式切换」） */
    private boolean merge;
}