package com.gitgui.domain.redline;

import com.gitgui.core.constant.OperationType;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 红线校验上下文
 * <p>携带写操作的全部相关信息，供 13 个 {@link RedLineRule} 进行命中判定。</p>
 * <p>字段含义参见 02-api.md {@code CommandRedLineService.check} 注释。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
public class RedLineContext {

    /**
     * 操作类型
     */
    private OperationType operation;

    /**
     * 实际命令（如 "git push --force"）
     */
    private String command;

    /**
     * 仓库路径
     */
    private String repoPath;

    /**
     * 涉及分支
     */
    private String branch;

    /**
     * 涉及远程 URL
     */
    private String remoteUrl;

    /**
     * 暂存区文件清单（敏感文件扫描用，BR-32）
     */
    private List<String> stagedFiles;

    /**
     * 暂存区文件大小映射（字节，超大文件检测用，BR-29）
     */
    private List<StagedFile> stagedFilesWithSize;

    /**
     * 目标提交（reset/rebase 用）
     */
    private String targetCommit;

    /**
     * 目标提交是否已推送（amend/rebase 二次确认用，BR-29）
     */
    private boolean pushed;

    /**
     * 是否使用 --no-verify（BR-26）
     */
    private boolean noVerify;

    /**
     * 是否 amend 提交（BR-29）
     */
    private boolean amend;

    /**
     * 是否裸 --force（BR-26）
     */
    private boolean force;

    /**
     * 是否 force with lease（BR-11 默认暴露）
     */
    private boolean forceWithLease;

    /**
     * 是否删除远程分支
     */
    private boolean deleteRemoteBranch;

    /**
     * 重置模式（reset --hard 时为 HARD，BR-29）
     */
    private String resetMode;

    /**
     * 是否 clean -fdx（含忽略文件）
     */
    private boolean cleanIncludeIgnored;

    /**
     * 是否 filter-branch / filter-repo
     */
    private boolean filterBranch;

    /**
     * 暂存文件（含大小）。
     */
    @Data
    @Builder
    public static class StagedFile {

        /**
         * 文件路径
         */
        private String path;
        /**
         * 文件大小（字节）
         */
        private long size;
    }
}
