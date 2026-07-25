package com.gitgui.application.service;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.core.constant.OperationType;
import com.gitgui.core.constant.RedLineCode;
import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.core.util.JsonUtil;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.OperationLog;
import com.gitgui.domain.model.request.CheckoutRequest;
import com.gitgui.domain.model.request.CommitRequest;
import com.gitgui.domain.model.request.PullRequest;
import com.gitgui.domain.model.request.PushRequest;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.redline.RedLineResult;
import com.gitgui.domain.repository.OperationLogRepository;
import com.gitgui.domain.service.AsyncTaskService;
import com.gitgui.domain.service.GitOperationService;
import com.gitgui.application.redline.CommandInterceptor;
import com.gitgui.infrastructure.cli.CliGitExecutor;
import com.gitgui.infrastructure.jgit.JGitOperationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import com.google.inject.Inject;

/**
 * Git 操作服务实现（核心写操作）
 * <p>所有写方法执行前由 {@link CommandInterceptor} 自动调用红线校验。</p>
 * <p>关联 BR：BR-06~BR-25。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class GitOperationServiceImpl implements GitOperationService {

    private static final Logger log = LoggerFactory.getLogger(GitOperationServiceImpl.class);

    private final JGitOperationExecutor jgitExecutor;
    private final CliGitExecutor cliExecutor;
    private final CommandInterceptor commandInterceptor;
    private final AsyncTaskService asyncTaskService;
    private final OperationLogRepository operationLogRepository;

    @Inject
    public GitOperationServiceImpl(JGitOperationExecutor jgitExecutor,
                                   CliGitExecutor cliExecutor,
                                   CommandInterceptor commandInterceptor,
                                   AsyncTaskService asyncTaskService,
                                   OperationLogRepository operationLogRepository) {
        this.jgitExecutor = jgitExecutor;
        this.cliExecutor = cliExecutor;
        this.commandInterceptor = commandInterceptor;
        this.asyncTaskService = asyncTaskService;
        this.operationLogRepository = operationLogRepository;
    }

    @Override
    public String commit(CommitRequest req) {
        // BR-06：至少勾选一个文件
        if (req.getStagedFiles() == null || req.getStagedFiles().isEmpty()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请至少勾选一个变更文件（BR-06）");
        }
        // BR-07：提交信息非空（Amend 可复用上次 message，仍允许空以走 git commit --amend 默认行为）
        if (!req.isAmend() && (req.getMessage() == null || req.getMessage().trim().isEmpty())) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "提交信息不能为空（BR-07）");
        }
        // 红线拦截：扫描敏感文件（BR-32）+ amend 已推送确认（BR-29）
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.COMMIT)
                .command("git commit")
                .repoPath(req.getRepoPath())
                .stagedFiles(req.getStagedFiles())
                .amend(req.isAmend())
                .noVerify(false)
                .build();
        RedLineResult result = commandInterceptor.intercept(ctx);
        // CONFIRM 由 UI 层处理；PASS 放行执行
        long start = System.currentTimeMillis();
        try {
            String commitId = jgitExecutor.commit(req);
            recordLog(req.getRepoPath(), OperationType.COMMIT, "git commit", true, System.currentTimeMillis() - start, null, null);
            return commitId;
        } catch (Exception e) {
            recordLog(req.getRepoPath(), OperationType.COMMIT, "git commit", false, System.currentTimeMillis() - start, e.getMessage(), null);
            throw e instanceof GitGuiException ge ? ge : new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
        }
    }

    @Override
    public TaskHandle push(PushRequest req, ProgressCallback cb) {
        // BR-09：必选 Remote 与分支
        if (req.getRemote() == null || req.getRemote().isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请选择 Remote（BR-09）");
        }
        // BR-10：推送前红线校验
        // BR-10：推送前红线校验
        RedLineContext ctx = RedLineContext.builder()
                .operation(OperationType.PUSH)
                .command("git push")
                .repoPath(req.getRepoPath())
                .branch(req.getBranch())
                .force(req.isForce())
                .forceWithLease(req.isForceWithLease())
                .deleteRemoteBranch(req.isDeleteRemoteBranch())
                .build();
        RedLineResult result = commandInterceptor.intercept(ctx);
        // 异步执行推送
        return asyncTaskService.submitWrite(req.getRepoPath(), TaskType.PUSH, () -> {
            long start = System.currentTimeMillis();
            try {
                jgitExecutor.push(req, cb);
                recordLog(req.getRepoPath(), OperationType.PUSH, "git push", true, System.currentTimeMillis() - start, null, null);
            } catch (Exception e) {
                recordLog(req.getRepoPath(), OperationType.PUSH, "git push", false, System.currentTimeMillis() - start, e.getMessage(), null);
                throw e instanceof GitGuiException ge ? ge : new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
            }
        }, cb);
    }

    @Override
    public String pushViaCli(PushRequest req) {
        // BR-09：必选 Remote
        if (req.getRemote() == null || req.getRemote().isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请选择 Remote（BR-09）");
        }
        long start = System.currentTimeMillis();
        try {
            String output = jgitExecutor.pushViaCli(req);
            recordLog(req.getRepoPath(), OperationType.PUSH, "git push (CLI fallback)", true,
                    System.currentTimeMillis() - start, null, null);
            return output;
        } catch (Exception e) {
            recordLog(req.getRepoPath(), OperationType.PUSH, "git push (CLI fallback)", false,
                    System.currentTimeMillis() - start, e.getMessage(), null);
            throw e instanceof GitGuiException ge ? ge
                    : new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
        }
    }

    @Override
    public TaskHandle pull(PullRequest req, ProgressCallback cb) {
        if (req.getRemote() == null || req.getRemote().isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请选择 Remote（BR-09）");
        }
        return asyncTaskService.submitWrite(req.getRepoPath(), TaskType.PULL, () -> {
            long start = System.currentTimeMillis();
            try {
                jgitExecutor.pull(req, cb);
                recordLog(req.getRepoPath(), OperationType.PULL, "git pull", true, System.currentTimeMillis() - start, null, null);
            } catch (Exception e) {
                recordLog(req.getRepoPath(), OperationType.PULL, "git pull", false, System.currentTimeMillis() - start, e.getMessage(), null);
                throw e instanceof GitGuiException ge ? ge : new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
            }
        }, cb);
    }

    @Override
    public TaskHandle fetch(String repoPath, String remote, String branch, boolean prune, ProgressCallback cb) {
        return asyncTaskService.submitWrite(repoPath, TaskType.FETCH, () -> {
            long start = System.currentTimeMillis();
            try {
                jgitExecutor.fetch(repoPath, remote, branch, prune, cb);
                recordLog(repoPath, OperationType.FETCH, "git fetch", true, System.currentTimeMillis() - start, null, null);
            } catch (Exception e) {
                recordLog(repoPath, OperationType.FETCH, "git fetch", false, System.currentTimeMillis() - start, e.getMessage(), null);
                throw e instanceof GitGuiException ge ? ge : new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
            }
        }, cb);
    }

    @Override
    public TaskHandle sync(String repoPath, String remote, String branch, ProgressCallback cb) {
        // Sync = Pull + Push
        return asyncTaskService.submitWrite(repoPath, TaskType.PULL, () -> {
            long start = System.currentTimeMillis();
            try {
                PullRequest pullReq = PullRequest.builder().repoPath(repoPath).remote(remote).branch(branch).build();
                jgitExecutor.pull(pullReq, cb);
                PushRequest pushReq = PushRequest.builder().repoPath(repoPath).remote(remote).branch(branch).build();
                jgitExecutor.push(pushReq, cb);
                recordLog(repoPath, OperationType.SYNC, "git pull && git push", true, System.currentTimeMillis() - start, null, null);
            } catch (Exception e) {
                recordLog(repoPath, OperationType.SYNC, "git pull && git push", false, System.currentTimeMillis() - start, e.getMessage(), null);
                throw e instanceof GitGuiException ge ? ge : new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
            }
        }, cb);
    }

    @Override
    public void checkout(CheckoutRequest req) {
        if (req == null) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "切换请求不能为空");
        }
        if (req.getRepoPath() == null || req.getRepoPath().isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        CheckoutRequest.TargetType type = req.getTargetType() == null
                ? CheckoutRequest.TargetType.BRANCH : req.getTargetType();
        long start = System.currentTimeMillis();
        try {
            String op;
            switch (type) {
                case TAG:
                    if (req.getTag() == null || req.getTag().isBlank()) {
                        throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请选择目标 tag");
                    }
                    jgitExecutor.checkoutTag(req.getRepoPath(), req.getTag(), req.isForce());
                    op = "git checkout " + req.getTag();
                    break;
                case COMMIT:
                    if (req.getCommitId() == null || req.getCommitId().isBlank()) {
                        throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请选择目标 commit");
                    }
                    // 若用户勾选了 Create New Branch / Override branch → 从 commit 创建新分支
                    if ((req.isCreate() || req.isOverrideExisting())
                            && req.getNewBranch() != null && !req.getNewBranch().isBlank()) {
                        jgitExecutor.checkoutNewBranchFromCommit(
                                req.getRepoPath(), req.getNewBranch(), req.getCommitId(),
                                req.isForce(), req.isOverrideExisting());
                        op = "git checkout " + (req.isOverrideExisting() ? "-B " : "-b ")
                                + req.getNewBranch() + " " + req.getCommitId();
                    } else {
                        // 否则：游离 HEAD 切换到 commit
                        jgitExecutor.checkoutCommit(req.getRepoPath(), req.getCommitId(), req.isForce());
                        op = "git checkout " + req.getCommitId();
                    }
                    break;
                case BRANCH:
                default:
                    // 处理「覆盖已存在分支」(git checkout -B) 优先级最高
                    String branchName;
                    if (req.isOverrideExisting() && req.getNewBranch() != null && !req.getNewBranch().isBlank()) {
                        branchName = req.getNewBranch();
                    } else if (req.isCreate() && req.getNewBranch() != null && !req.getNewBranch().isBlank()) {
                        branchName = req.getNewBranch();
                    } else {
                        branchName = req.getBranch();
                    }
                    if (branchName == null || branchName.isBlank()) {
                        throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请选择目标分支");
                    }
                    // 1. 优先使用完整 refName（来自 BrowseReferencesDialog，避免远程分支被剥离前缀后无法解析）
                    // 2. 其次看是否为 refs/... 完整路径（用户手动键入）
                    // 3. 否则按本地分支处理（需考虑 -b / -B 选项）
                    String refName = req.getRefName();
                    if (refName != null && !refName.isBlank() && refName.startsWith("refs/")) {
                        // 完整 ref：refs/heads/<x> / refs/remotes/<remote>/<x> / refs/tags/<x>
                        // 直接 JGit 解析；若是远程分支，先尝试创建同名本地分支（带 track），再 checkout
                        if (refName.startsWith("refs/remotes/")) {
                            checkoutRemoteRef(req.getRepoPath(), refName, req.isForce());
                        } else {
                            // refs/heads/ 或 refs/tags/ 直接 checkout
                            jgitExecutor.checkoutRef(req.getRepoPath(), refName, req.isForce());
                        }
                    } else if (req.isTrack()) {
                        // 跟踪分支：使用 git.checkout().setStartPoint(remote).setCreateBranch(true).setUpstreamMode(TRACK).call()
                        checkoutWithTrack(req.getRepoPath(), branchName, req.isForce());
                    } else {
                        jgitExecutor.checkout(req.getRepoPath(), branchName,
                                req.isCreate() || req.isOverrideExisting(), req.isForce());
                    }
                    op = "git checkout " + (req.isCreate() ? "-b " : "")
                            + (req.isOverrideExisting() ? "-B " : "") + branchName
                            + (refName != null && !refName.isBlank() ? "  (ref: " + refName + ")" : "");
                    break;
            }
            recordLog(req.getRepoPath(), OperationType.CHECKOUT, op,
                    true, System.currentTimeMillis() - start, null, null);
        } catch (Exception e) {
            recordLog(req.getRepoPath(), OperationType.CHECKOUT,
                    "git checkout (failed)", false,
                    System.currentTimeMillis() - start, e.getMessage(), null);
            throw e instanceof GitGuiException ge ? ge : new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
        }
    }

    /**
     * 创建本地分支并设置跟踪远程上游（git checkout -b branch --track origin/branch）。
     */
    private void checkoutWithTrack(String repoPath, String branchName, boolean force) {
        try (org.eclipse.jgit.api.Git git = org.eclipse.jgit.api.Git.open(new java.io.File(repoPath, ".git").exists()
                ? new java.io.File(repoPath, ".git") : new java.io.File(repoPath))) {
            org.eclipse.jgit.api.CreateBranchCommand create = git.branchCreate()
                    .setName(branchName)
                    .setStartPoint("origin/" + branchName)
                    .setUpstreamMode(org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .setForce(force);
            create.call();
            // 再 checkout 到新分支
            git.checkout().setName(branchName).call();
        } catch (Exception e) {
            throw new com.gitgui.core.exception.GitGuiException(
                    com.gitgui.core.exception.ErrorCode.GIT_EXECUTION_FAILED,
                    "创建跟踪分支失败：" + e.getMessage(), e);
        }
    }

    /**
     * 处理远程分支 checkout（refs/remotes/&lt;remote&gt;/&lt;branch&gt; 形式）：
     * <ol>
     *   <li>创建同名本地分支（带跟踪上游 origin/branch），等价于 git checkout -b branch --track origin/branch</li>
     *   <li>再 checkout 到该本地分支</li>
     * </ol>
     * <p>与 TortoiseGit 行为一致：双击远程分支后，本地会出现同名分支并设置跟踪。</p>
     *
     * @param repoPath 仓库路径
     * @param refName  完整 ref 名 refs/remotes/<remote>/<branch>
     * @param force    是否强制（丢弃本地修改）
     */
    private void checkoutRemoteRef(String repoPath, String refName, boolean force) {
        // 1. 解析 refName 拿到本地分支名（strip refs/remotes/<remote>/）
        // 典型形态：refs/remotes/origin/feature/x → localName = feature/x
        String prefix = "refs/remotes/";
        if (!refName.startsWith(prefix)) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED,
                    "非法的远程 ref 路径：" + refName);
        }
        String rest = refName.substring(prefix.length());
        int slash = rest.indexOf('/');
        if (slash <= 0) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED,
                    "远程 ref 缺少分支段：" + refName);
        }
        String remote = rest.substring(0, slash);
        String branchPath = rest.substring(slash + 1);

        try (org.eclipse.jgit.api.Git git = org.eclipse.jgit.api.Git.open(new java.io.File(repoPath, ".git").exists()
                ? new java.io.File(repoPath, ".git") : new java.io.File(repoPath))) {
            // 2. 若已存在同名本地分支，仅 checkout 即可（更新工作区到远程 HEAD）
            if (git.getRepository().findRef("refs/heads/" + branchPath) != null) {
                if (force) {
                    // 丢弃本地未提交修改 → 用 setForceRefUpdate
                    git.checkout().setName(branchPath).setForceRefUpdate(true).call();
                } else {
                    git.checkout().setName(branchPath).call();
                }
            } else {
                // 3. 不存在则创建本地分支并设置上游
                org.eclipse.jgit.api.CreateBranchCommand create = git.branchCreate()
                        .setName(branchPath)
                        .setStartPoint(remote + "/" + branchPath)
                        .setUpstreamMode(org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .setForce(true);
                create.call();
                // 4. checkout 到新分支
                git.checkout().setName(branchPath).call();
            }
            log.info("远程分支 checkout 完成：repo={}, ref={} → local={}/{}", repoPath, refName, remote, branchPath);
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED,
                    "切换远程分支失败：" + e.getMessage(), e);
        }
    }

    /**
     * 记录操作日志（BR-35）。
     */
    private void recordLog(String repoPath, OperationType operation, String command,
                           boolean success, long durationMs, String errorMessage, String taskId) {
        OperationLog logEntry = OperationLog.builder()
                .repoPath(repoPath)
                .operation(operation)
                .command(command)
                .success(success)
                .durationMs(durationMs)
                .errorMessage(errorMessage == null ? "" : errorMessage)
                .taskId(taskId == null ? "" : taskId)
                .build();
        operationLogRepository.save(logEntry);
    }
}
