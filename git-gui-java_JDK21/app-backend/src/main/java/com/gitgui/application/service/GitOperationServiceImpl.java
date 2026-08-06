package com.gitgui.application.service;

import com.gitgui.application.redline.CommandInterceptor;
import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.core.constant.OperationType;
import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.OperationLog;
import com.gitgui.domain.model.request.CheckoutRequest;
import com.gitgui.domain.model.request.CommitRequest;
import com.gitgui.domain.model.request.PullRequest;
import com.gitgui.domain.model.request.PushRequest;
import com.gitgui.domain.redline.RedLineContext;
import com.gitgui.domain.repository.OperationLogRepository;
import com.gitgui.domain.service.AsyncTaskService;
import com.gitgui.domain.service.GitOperationService;
import com.gitgui.infrastructure.cli.CliGitExecutor;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Git 操作服务实现（核心写操作）
 * <p>所有 Git 操作通过 CLI 执行。所有写方法执行前由 {@link CommandInterceptor} 自动调用红线校验。</p>
 * <p>关联 BR：BR-06~BR-25。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class GitOperationServiceImpl implements GitOperationService {

    private static final Logger log = LoggerFactory.getLogger(GitOperationServiceImpl.class);

    private final CliGitExecutor gitExecutor;
    private final CommandInterceptor commandInterceptor;
    private final AsyncTaskService asyncTaskService;
    private final OperationLogRepository operationLogRepository;

    @Inject
    public GitOperationServiceImpl(CliGitExecutor gitExecutor, CommandInterceptor commandInterceptor, AsyncTaskService asyncTaskService,
            OperationLogRepository operationLogRepository) {
        this.gitExecutor = gitExecutor;
        this.commandInterceptor = commandInterceptor;
        this.asyncTaskService = asyncTaskService;
        this.operationLogRepository = operationLogRepository;
    }

    @Override
    public String commit(CommitRequest req) {
        // 同步版本走异步路径后阻塞拿结果（保留向后兼容，新代码请用 commit(req, cb)）
        if (req.getStagedFiles() == null || req.getStagedFiles()
                                               .isEmpty()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请至少勾选一个变更文件（BR-06）");
        }
        if (req.getMessage() == null || req.getMessage()
                                           .trim()
                                           .isEmpty()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "提交信息不能为空（BR-07）");
        }
        validateRedLine(req);
        long start = System.currentTimeMillis();
        try {
            String commitId = gitExecutor.commit(req, null);
            recordLog(req.getRepoPath(), OperationType.COMMIT, "git commit", true, System.currentTimeMillis() - start, null, null);
            return commitId;
        } catch (Exception e) {
            recordLog(req.getRepoPath(), OperationType.COMMIT, "git commit", false, System.currentTimeMillis() - start, e.getMessage(), null);
            throw e instanceof GitGuiException ge ? ge : new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
        }
    }

    @Override
    public TaskHandle commit(CommitRequest req, ProgressCallback cb) {
        if (req == null) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "提交请求不能为空");
        }
        if (req.getStagedFiles() == null || req.getStagedFiles()
                                               .isEmpty()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请至少勾选一个变更文件（BR-06）");
        }
        if (req.getMessage() == null || req.getMessage()
                                           .trim()
                                           .isEmpty()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "提交信息不能为空（BR-07）");
        }
        validateRedLine(req);
        return asyncTaskService.submitWrite(req.getRepoPath(), TaskType.COMMIT, () -> {
            long start = System.currentTimeMillis();
            try {
                gitExecutor.commit(req, cb);
                recordLog(req.getRepoPath(), OperationType.COMMIT, "git commit", true, System.currentTimeMillis() - start, null, null);
            } catch (Exception e) {
                recordLog(req.getRepoPath(), OperationType.COMMIT, "git commit", false, System.currentTimeMillis() - start, e.getMessage(), null);
                throw e instanceof GitGuiException ge ? ge : new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
            }
        }, cb);
    }

    /**
     * 提交请求的红线校验（BR-06/BR-07 已在校验阶段完成，这里只校验 commit 类型的红线规则）。
     */
    private void validateRedLine(CommitRequest req) {
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.COMMIT)
                                           .command("git commit")
                                           .repoPath(req.getRepoPath())
                                           .stagedFiles(req.getStagedFiles())
                                           .amend(false)
                                           .noVerify(false)
                                           .build();
        commandInterceptor.intercept(ctx);
    }

    @Override
    public TaskHandle push(PushRequest req, ProgressCallback cb) {
        if (req.getRemote() == null || req.getRemote()
                                          .isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请选择 Remote（BR-09）");
        }
        // v3 起 UI 取消 Force / Tags / 高级模式 / 临时 URL 等选项，
        // RedLineContext 不再带 force/forceWithLease/deleteRemoteBranch（这些字段永远 false）。
        RedLineContext ctx = RedLineContext.builder()
                                           .operation(OperationType.PUSH)
                                           .command("git push")
                                           .repoPath(req.getRepoPath())
                                           .branch(req.getBranch())
                                           .build();
        commandInterceptor.intercept(ctx);
        return asyncTaskService.submitWrite(req.getRepoPath(), TaskType.PUSH, () -> {
            long start = System.currentTimeMillis();
            try {
                gitExecutor.push(req, cb);
                recordLog(req.getRepoPath(), OperationType.PUSH, "git push", true, System.currentTimeMillis() - start, null, null);
            } catch (Exception e) {
                recordLog(req.getRepoPath(), OperationType.PUSH, "git push", false, System.currentTimeMillis() - start, e.getMessage(), null);
                throw e instanceof GitGuiException ge ? ge : new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
            }
        }, cb);
    }

    @Override
    public String pushViaCli(PushRequest req) {
        if (req.getRemote() == null || req.getRemote()
                                          .isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请选择 Remote（BR-09）");
        }
        long start = System.currentTimeMillis();
        try {
            gitExecutor.push(req, null);
            recordLog(req.getRepoPath(), OperationType.PUSH, "git push", true, System.currentTimeMillis() - start, null, null);
            return "push completed";
        } catch (Exception e) {
            recordLog(req.getRepoPath(), OperationType.PUSH, "git push", false, System.currentTimeMillis() - start, e.getMessage(), null);
            throw e instanceof GitGuiException ge ? ge : new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
        }
    }

    @Override
    public TaskHandle pull(PullRequest req, ProgressCallback cb) {
        if (req.getRemote() == null || req.getRemote()
                                          .isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请选择 Remote（BR-09）");
        }
        return asyncTaskService.submitWrite(req.getRepoPath(), TaskType.PULL, () -> {
            long start = System.currentTimeMillis();
            try {
                gitExecutor.pull(req, cb);
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
                gitExecutor.fetch(repoPath, remote, branch, prune, cb);
                recordLog(repoPath, OperationType.FETCH, "git fetch", true, System.currentTimeMillis() - start, null, null);
            } catch (Exception e) {
                recordLog(repoPath, OperationType.FETCH, "git fetch", false, System.currentTimeMillis() - start, e.getMessage(), null);
                throw e instanceof GitGuiException ge ? ge : new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
            }
        }, cb);
    }

    @Override
    public TaskHandle sync(String repoPath, String remote, String branch, ProgressCallback cb) {
        return asyncTaskService.submitWrite(repoPath, TaskType.PULL, () -> {
            long start = System.currentTimeMillis();
            try {
                PullRequest pullReq = PullRequest.builder()
                                                 .repoPath(repoPath)
                                                 .remote(remote)
                                                 .branch(branch)
                                                 .build();
                gitExecutor.pull(pullReq, cb);
                PushRequest pushReq = PushRequest.builder()
                                                 .repoPath(repoPath)
                                                 .remote(remote)
                                                 .branch(branch)
                                                 .build();
                gitExecutor.push(pushReq, cb);
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
        if (req.getRepoPath() == null || req.getRepoPath()
                                            .isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        CheckoutRequest.TargetType type = req.getTargetType() == null ? CheckoutRequest.TargetType.BRANCH : req.getTargetType();
        long start = System.currentTimeMillis();
        try {
            String op;
            switch (type) {
                case TAG:
                    if (req.getTag() == null || req.getTag()
                                                   .isBlank()) {
                        throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请选择目标 tag");
                    }
                    gitExecutor.checkoutTag(req.getRepoPath(), req.getTag(), req.isForce());
                    op = String.format("git checkout %s", req.getTag());
                    break;
                case COMMIT:
                    if (req.getCommitId() == null || req.getCommitId()
                                                        .isBlank()) {
                        throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请选择目标 commit");
                    }
                    if ((req.isCreate() || req.isOverrideExisting()) && req.getNewBranch() != null && !req.getNewBranch()
                                                                                                          .isBlank()) {
                        gitExecutor.checkoutNewBranchFromCommit(req.getRepoPath(), req.getNewBranch(), req.getCommitId(), req.isForce(),
                                                                req.isOverrideExisting());
                        op = String.format("git checkout %s %s %s", req.isOverrideExisting() ? "-B" : "-b", req.getNewBranch(), req.getCommitId());
                    } else {
                        gitExecutor.checkoutCommit(req.getRepoPath(), req.getCommitId(), req.isForce());
                        op = String.format("git checkout %s", req.getCommitId());
                    }
                    break;
                case BRANCH:
                default:
                    String branchName;
                    if (req.isOverrideExisting() && req.getNewBranch() != null && !req.getNewBranch()
                                                                                      .isBlank()) {
                        branchName = req.getNewBranch();
                    } else if (req.isCreate() && req.getNewBranch() != null && !req.getNewBranch()
                                                                                   .isBlank()) {
                        branchName = req.getNewBranch();
                    } else {
                        branchName = req.getBranch();
                    }
                    if (branchName == null || branchName.isBlank()) {
                        throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请选择目标分支");
                    }
                    String refName = req.getRefName();
                    if (refName != null && !refName.isBlank() && refName.startsWith("refs/")) {
                        if (refName.startsWith("refs/remotes/")) {
                            gitExecutor.checkoutRemoteRef(req.getRepoPath(), refName, req.isForce());
                        } else {
                            gitExecutor.checkoutRef(req.getRepoPath(), refName, req.isForce());
                        }
                    } else if (req.isTrack()) {
                        gitExecutor.checkoutWithTrack(req.getRepoPath(), branchName, req.isForce());
                    } else {
                        gitExecutor.checkout(req.getRepoPath(), branchName, req.isCreate() || req.isOverrideExisting(), req.isForce());
                    }
                    op = String.format("git checkout %s%s%s%s", req.isCreate() ? "-b " : "", req.isOverrideExisting() ? "-B " : "", branchName,
                                       refName != null && !refName.isBlank() ? String.format(" (ref: %s)", refName) : "");
                    break;
            }
            recordLog(req.getRepoPath(), OperationType.CHECKOUT, op, true, System.currentTimeMillis() - start, null, null);
        } catch (Exception e) {
            recordLog(req.getRepoPath(), OperationType.CHECKOUT, "git checkout (failed)", false, System.currentTimeMillis() - start, e.getMessage(),
                      null);
            throw e instanceof GitGuiException ge ? ge : new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
        }
    }

    private void recordLog(String repoPath, OperationType operation, String command, boolean success, long durationMs, String errorMessage,
            String taskId) {
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
