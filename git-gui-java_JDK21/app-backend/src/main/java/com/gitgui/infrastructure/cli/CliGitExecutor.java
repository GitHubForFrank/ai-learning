package com.gitgui.infrastructure.cli;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.domain.model.request.PullRequest;
import com.gitgui.domain.model.request.PushRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Git CLI 兜底执行器
 * <p>JGit 不支持或异常时降级为 CLI，覆盖 LFS / Hook / 复杂交互式 Rebase / Worktree / Submodule / filter-branch 等场景。</p>
 * <p>遵循 03-backend.md「直通优先策略」与 BR-42 编码约束。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class CliGitExecutor {

    private static final Logger log = LoggerFactory.getLogger(CliGitExecutor.class);

    /**
     * 通过 CLI 推送（兜底，用于子模块递归等场景）。
     *
     * @param req      推送请求
     * @param callback 进度回调
     */
    public void push(PushRequest req, ProgressCallback callback) {
        List<String> args = new java.util.ArrayList<>();
        args.add("push");
        if (req.isForce()) {
            args.add("--force");
        } else if (req.isForceWithLease()) {
            args.add("--force-with-lease");
        }
        if (req.isIncludeTags()) {
            args.add("--tags");
        }
        if (req.isPushAllBranches()) {
            args.add("--all");
        }
        if (req.isSetUpstream()) {
            args.add("-u");
        }
        if (req.getRemote() != null && !req.getRemote().isBlank()) {
            args.add(req.getRemote());
        }
        if (req.getBranch() != null && !req.getBranch().isBlank()) {
            args.add(req.getBranch());
        }
        GitProcessBuilder.execute(req.getRepoPath(), args, callback);
        log.info("CLI 推送完成：repoPath={}", req.getRepoPath());
    }

    /**
     * 通过 CLI 拉取（兜底）。
     *
     * @param req      拉取请求
     * @param callback 进度回调
     */
    public void pull(PullRequest req, ProgressCallback callback) {
        List<String> args = new java.util.ArrayList<>();
        args.add("pull");
        if (req.isAutoStash()) {
            args.add("--autostash");
        }
        if (req.isRebaseInsteadOfMerge()) {
            args.add("--rebase");
        }
        if (req.isFetchTags()) {
            args.add("--tags");
        }
        if (req.isAllBranches()) {
            args.add("--all");
        }
        if (req.isUpdateSubmodules()) {
            args.add("--recurse-submodules");
        }
        if (req.isDryRun()) {
            args.add("--dry-run");
        }
        if (req.getRemote() != null && !req.getRemote().isBlank()) {
            args.add(req.getRemote());
        }
        if (req.getBranch() != null && !req.getBranch().isBlank()) {
            args.add(req.getBranch());
        }
        GitProcessBuilder.execute(req.getRepoPath(), args, callback);
        log.info("CLI 拉取完成：repoPath={}", req.getRepoPath());
    }

    /**
     * 通过 CLI 执行子模块更新（PRD 4.17）。
     *
     * @param repoPath  仓库路径
     * @param recursive 是否递归
     * @param callback  进度回调
     */
    public void submoduleUpdate(String repoPath, boolean recursive, ProgressCallback callback) {
        List<String> args = new java.util.ArrayList<>();
        args.add("submodule");
        args.add("update");
        if (recursive) {
            args.add("--recursive");
        }
        GitProcessBuilder.execute(repoPath, args, callback);
    }

    /**
     * 通过 CLI 执行 LFS 安装（PRD 4.17）。
     *
     * @param repoPath 仓库路径
     */
    public void lfsInstall(String repoPath) {
        try {
            GitProcessBuilder.execute(repoPath, List.of("lfs", "install"), null);
        } catch (GitGuiException e) {
            log.warn("LFS 安装失败（可能未安装 git-lfs）：{}", e.getMessage());
        }
    }

    /**
     * 通过 CLI 执行 git gc（PRD 4.9.5）。
     *
     * @param repoPath 仓库路径
     * @param callback 进度回调
     */
    public void gc(String repoPath, ProgressCallback callback) {
        GitProcessBuilder.execute(repoPath, List.of("gc", "--prune=now"), callback);
        log.info("CLI GC 完成：repoPath={}", repoPath);
    }
}
