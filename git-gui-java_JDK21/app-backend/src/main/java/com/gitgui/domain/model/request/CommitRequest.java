package com.gitgui.domain.model.request;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 提交请求（精简版）
 * <p>对应 PRD 4.4 Commit 对话框收集的参数，已简化：</p>
 * <ul>
 *   <li>移除 Amend Last Commit / GPG 签名 / 自定义作者等高级选项（UI 已移除）</li>
 *   <li>移除「new branch」分支（属于提交副作用，简洁起见不混合到提交请求里）</li>
 * </ul>
 * <p>遵循 BR-06（至少勾选一个文件）、BR-07（message 非空）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
public class CommitRequest {

    /**
     * 仓库路径
     */
    private String repoPath;

    /**
     * 勾选提交的文件列表（BR-06 至少一个）
     */
    private List<String> stagedFiles;

    /**
     * 提交信息（BR-07 去首尾空白后非空）
     */
    private String message;

    /**
     * 提交后是否推送（Commit & Push）
     */
    private boolean pushAfterCommit;

    /**
     * 提交后推送是否包含标签
     */
    private boolean pushWithTags;
}
