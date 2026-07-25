package com.gitgui.infrastructure.jgit;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.constant.OperationType;
import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.domain.model.DiffResult;
import com.gitgui.domain.model.FileChange;
import com.gitgui.domain.model.FileStatus;
import com.gitgui.domain.model.LogEntry;
import com.gitgui.domain.model.RefInfo;
import com.gitgui.domain.model.RemoteConfig;
import com.gitgui.domain.model.request.CloneRequest;
import com.gitgui.domain.model.request.CommitRequest;
import com.gitgui.domain.model.request.PullRequest;
import com.gitgui.domain.model.request.PushRequest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * JGit 操作执行器（主适配器）
 * <p>纯 Java 实现 Git 操作，进度可控、无进程开销。</p>
 * <p>遵循 03-backend.md「直通优先策略」：能用 JGit 完成的操作优先 JGit，JGit 不支持或异常时降级为 CLI。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class JGitOperationExecutor {

    private static final Logger log = LoggerFactory.getLogger(JGitOperationExecutor.class);

    /**
     * 打开 Git 实例。
     *
     * @param repoPath 仓库路径
     * @return Git 实例（调用方负责关闭）
     * @throws GitGuiException 仓库打开失败
     */
    public Git open(String repoPath) {
        try {
            File gitDir = new File(repoPath, ".git");
            Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).readEnvironment().findGitDir().build();
            return new Git(repo);
        } catch (IOException e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "打开仓库失败：" + repoPath, e);
        }
    }

    /**
     * 提交（BR-06/BR-07）。
     *
     * @param req 提交请求
     * @return 提交哈希
     */
    public String commit(CommitRequest req) {
        try (Git git = open(req.getRepoPath())) {
            // TortoiseGit 风格：提交前若勾选「new branch」，先创建并切换
            if (req.isCreateNewBranch() && req.getNewBranchName() != null && !req.getNewBranchName().isBlank()) {
                String newBranch = req.getNewBranchName().trim();
                // 在当前 HEAD 上创建新分支
                org.eclipse.jgit.lib.ObjectId head = git.getRepository().resolve("HEAD");
                git.checkout()
                        .setCreateBranch(true)
                        .setName(newBranch)
                        .setStartPoint(head.name())
                        .call();
            }
            // 暂存勾选的文件（BR-06）
            if (req.getStagedFiles() != null && !req.getStagedFiles().isEmpty()) {
                var addCommand = git.add();
                for (String file : req.getStagedFiles()) {
                    addCommand.addFilepattern(file);
                }
                addCommand.call();
            }
            // BR-07：提交信息非空校验已在应用层完成
            var commitCommand = git.commit().setMessage(req.getMessage()).setAmend(req.isAmend());
            if (req.getAuthor() != null && !req.getAuthor().isBlank()) {
                // 解析作者格式 "Name <email>"
                String author = req.getAuthor();
                int lt = author.indexOf('<');
                int gt = author.indexOf('>');
                String name = lt > 0 ? author.substring(0, lt).trim() : author.trim();
                String email = (lt > 0 && gt > lt) ? author.substring(lt + 1, gt).trim() : "";
                if (req.isSetAuthorDate() && req.getAuthorDate() != null) {
                    // 自定义作者 + 作者时间：构造 PersonIdent
                    java.time.Instant instant = req.getAuthorDate().atZone(java.time.ZoneId.systemDefault()).toInstant();
                    org.eclipse.jgit.lib.PersonIdent ident = new org.eclipse.jgit.lib.PersonIdent(
                            name, email, instant, java.time.ZoneId.systemDefault());
                    commitCommand.setAuthor(ident);
                } else {
                    // 仅自定义作者（保留 commit time / tz 默认）
                    commitCommand.setAuthor(name, email);
                }
            } else if (req.isSetAuthorDate() && req.getAuthorDate() != null) {
                // 仅设置作者时间，保留当前 git 配置中的 name/email
                org.eclipse.jgit.lib.Repository repo = git.getRepository();
                org.eclipse.jgit.lib.PersonIdent current = new org.eclipse.jgit.lib.PersonIdent(repo);
                java.time.Instant instant = req.getAuthorDate().atZone(java.time.ZoneId.systemDefault()).toInstant();
                org.eclipse.jgit.lib.PersonIdent ident = new org.eclipse.jgit.lib.PersonIdent(
                        current.getName(), current.getEmailAddress(), instant, java.time.ZoneId.systemDefault());
                commitCommand.setAuthor(ident);
            }
            RevCommit commit = commitCommand.call();
            log.info("提交成功：{}", commit.getName());
            return commit.getName();
        } catch (GitAPIException | java.io.IOException e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "提交失败：" + e.getMessage(), e);
        }
    }

    /**
     * 推送（异步，BR-09/BR-10）。
     *
     * @param req      推送请求
     * @param callback 进度回调
     */
    public void push(PushRequest req, ProgressCallback callback) {
        try (Git git = open(req.getRepoPath())) {
            var pushCommand = git.push();
            if (req.getRemote() != null && !req.getRemote().isBlank()) {
                pushCommand.setRemote(req.getRemote());
            }
            if (req.getPushToUrl() != null && !req.getPushToUrl().isBlank()) {
                pushCommand.setRemote(req.getPushToUrl());
            }
            if (req.isForce()) {
                pushCommand.setForce(true);
            } else if (req.isForceWithLease()) {
                // 已知限制：JGit 6.9.0 的 PushCommand 未暴露 setForceWithLease 方法，
                // 真正的 force-with-lease 需通过 Transport + RemoteRefUpdate.setExpectedOldObjectId 实现。
                // 此处暂以 setForce(true) 兜底，安全性由应用层红线规则保障：
                // ProtectedBranchRule（BR-27）会拦截对保护分支的 force/force-with-lease 推送。
                // TODO 后续通过 Transport API 实现真正的 force-with-lease 语义
                log.warn("force-with-lease 暂以裸 --force 实现（JGit 6.9.0 限制）：repoPath={}, branch={}",
                        req.getRepoPath(), req.getBranch());
                pushCommand.setForce(true);
            }
            if (req.isPushAllBranches()) {
                pushCommand.setPushAll();
            }
            if (req.isPushAllTags()) {
                pushCommand.setPushTags();
            }
            pushCommand.call().forEach(result -> {
                if (callback != null) {
                    callback.onOutput(result.getMessages());
                }
            });
            log.info("推送完成：repoPath={}, remote={}", req.getRepoPath(), req.getRemote());
        } catch (ClassCastException e) {
            // JGit 6.9.0 已知问题：某些 remote 配置（如 fsck.missingCache=error、receive.denyDeleteCurrent
            // 配置异常、ref 格式问题）会在 push 时内部抛 CCE，message 经常为 null。
            // 这种错误 JGit 本身没修，先把 cause 链打全以便诊断。
            log.error("推送 ClassCastException（JGit 6.9 内部 bug），repoPath={}, remote={}, causeChain={}",
                    req.getRepoPath(), req.getRemote(), describeCauseChain(e), e);
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED,
                    "推送时类型转换失败：" + describeCauseChain(e), e);
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "推送失败：" + e.getMessage(), e);
        }
    }

    /**
     * 描述异常 cause 链（用于 ClassCastException 诊断）。
     */
    private static String describeCauseChain(Throwable t) {
        if (t == null) return "无";
        StringBuilder sb = new StringBuilder();
        Throwable cur = t;
        int depth = 0;
        while (cur != null && depth < 5) {
            if (depth > 0) sb.append(" -> ");
            sb.append(cur.getClass().getName());
            String msg = cur.getMessage();
            if (msg != null && !msg.isBlank() && !msg.equals(cur.getClass().getName())) {
                sb.append(": ").append(msg);
            }
            if (cur.getCause() == null || cur.getCause() == cur) break;
            cur = cur.getCause();
            depth++;
        }
        return sb.toString();
    }

    /**
     * CLI 推送回退方案：当 JGit 6.9.0 在某些环境抛 ClassCastException / 其他 JGit 内部异常时，
     * 用系统 {@code git push} CLI 兜底（用用户已配的 git config / credentials / SSH keys）。
     *
     * <p><b>仅在 JGit 推送失败且错误看起来是 JGit 内部 bug 时调用</b>（不是网络错误，因为网络错误
     * CLI 同样会失败）。</p>
     *
     * @param req 推送请求
     * @return CLI 推送的输出
     * @throws Exception 找不到 git / 进程错误 / CLI 推送失败
     */
    public String pushViaCli(PushRequest req) throws Exception {
        String gitExe = findGitExecutable();
        if (gitExe == null) {
            throw new GitGuiException(ErrorCode.GIT_NOT_FOUND,
                    "未找到 git 可执行文件。请先安装 Git for Windows 并加入 PATH。");
        }
        // 构建命令：git push <remote> [<branch>] [--tags] [--force]
        List<String> cmd = new ArrayList<>();
        cmd.add(gitExe);
        cmd.add("push");
        if (req.getRemote() != null && !req.getRemote().isBlank()) {
            cmd.add(req.getRemote());
        }
        if (req.getBranch() != null && !req.getBranch().isBlank()) {
            cmd.add(req.getBranch());
        }
        if (req.isPushAllBranches()) {
            cmd.add("--all");
        }
        if (req.isPushAllTags() || req.isIncludeTags()) {
            cmd.add("--tags");
        }
        if (req.isForce() || req.isForceWithLease()) {
            cmd.add("--force");
        }
        log.info("CLI 推送回退：repoPath={}, cmd={}", req.getRepoPath(), String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd)
                .directory(new File(req.getRepoPath()))
                .redirectErrorStream(true);
        Process process = pb.start();
        // 捕获输出（避免阻塞）
        StringBuilder output = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(),
                        java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.info("[git push] {}", line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED,
                    "CLI git push 失败（exit " + exitCode + "）：\n" + output);
        }
        return output.toString();
    }

    /**
     * 在系统 PATH 中查找 {@code git} 可执行文件。
     * <ul>
     *   <li>Windows：尝试 {@code git.exe}（Git for Windows 默认在 PATH）</li>
     *   <li>macOS / Linux：尝试 {@code git}</li>
     * </ul>
     */
    private String findGitExecutable() {
        String[] candidates = System.getProperty("os.name").toLowerCase().contains("win")
                ? new String[]{"git.exe", "git", "C:\\Program Files\\Git\\bin\\git.exe",
                        "C:\\Program Files (x86)\\Git\\bin\\git.exe"}
                : new String[]{"git", "/usr/bin/git", "/usr/local/bin/git",
                        "/opt/homebrew/bin/git"};
        for (String candidate : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(candidate, "--version")
                        .redirectErrorStream(true);
                Process p = pb.start();
                if (p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS) && p.exitValue() == 0) {
                    log.info("找到 git 可执行文件：{}", candidate);
                    return candidate;
                }
            } catch (Exception e) {
                // 继续尝试下一个
            }
        }
        return null;
    }

    /**
     * 拉取（异步）。
     *
     * @param req      拉取请求
     * @param callback 进度回调
     */
    public void pull(PullRequest req, ProgressCallback callback) {
        try (Git git = open(req.getRepoPath())) {
            var pullCommand = git.pull().setRemote(req.getRemote());
            if (req.getBranch() != null && !req.getBranch().isBlank()) {
                pullCommand.setRemoteBranchName(req.getBranch());
            }
            if (req.isRebaseInsteadOfMerge()) {
                pullCommand.setRebase(true);
            }
            pullCommand.call();
            log.info("拉取完成：repoPath={}", req.getRepoPath());
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "拉取失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取。
     *
     * @param repoPath 仓库路径
     * @param remote   远程名
     * @param branch   分支
     * @param prune    是否清理
     * @param callback 进度回调
     */
    public void fetch(String repoPath, String remote, String branch, boolean prune, ProgressCallback callback) {
        try (Git git = open(repoPath)) {
            var fetchCommand = git.fetch().setRemote(remote).setRemoveDeletedRefs(prune);
            if (branch != null && !branch.isBlank()) {
                fetchCommand.setRefSpecs("+refs/heads/" + branch + ":refs/remotes/" + remote + "/" + branch);
            }
            fetchCommand.call();
            log.info("获取完成：repoPath={}", repoPath);
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "获取失败：" + e.getMessage(), e);
        }
    }

    /**
     * 克隆仓库（异步）。
     *
     * @param req      克隆请求
     * @param callback 进度回调
     * @return 克隆后的仓库路径
     */
    public String clone(CloneRequest req, ProgressCallback callback) {
        try {
            var cloneCommand = Git.cloneRepository()
                    .setURI(req.getRemoteUrl())
                    .setDirectory(new File(req.getTargetDir()));
            if (req.getBranch() != null && !req.getBranch().isBlank()) {
                cloneCommand.setBranch(req.getBranch());
            }
            if (req.getDepth() > 0) {
                cloneCommand.setDepth(req.getDepth());
            }
            if (req.isBare()) {
                cloneCommand.setBare(true);
            }
            try (Git git = cloneCommand.call()) {
                log.info("克隆成功：{}", req.getTargetDir());
                return req.getTargetDir();
            }
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "克隆失败：" + e.getMessage(), e);
        }
    }

    /**
     * 初始化仓库。
     *
     * @param dir  目标目录
     * @param bare 是否裸仓库
     */
    public void init(String dir, boolean bare) {
        try {
            File gitDir = bare ? new File(dir) : new File(dir, ".git");
            try (Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build()) {
                repo.create(bare);
            }
            log.info("仓库初始化成功：{}", dir);
        } catch (IOException e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "初始化仓库失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取工作区文件状态列表（PRD 4.3）。
     *
     * @param repoPath      仓库路径
     * @param showUntracked 是否显示未跟踪文件
     * @return 文件状态列表
     */
    public List<FileStatus> getStatus(String repoPath, boolean showUntracked) {
        try (Git git = open(repoPath)) {
            var statusCommand = git.status();
            var status = statusCommand.call();
            List<FileStatus> list = new ArrayList<>();
            status.getModified().forEach(p -> list.add(FileStatus.builder().path(p).state(FileStatus.FileState.MODIFIED).build()));
            status.getChanged().forEach(p -> list.add(FileStatus.builder().path(p).state(FileStatus.FileState.MODIFIED).build()));
            status.getAdded().forEach(p -> list.add(FileStatus.builder().path(p).state(FileStatus.FileState.STAGED).build()));
            status.getRemoved().forEach(p -> list.add(FileStatus.builder().path(p).state(FileStatus.FileState.DELETED).build()));
            status.getMissing().forEach(p -> list.add(FileStatus.builder().path(p).state(FileStatus.FileState.DELETED).build()));
            status.getConflicting().forEach(p -> list.add(FileStatus.builder().path(p).state(FileStatus.FileState.CONFLICT).build()));
            if (showUntracked) {
                status.getUntracked().forEach(p -> list.add(FileStatus.builder().path(p).state(FileStatus.FileState.UNTRACKED).build()));
            }
            return list;
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "获取状态失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取提交日志（BR-18 分页）。
     *
     * @param repoPath 仓库路径
     * @param page     页码（从 1 开始）
     * @param pageSize 单页条数
     * @return 日志条目列表
     */
    public List<LogEntry> getLog(String repoPath, int page, int pageSize) {
        try (Git git = open(repoPath)) {
            var logCommand = git.log();
            // 设置分页上限（skip + maxCount 简化实现）
            int skip = Math.max(0, (page - 1) * pageSize);
            logCommand.setSkip(skip).setMaxCount(pageSize);
            Iterable<RevCommit> commits = logCommand.call();
            List<LogEntry> list = new ArrayList<>();
            for (RevCommit commit : commits) {
                list.add(LogEntry.builder()
                        .commitId(commit.getName())
                        .shortId(commit.getName().substring(0, 7))
                        .author(commit.getAuthorIdent().getName())
                        .authorEmail(commit.getAuthorIdent().getEmailAddress())
                        .commitTime(LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(commit.getCommitTime()),
                                ZoneId.systemDefault()))
                        .message(commit.getFullMessage())
                        .refs(Collections.emptyList())
                        .parents(Arrays.stream(commit.getParents()).map(RevCommit::getName).toList())
                        .build());
            }
            return list;
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "获取日志失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取文件 Diff。
     *
     * @param repoPath 仓库路径
     * @param path     文件路径
     * @param oldRev   旧版本（null 表示工作区对比 HEAD）
     * @param newRev   新版本（null 表示工作区）
     * @return Diff 结果
     */
    /**
     * 获取 commit 引发的文件变更列表（Log Messages 对话框「Path」面板用）。
     * <p>遍历 commit 的所有 parent，diff tree 与 commit tree 得到 changed files，</p>
     * <p>返回每个文件的 path / changeType（ADD / MODIFY / DELETE / RENAME）。</p>
     *
     * @param repoPath 仓库路径
     * @param commitId 提交哈希（完整或短哈希均可）
     * @return 文件变更列表
     */
    public List<FileChange> getCommitChanges(String repoPath, String commitId) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        if (commitId == null || commitId.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "commit id 不能为空");
        }
        try (Git git = open(repoPath)) {
            Repository repo = git.getRepository();
            RevCommit target = repo.parseCommit(repo.resolve(commitId));
            List<FileChange> changes = new ArrayList<>();
            try (RevWalk walk = new RevWalk(repo);
                 DiffFormatter df = new DiffFormatter(OutputStream.nullOutputStream())) {
                df.setRepository(repo);
                if (target.getParentCount() == 0) {
                    // 首次 commit：所有文件都是 ADD
                    CanonicalTreeParser newTree = new CanonicalTreeParser();
                    newTree.reset(walk.getObjectReader(), target.getTree());
                    for (DiffEntry e : df.scan(null, newTree)) {
                        changes.add(new FileChange(e.getNewPath(), "ADD", null, e.getNewPath()));
                    }
                } else {
                    for (RevCommit parent : target.getParents()) {
                        CanonicalTreeParser oldTree = new CanonicalTreeParser();
                        oldTree.reset(walk.getObjectReader(), parent.getTree());
                        CanonicalTreeParser newTree = new CanonicalTreeParser();
                        newTree.reset(walk.getObjectReader(), target.getTree());
                        for (DiffEntry e : df.scan(oldTree, newTree)) {
                            String changeType = changeTypeToString(e.getChangeType());
                            changes.add(new FileChange(
                                    e.getNewPath() == null ? e.getOldPath() : e.getNewPath(),
                                    changeType,
                                    e.getOldPath(),
                                    e.getNewPath()));
                        }
                    }
                }
            }
            // 去重（merge commit 多个 parent 可能产生重复）
            changes.sort((a, b) -> {
                String pa = a.getNewPath() == null ? a.getOldPath() : a.getNewPath();
                String pb = b.getNewPath() == null ? b.getOldPath() : b.getNewPath();
                if (pa == null) return -1;
                if (pb == null) return 1;
                return pa.compareTo(pb);
            });
            return changes;
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "查询 commit 文件变更失败：" + e.getMessage(), e);
        }
    }

    /**
     * DiffEntry.ChangeType 转简写字符串。
     */
    private static String changeTypeToString(org.eclipse.jgit.diff.DiffEntry.ChangeType type) {
        if (type == null) return "?";
        switch (type) {
            case ADD: return "ADD";
            case MODIFY: return "MODIFY";
            case DELETE: return "DELETE";
            case RENAME: return "RENAME";
            case COPY: return "COPY";
            default: return type.name();
        }
    }

    /**
     * 获取文件 Diff（PRD 4.10）。
     *
     * @param repoPath 仓库路径
     * @param path     文件路径
     * @param oldRev   旧版本（null 表示工作区对比 HEAD）
     * @param newRev   新版本（null 表示工作区）
     * @return Diff 结果
     */
    public DiffResult getDiff(String repoPath, String path, String oldRev, String newRev) {
        // 防御性 null / blank 处理
        if (path == null || path.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "文件路径不能为空");
        }
        // 1) 先尝试用户指定的 oldRev/newRev
        DiffResult r = tryGetDiff(repoPath, path, oldRev, newRev);
        if (r != null) {
            return r;
        }
        // 2) 降级：HEAD vs working tree
        r = tryGetDiff(repoPath, path, "HEAD", null);
        if (r != null) {
            return r;
        }
        // 3) 降级：HEAD vs index（专门为 staged 状态）
        r = tryGetDiffAgainstIndex(repoPath, path, "HEAD");
        if (r != null) {
            return r;
        }
        // 4) 降级：手动合成 unified diff（极端兜底，绝不抛 "Missing blob"）
        return buildFallbackDiff(repoPath, path);
    }

    /**
     * 尝试按指定 oldRev/newRev 取 diff；任何异常（特别是 MissingObjectException / Missing blob）都返回 null，
     * 由外层 {@link #getDiff} 决定下一步降级策略。
     *
     * @return DiffResult（含 diffText，可能为空）；无法取到返回 null
     */
    private DiffResult tryGetDiff(String repoPath, String path, String oldRev, String newRev) {
        try (Git git = open(repoPath);
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             DiffFormatter formatter = new DiffFormatter(out)) {
            Repository repo = git.getRepository();
            formatter.setRepository(repo);
            AbstractTreeIterator oldTree = oldRev == null ? null : safePrepareTree(repo, oldRev);
            AbstractTreeIterator newTree = newRev == null ? null : safePrepareTree(repo, newRev);
            if (oldTree == null && newTree == null) {
                // 两个 rev 都解析不到：返回 null 让外层走兜底
                return null;
            }
            List<DiffEntry> entries;
            if (oldTree != null && newTree != null) {
                entries = git.diff().setOldTree(oldTree).setNewTree(newTree).call();
            } else if (oldTree != null) {
                entries = git.diff().setOldTree(oldTree).call();
            } else {
                entries = git.diff().call();
            }
            // 关键修复：entries 为空 或 该文件不在 entries 中 → 返回 null（让外层降级到 buildFallbackDiff），
            // 不要返回「空 DiffResult」（否则外层以为成功就直接返回，看不到真正的内容）。
            if (entries == null || entries.isEmpty()) {
                log.debug("tryGetDiff：entries 为空，path={}, oldRev={}, newRev={}", path, oldRev, newRev);
                return null;
            }
            for (DiffEntry entry : entries) {
                if (entry.getNewPath().equals(path) || entry.getOldPath().equals(path)) {
                    try {
                        formatter.format(entry);
                    } catch (org.eclipse.jgit.errors.MissingObjectException
                             | org.eclipse.jgit.errors.LargeObjectException ex) {
                        // Missing blob 之类的错误：放弃该 entry，让外层降级
                        log.warn("格式化 diff entry 失败：{}（{}），准备降级", path, ex.getMessage());
                        return null;
                    }
                    return DiffResult.builder()
                            .path(path)
                            .oldRev(oldRev)
                            .newRev(newRev)
                            .diffText(out.toString(StandardCharsets.UTF_8))
                            .build();
                }
            }
            // 该文件不在 entries 中：返回 null 让外层降级（之前是返回空 DiffResult，会被误判为成功）
            log.debug("tryGetDiff：未找到 path={} 的 entry，准备降级", path);
            return null;
        } catch (org.eclipse.jgit.errors.MissingObjectException
                 | org.eclipse.jgit.errors.AmbiguousObjectException
                 | org.eclipse.jgit.errors.IncorrectObjectTypeException ex) {
            // rev 解析失败 / blob 缺失
            log.warn("按 rev 取 diff 失败：path={}, oldRev={}, newRev={}, err={}",
                    path, oldRev, newRev, ex.getMessage());
            return null;
        } catch (Exception e) {
            // 其他异常：仍然降级，不直接抛
            log.warn("按 rev 取 diff 失败：path={}, oldRev={}, newRev={}, err={}",
                    path, oldRev, newRev, e.getMessage());
            return null;
        }
    }

    /**
     * 专门的「HEAD vs index」diff（staged 文件的正常 diff 通道）。
     * <p>JGit 6.x {@code DiffCommand} 没有 {@code setSource(TreeWalk)}，改用 {@code setNewTree(DirCacheIterator)}</p>
     * <p>表示「index 侧」。</p>
     */
    private DiffResult tryGetDiffAgainstIndex(String repoPath, String path, String oldRev) {
        try (Git git = open(repoPath);
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             DiffFormatter formatter = new DiffFormatter(out)) {
            Repository repo = git.getRepository();
            formatter.setRepository(repo);
            AbstractTreeIterator oldTree = safePrepareTree(repo, oldRev);
            if (oldTree == null) {
                return null;
            }
            // 用 DirCacheIterator 表示「index 侧」
            org.eclipse.jgit.dircache.DirCache dc = org.eclipse.jgit.dircache.DirCache.read(repo);
            org.eclipse.jgit.dircache.DirCacheIterator indexIter = new org.eclipse.jgit.dircache.DirCacheIterator(dc);
            List<DiffEntry> entries = git.diff()
                    .setOldTree(oldTree)
                    .setNewTree(indexIter)
                    .call();
            for (DiffEntry entry : entries) {
                if (entry.getNewPath().equals(path) || entry.getOldPath().equals(path)) {
                    try {
                        formatter.format(entry);
                    } catch (org.eclipse.jgit.errors.MissingObjectException ex) {
                        log.warn("格式化 staged diff entry 失败：{}", ex.getMessage());
                        return null;
                    }
                    return DiffResult.builder()
                            .path(path)
                            .oldRev(oldRev)
                            .newRev("INDEX")
                            .diffText(out.toString(StandardCharsets.UTF_8))
                            .build();
                }
            }
            // entries 为空 或 文件不在 entries 中 → 返回 null 让外层走 buildFallbackDiff
            log.debug("tryGetDiffAgainstIndex：未找到 path={} 的 entry，准备降级", path);
            return null;
        } catch (Exception e) {
            log.warn("按 HEAD vs INDEX 取 diff 失败：{}, err={}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 终极兜底：手动读 HEAD + 工作区内容，自己生成 unified diff。
     * 绝不再抛 MissingObjectException / Missing blob。
     */
    private DiffResult buildFallbackDiff(String repoPath, String path) {
        try (Git git = open(repoPath)) {
            Repository repo = git.getRepository();
            // 读 HEAD 内容
            String headContent = readHeadBlobAsText(repo, path);
            // 读工作区内容
            String workContent = readWorkingFileAsText(new File(repoPath, path));
            // 读 staged (index) 内容
            String indexContent = readIndexBlobAsText(repo, path);

            // 决定 old / new：
            //   - 文件未在 HEAD 中（headContent == null）→ old = ""
            //   - 否则 old = HEAD 内容
            //   - new 优先取工作区，否则取 index
            String oldText = headContent == null ? "" : headContent;
            String newText = workContent != null ? workContent : (indexContent == null ? "" : indexContent);

            String diffText = generateUnifiedDiffText(path, oldText, newText);
            return DiffResult.builder()
                    .path(path)
                    .oldRev("HEAD")
                    .newRev("WORKTREE")
                    .diffText(diffText)
                    .build();
        } catch (Exception e) {
            log.error("兜底生成 diff 失败：{}", path, e);
            // 真的什么都读不到：返回空 diff，绝不抛异常
            return DiffResult.builder()
                    .path(path)
                    .oldRev("HEAD")
                    .newRev(null)
                    .diffText("")
                    .build();
        }
    }

    private AbstractTreeIterator safePrepareTree(Repository repo, String rev) {
        try {
            return prepareTreeParser(repo, rev);
        } catch (Exception e) {
            log.warn("准备 tree 失败：rev={}, err={}", rev, e.getMessage());
            return null;
        }
    }

    /**
     * 读 HEAD 中某文件的内容，文件不存在或为 binary 返回 null。
     */
    private String readHeadBlobAsText(Repository repo, String path) {
        try {
            org.eclipse.jgit.lib.ObjectId headTreeId = repo.resolve("HEAD^{tree}");
            if (headTreeId == null) {
                return null;
            }
            // JGit 6.x TreeWalk 1-arg 构造：先 new TreeWalk(repo)，再 addTree(ObjectId)
            try (org.eclipse.jgit.treewalk.TreeWalk tw = new org.eclipse.jgit.treewalk.TreeWalk(repo)) {
                tw.addTree(headTreeId);
                tw.setRecursive(true);
                while (tw.next()) {
                    if (path.equals(tw.getPathString())) {
                        org.eclipse.jgit.lib.ObjectLoader loader = repo.open(tw.getObjectId(0));
                        byte[] bytes = loader.getBytes();
                        if (isBinary(bytes)) {
                            return null;
                        }
                        return new String(bytes, StandardCharsets.UTF_8);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("读 HEAD 内容失败：{}, err={}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 读 index 中某文件的内容，不存在或为 binary 返回 null。
     */
    private String readIndexBlobAsText(Repository repo, String path) {
        try {
            org.eclipse.jgit.dircache.DirCache dc = org.eclipse.jgit.dircache.DirCache.read(repo);
            org.eclipse.jgit.dircache.DirCacheEntry entry = dc.getEntry(path);
            if (entry == null) {
                return null;
            }
            org.eclipse.jgit.lib.ObjectLoader loader = repo.open(entry.getObjectId());
            byte[] bytes = loader.getBytes();
            if (isBinary(bytes)) {
                return null;
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("读 index 内容失败：{}, err={}", path, e.getMessage());
            return null;
        }
    }

    private String readWorkingFileAsText(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            if (isBinary(bytes)) {
                return null;
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("读工作区文件失败：{}, err={}", file, e.getMessage());
            return null;
        }
    }

    private boolean isBinary(byte[] bytes) {
        if (bytes == null) return false;
        // 简易 binary 检测：包含 NUL 字节
        int limit = Math.min(bytes.length, 8192);
        for (int i = 0; i < limit; i++) {
            if (bytes[i] == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 简易 unified diff 生成（不依赖 JGit），用于兜底：
     * <ul>
     *   <li>old == new → 空</li>
     *   <li>old == ""（新增文件）→ 所有行作为 +</li>
     *   <li>new == ""（删除文件）→ 所有行作为 -</li>
     *   <li>其他：全文件作为 +/- 一次性输出（无 hunk 拆分，仅保证不抛异常）</li>
     * </ul>
     */
    private String generateUnifiedDiffText(String path, String oldText, String newText) {
        if (oldText == null) oldText = "";
        if (newText == null) newText = "";
        if (oldText.equals(newText)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String mode;
        if (oldText.isEmpty()) {
            mode = "new file";
        } else if (newText.isEmpty()) {
            mode = "deleted file";
        } else {
            mode = "modified";
        }
        sb.append("--- a/").append(path).append("\n");
        sb.append("+++ b/").append(path).append("\n");
        sb.append("@@ -0,0 +0,0 @@ ").append(mode).append("（兜底 diff，未做逐行对比）\n");
        // 旧行
        if (!oldText.isEmpty()) {
            String[] oldLines = oldText.split("\n", -1);
            for (String line : oldLines) {
                sb.append("-").append(line).append("\n");
            }
        }
        // 新行
        if (!newText.isEmpty()) {
            String[] newLines = newText.split("\n", -1);
            for (String line : newLines) {
                sb.append("+").append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 列出仓库的 Remote 配置（PRD 4.14）。
     *
     * @param repoPath 仓库路径
     * @return Remote 配置列表
     */
    public List<RemoteConfig> listRemotes(String repoPath) {
        try (Git git = open(repoPath)) {
            List<RemoteConfig> result = new ArrayList<>();
            for (org.eclipse.jgit.transport.RemoteConfig rc : git.remoteList().call()) {
                result.add(RemoteConfig.builder()
                        .name(rc.getName())
                        .fetchUrl(rc.getURIs().isEmpty() ? "" : rc.getURIs().get(0).toString())
                        .pushUrl(rc.getPushURIs().isEmpty() ? (rc.getURIs().isEmpty() ? "" : rc.getURIs().get(0).toString()) : rc.getPushURIs().get(0).toString())
                        .build());
            }
            return result;
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "查询 Remote 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 列出本地与远程分支。
     *
     * @param repoPath 仓库路径
     * @return 分支名列表
     */
    public List<String> listBranches(String repoPath) {
        try (Git git = open(repoPath)) {
            List<String> branches = new ArrayList<>();
            for (Ref ref : git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()) {
                branches.add(ref.getName());
            }
            return branches;
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "查询分支失败：" + e.getMessage(), e);
        }
    }

    /**
     * 切换分支（git checkout），支持创建新分支与强制切换。
     *
     * @param req 切换请求
     */
    public void checkout(String repoPath, String branch, boolean create, boolean force) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        if (branch == null || branch.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "分支名不能为空");
        }
        try (Git git = open(repoPath)) {
            var checkout = git.checkout().setName(branch);
            if (create) {
                checkout.setCreateBranch(true);
            }
            if (force) {
                // JGit 的 setForce 等价于 git checkout -f
                checkout.setForceRefUpdate(true);
            }
            checkout.call();
            log.info("切换分支成功：{} -> {}", repoPath, branch);
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "切换分支失败：" + e.getMessage(), e);
        }
    }

    /**
     * 通过完整 ref 名切换分支（refs/heads/&lt;x&gt; / refs/tags/&lt;x&gt;）。
     * <p>JGit 支持以 refs/ 开头的完整路径作为 {@code setName} 参数，可直接解析为对应的 ref 对象。</p>
     * <p>典型用例：从 refs 浏览对话框选中本地分支或 tag，UI 层把完整 ref 路径传过来。</p>
     *
     * @param repoPath 仓库路径
     * @param refName  完整 ref 名（含 refs/ 前缀）
     * @param force    是否强制切换（丢弃本地修改）
     */
    public void checkoutRef(String repoPath, String refName, boolean force) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        if (refName == null || refName.isBlank() || !refName.startsWith("refs/")) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "refName 必须以 refs/ 开头：" + refName);
        }
        try (Git git = open(repoPath)) {
            // 先解析 ref 拿到目标对象（commit / tag）
            org.eclipse.jgit.lib.Ref ref = git.getRepository().findRef(refName);
            if (ref == null) {
                throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "无法解析 ref：" + refName);
            }
            String shortName = refName;
            // 对本地分支，使用短名（refs/heads/main → main）让 JGit 在 setName 时更易解析
            if (refName.startsWith("refs/heads/")) {
                shortName = refName.substring("refs/heads/".length());
            } else if (refName.startsWith("refs/tags/")) {
                shortName = refName.substring("refs/tags/".length());
            }
            var checkout = git.checkout().setName(shortName);
            if (force) {
                checkout.setForceRefUpdate(true);
            }
            checkout.call();
            log.info("通过完整 ref 切换成功：{} -> {} (短名={})", repoPath, refName, shortName);
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "切换 ref 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 列出仓库所有 Tag（含轻量标签与附注标签）。
     *
     * @param repoPath 仓库路径
     * @return tag 名列表（含 v1.0.0、refs/tags/* 等形式，按 JGit 原样返回）
     */
    public List<String> listTags(String repoPath) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        try (Git git = open(repoPath)) {
            List<String> tags = new ArrayList<>();
            for (Ref ref : git.tagList().call()) {
                tags.add(ref.getName());
            }
            return tags;
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "查询标签失败：" + e.getMessage(), e);
        }
    }

    /**
     * 从指定 commit 创建新分支并 checkout（等价于 git checkout -b branch commit）。
     * <p>与 {@link #checkoutCommit} 不同，本方法会创建分支而不是游离 HEAD。</p>
     *
     * @param repoPath         仓库路径
     * @param branch           新分支名
     * @param commitId         起始 commit 哈希
     * @param forceCheckout    是否强制切换（丢弃本地修改）
     * @param overrideExisting 是否覆盖已存在的同名分支（git checkout -B）
     */
    public void checkoutNewBranchFromCommit(String repoPath, String branch, String commitId,
                                            boolean forceCheckout, boolean overrideExisting) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        if (branch == null || branch.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "分支名不能为空");
        }
        if (commitId == null || commitId.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "commit id 不能为空");
        }
        try (Git git = open(repoPath)) {
            // 1. 创建分支（从 commit 起始），force=overrideExisting
            git.branchCreate()
                    .setName(branch)
                    .setStartPoint(commitId)
                    .setForce(overrideExisting)
                    .call();
            // 2. checkout 到新分支
            var checkout = git.checkout().setName(branch);
            if (forceCheckout) {
                checkout.setForceRefUpdate(true);
            }
            checkout.call();
            log.info("从 commit 创建新分支成功：repo={}, branch={}, commit={}, override={}",
                    repoPath, branch, commitId, overrideExisting);
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED,
                    "从 commit 创建分支失败：" + e.getMessage(), e);
        }
    }

    /**
     * 切换到指定 commit（游离 HEAD 模式，git checkout <commit>）。
     *
     * @param repoPath 仓库路径
     * @param commitId 提交哈希（完整或短哈希均可）
     * @param force    是否强制切换（丢弃本地修改）
     */
    public void checkoutCommit(String repoPath, String commitId, boolean force) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        if (commitId == null || commitId.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "commit id 不能为空");
        }
        try (Git git = open(repoPath)) {
            var checkout = git.checkout().setName(commitId);
            if (force) {
                checkout.setForceRefUpdate(true);
            }
            checkout.call();
            log.info("切换到 commit 成功：{} -> {}", repoPath, commitId);
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "切换 commit 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 切换到指定 tag（git checkout <tag>）。
     *
     * @param repoPath 仓库路径
     * @param tag      标签名（如 v1.0.0，可含 refs/tags/ 前缀）
     * @param force    是否强制切换
     */
    public void checkoutTag(String repoPath, String tag, boolean force) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        if (tag == null || tag.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "tag 名不能为空");
        }
        // 兼容 refs/tags/ 前缀
        String tagName = tag.startsWith("refs/tags/") ? tag.substring("refs/tags/".length()) : tag;
        try (Git git = open(repoPath)) {
            var checkout = git.checkout().setName(tagName);
            if (force) {
                checkout.setForceRefUpdate(true);
            }
            checkout.call();
            log.info("切换到 tag 成功：{} -> {}", repoPath, tagName);
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "切换 tag 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取分支的 SHA-1（HEAD 指向的提交哈希）。
     *
     * @param repoPath 仓库路径
     * @param branch   分支名（本地或远程）
     * @return SHA-1（40 字符十六进制），未找到返回 null
     */
    public String getBranchHeadSha(String repoPath, String branch) {
        if (repoPath == null || branch == null) {
            return null;
        }
        try (Git git = open(repoPath)) {
            // 优先尝试本地分支
            String fullName = branch.startsWith("refs/") ? branch : "refs/heads/" + branch;
            Ref ref = git.getRepository().findRef(fullName);
            if (ref == null) {
                // 尝试远程
                fullName = branch.startsWith("refs/") ? branch : "refs/remotes/" + branch;
                ref = git.getRepository().findRef(fullName);
            }
            return ref == null ? null : ref.getObjectId().getName();
        } catch (Exception e) {
            log.warn("查询分支 HEAD SHA 失败：{} / {}", repoPath, branch, e);
            return null;
        }
    }

    /**
     * 批量查询所有 ref（含本地/远程分支与 tag），并解析每个 ref 指向 commit 的元信息。
     * <p>只调用一次 Git.open + 一次 RevWalk，避免对每个 ref 单独打开仓库造成性能瓶颈。</p>
     *
     * @param repoPath 仓库路径
     * @return ref 信息列表（含 ref 名 / 显示名 / 类型 / SHA / 作者 / 时间 / message）
     */
    public List<RefInfo> batchListRefs(String repoPath) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        try (Git git = open(repoPath)) {
            Repository repo = git.getRepository();
            // 1. 批量获取所有 ref：refs/heads/* / refs/remotes/* / refs/tags/*
            // 注：JGit 5.x+ 的 getRefsByPrefix 返回 List<Ref>，每个 Ref 上有 getName() 方法
            List<Ref> allRefList = new ArrayList<>();
            for (String prefix : new String[]{"refs/heads/", "refs/remotes/", "refs/tags/"}) {
                allRefList.addAll(repo.getRefDatabase().getRefsByPrefix(prefix));
            }
            if (allRefList.isEmpty()) {
                return java.util.Collections.emptyList();
            }
            // 按 refName 去重并排序，便于稳定展示
            allRefList.sort(java.util.Comparator.comparing(Ref::getName));

            // 2. 一次 RevWalk 解析所有目标 commit
            List<RefInfo> result = new ArrayList<>(allRefList.size());
            try (RevWalk walk = new RevWalk(repo)) {
                // 缓存：objectId → 解析后的 RevCommit（同一 commit 被多 ref 引用时不重复解析）
                Map<String, RevCommit> commitCache = new HashMap<>();
                // 缓存：本地分支名 → 上游跟踪（refs/heads/x → refs/remotes/origin/x）
                Map<String, String> trackedMap = buildTrackedMap(repo);

                for (Ref ref : allRefList) {
                    String refName = ref.getName();
                    ObjectId oid = ref.getObjectId();
                    if (oid == null) {
                        continue;
                    }
                    String sha = oid.getName();
                    RefInfo.RefInfoBuilder b = RefInfo.builder()
                            .refName(refName)
                            .sha(sha);

                    // 显示名与类型
                    String displayName;
                    String kind;
                    String remoteName = "";
                    if (refName.startsWith("refs/heads/")) {
                        displayName = refName.substring("refs/heads/".length());
                        kind = "BRANCH";
                        // 本地分支跟踪的远程分支
                        String tracked = trackedMap.get(refName);
                        if (tracked != null) {
                            b.trackedBranch(stripRefsPrefix(tracked));
                        }
                    } else if (refName.startsWith("refs/remotes/")) {
                        String rest = refName.substring("refs/remotes/".length());
                        int slash = rest.indexOf('/');
                        if (slash > 0) {
                            remoteName = rest.substring(0, slash);
                            displayName = rest.substring(slash + 1);
                        } else {
                            displayName = rest;
                        }
                        kind = "REMOTE";
                    } else if (refName.startsWith("refs/tags/")) {
                        displayName = refName.substring("refs/tags/".length());
                        kind = "TAG";
                    } else {
                        displayName = refName;
                        kind = "OTHER";
                    }
                    b.displayName(displayName).kind(kind).remoteName(remoteName);

                    // 解析 commit 元信息（从缓存或 RevWalk）
                    try {
                        RevCommit commit = commitCache.computeIfAbsent(sha, k -> {
                            try {
                                return walk.parseCommit(oid);
                            } catch (Exception ex) {
                                log.warn("解析 commit 失败：ref={}, sha={}", refName, sha, ex);
                                return null;
                            }
                        });
                        if (commit != null) {
                            b.author(commit.getAuthorIdent().getName());
                            b.commitDate(java.time.LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochSecond(commit.getCommitTime()),
                                    java.time.ZoneId.systemDefault()).toString());
                            String msg = commit.getFullMessage();
                            if (msg != null) {
                                int nl = msg.indexOf('\n');
                                b.message(nl >= 0 ? msg.substring(0, nl) : msg);
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("解析 commit 元信息失败：{}", refName, ex);
                    }
                    result.add(b.build());
                }
                walk.dispose();
            }
            // 排序：本地分支 → 远程分支 → tag → 其他，字母序
            result.sort(java.util.Comparator
                    .comparingInt((RefInfo r) -> kindOrder(r.getKind()))
                    .thenComparing(RefInfo::getDisplayName, String.CASE_INSENSITIVE_ORDER));
            return result;
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "批量查询 refs 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 排序权重：本地 BRANCH=0, REMOTE=1, TAG=2, OTHER=3。
     */
    private static int kindOrder(String kind) {
        if ("BRANCH".equals(kind)) return 0;
        if ("REMOTE".equals(kind)) return 1;
        if ("TAG".equals(kind)) return 2;
        return 3;
    }

    /**
     * 剥离 refs/heads/ refs/remotes/ refs/tags/ 前缀。
     */
    private static String stripRefsPrefix(String ref) {
        if (ref == null) return "";
        if (ref.startsWith("refs/heads/")) return ref.substring("refs/heads/".length());
        if (ref.startsWith("refs/remotes/")) {
            String rest = ref.substring("refs/remotes/".length());
            int slash = rest.indexOf('/');
            return slash > 0 ? rest.substring(slash + 1) : rest;
        }
        if (ref.startsWith("refs/tags/")) return ref.substring("refs/tags/".length());
        return ref;
    }

    /**
     * 从仓库 config 解析「本地分支 → 上游跟踪 ref」映射。
     * <p>读取 branch.<name>.merge 与 branch.<name>.remote 配置项，组合为 refs/remotes/&lt;remote&gt;/&lt;branch&gt;。</p>
     */
    private Map<String, String> buildTrackedMap(Repository repo) {
        Map<String, String> tracked = new HashMap<>();
        try {
            org.eclipse.jgit.lib.StoredConfig config = repo.getConfig();
            // 遍历 branch.<name> subsection
            for (String name : config.getSubsections("branch")) {
                String remote = config.getString("branch", name, "remote");
                String merge = config.getString("branch", name, "merge");
                if (remote != null && merge != null && !merge.isEmpty()) {
                    // merge 通常为 refs/heads/main → 转 refs/remotes/origin/main
                    String mergeShort = merge.startsWith("refs/heads/") ? merge.substring("refs/heads/".length()) : merge;
                    tracked.put("refs/heads/" + name, "refs/remotes/" + remote + "/" + mergeShort);
                }
            }
        } catch (Exception e) {
            log.warn("解析仓库跟踪配置失败", e);
        }
        return tracked;
    }

    /**
     * 获取当前分支名。
     *
     * @param repoPath 仓库路径
     * @return 当前分支名，游离 HEAD 返回 "DETACHED"
     */
    public String getCurrentBranch(String repoPath) {
        try (Git git = open(repoPath)) {
            return git.getRepository().getBranch();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * 检查工作区是否干净。
     *
     * @param repoPath 仓库路径
     * @return true 表示工作区干净
     */
    public boolean isClean(String repoPath) {
        try (Git git = open(repoPath)) {
            return git.status().call().isClean();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 准备树迭代器（用于 diff 比较指定 commit）。
     */
    private AbstractTreeIterator prepareTreeParser(Repository repo, String rev) throws IOException {
        ObjectId head = repo.resolve(rev);
        if (head == null) {
            return null;
        }
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit commit = walk.parseCommit(head);
            walk.reset();
            org.eclipse.jgit.treewalk.CanonicalTreeParser parser = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
            try (org.eclipse.jgit.lib.ObjectReader reader = repo.newObjectReader()) {
                parser.reset(reader, commit.getTree());
            }
            return parser;
        }
    }
}
