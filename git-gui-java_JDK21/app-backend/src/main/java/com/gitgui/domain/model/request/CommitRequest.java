package com.gitgui.domain.model.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提交请求
 * <p>对应 PRD 4.4 Commit 对话框收集的参数，参照 TortoiseGit 布局扩展。</p>
 * <p>遵循 BR-06（至少勾选一个文件）、BR-07（message 非空）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
public class CommitRequest {

    /** 仓库路径 */
    private String repoPath;

    /** 勾选提交的文件列表（BR-06 至少一个） */
    private List<String> stagedFiles;

    /** 提交信息（BR-07 去首尾空白后非空） */
    private String message;

    /** 是否 Amend Last Commit */
    private boolean amend;

    /** 是否使用自定义作者 */
    private boolean customAuthor;

    /** 是否设置作者时间 */
    private boolean setAuthorDate;

    /** 自定义作者时间（setAuthorDate=true 时生效） */
    private LocalDateTime authorDate;

    /** 自定义作者 */
    private String author;

    /** 是否 GPG 签名提交 */
    private boolean signCommit;

    /** 提交后是否推送（Commit & Push） */
    private boolean pushAfterCommit;

    /** 提交后推送是否包含标签 */
    private boolean pushWithTags;

    /** 是否复用上次提交信息（ReCommit，message 不再从对话框收集） */
    private boolean reuseLastMessage;

    /** 是否在提交前创建并切换到新分支（TortoiseGit「new branch」） */
    private boolean createNewBranch;

    /** 新分支名（createNewBranch=true 时必填） */
    private String newBranchName;
}
