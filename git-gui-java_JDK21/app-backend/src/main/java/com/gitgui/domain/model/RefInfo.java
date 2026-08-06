package com.gitgui.domain.model;

import lombok.Builder;
import lombok.Data;

/**
 * 引用信息（ref + 对应 commit 元信息）
 * <p>用于 BrowseReferencesDialog 表格展示：一次查询所有 ref，附带每个 ref 指向 commit 的
 * SHA-1、提交时间、作者、message，避免每个 ref 单独打开仓库。</p>
 *
 * @author FrankKang
 * @since 2026-07-25
 */
@Data
@Builder
public class RefInfo {

    /**
     * 完整 ref 名（refs/heads/main、refs/remotes/origin/main、refs/tags/v1.0.0）
     */
    private String refName;

    /**
     * 显示名（剥离 refs/heads/ refs/remotes/<remote>/ refs/tags/ 前缀）
     */
    private String displayName;

    /**
     * 引用类型：BRANCH / TAG / REMOTE
     */
    private String kind;

    /**
     * 远程名（仅 REMOTE 类型，如 origin；本地分支 / tag 为空串）
     */
    private String remoteName;

    /**
     * 跟踪的远程分支显示名（仅本地分支且配置了上游时填，否则空串）
     */
    private String trackedBranch;

    /**
     * HEAD 指向的提交 SHA-1（40 字符十六进制）
     */
    private String sha;

    /**
     * 提交时间（ISO 字符串，便于直接展示；未解析为空串）
     */
    private String commitDate;

    /**
     * 提交作者（未解析为空串）
     */
    private String author;

    /**
     * 提交 message 第一行（未解析为空串）
     */
    private String message;

    /**
     * 短 SHA（前 8 字符）。
     */
    public String getShortSha() {
        return sha == null || sha.length() < 8 ? sha : sha.substring(0, 8);
    }
}
