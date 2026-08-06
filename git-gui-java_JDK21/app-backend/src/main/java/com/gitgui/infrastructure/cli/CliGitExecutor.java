package com.gitgui.infrastructure.cli;

import com.gitgui.core.async.ProgressCallback;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Git CLI 执行器（统一适配器）
 * <p>所有 Git 操作通过系统 {@code git} CLI 完成。使用 {@link ProcessBuilder} 参数数组确保安全，
 * 统一 UTF-8 编码 + {@code core.quotepath=false}。</p>
 *
 * <p>查询操作同步返回，长耗时操作（push/pull/clone/fetch）通过 {@link GitProcessBuilder} 异步执行。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class CliGitExecutor {

    private static final Logger log = LoggerFactory.getLogger(CliGitExecutor.class);

    private final GitOutputParser outputParser = new GitOutputParser();

    // ========== 基础执行方法 ==========

    /**
     * 同步执行 git 命令，返回 stdout 字符串。
     *
     * @param repoPath     仓库路径（可为 null，如 clone）
     * @param args         git 命令参数
     * @param allowNonZero 是否允许非零退出码（查询类通常允许）
     * @return stdout 输出
     * @throws GitGuiException 执行失败（仅当 allowNonZero=false 时因退出码非零抛）
     */
    private String execute(String repoPath, List<String> args, boolean allowNonZero) {
        return GitProcessBuilder.executeQuietly(repoPath, args, null, allowNonZero);
    }

    /**
     * 同步执行 git 命令，允许非零退出码，返回 stdout 行列表。
     */
    private List<String> executeLines(String repoPath, List<String> args, boolean allowNonZero) {
        String out = execute(repoPath, args, allowNonZero);
        return out == null || out.isBlank() ? List.of() : List.of(out.split("\n"));
    }

    /**
     * 异步执行 git 命令（用于长耗时操作，不允许非零退出码）。
     */
    private void executeAsync(String repoPath, List<String> args, ProgressCallback callback) {
        GitProcessBuilder.execute(repoPath, args, callback);
    }

    // ========== 查询操作 ==========

    /**
     * 获取工作区文件状态列表。
     */
    public List<FileStatus> getStatus(String repoPath, boolean showUntracked) {
        List<String> args = new ArrayList<>(List.of("status", "--porcelain", "-z"));
        if (!showUntracked) {
            args.add("-uno");
        }
        try {
            String output = execute(repoPath, args, true);
            return outputParser.parseStatus(output);
        } catch (GitGuiException e) {
            // 空仓库或无提交时 status 也可以正常执行
            log.debug("获取状态返回异常：{}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取当前分支名。
     */
    public String getCurrentBranch(String repoPath) {
        try {
            String out = execute(repoPath, List.of("rev-parse", "--abbrev-ref", "HEAD"), true);
            if (out == null || out.isBlank()) {
                return "UNKNOWN";
            }
            String trimmed = out.trim();
            // 排除 git 错误消息（rev-parse 对无 HEAD 仓库返回 fatal:...）
            if (trimmed.startsWith("fatal:") || trimmed.contains("fatal:")) {
                return "UNKNOWN";
            }
            if (trimmed.equals("HEAD")) {
                return "DETACHED_HEAD";
            }
            return trimmed;
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    /**
     * 获取分支 HEAD SHA-1。
     */
    public String getBranchHeadSha(String repoPath, String branch) {
        if (repoPath == null || branch == null) {
            return null;
        }
        try {
            String out = execute(repoPath, List.of("rev-parse", branch), true);
            return out == null || out.isBlank() ? null : out.trim();
        } catch (Exception e) {
            log.debug("查询分支 SHA 失败：{} / {}", repoPath, branch);
            return null;
        }
    }

    /**
     * 检查工作区是否干净。
     */
    public boolean isClean(String repoPath) {
        try {
            String out = execute(repoPath, List.of("status", "--porcelain"), true);
            return out == null || out.isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 列出所有分支。
     */
    public List<String> listBranches(String repoPath) {
        try {
            String out = execute(repoPath, List.of("branch", "-a", "--format=%(refname:short)"), true);
            if (out == null || out.isBlank()) {
                return List.of();
            }
            return out.lines()
                      .map(String::trim)
                      .filter(s -> !s.isEmpty() && !s.startsWith("HEAD"))
                      .toList();
        } catch (Exception e) {
            log.warn("列出分支失败：{}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 列出所有 Tag。
     */
    public List<String> listTags(String repoPath) {
        try {
            String out = execute(repoPath, List.of("tag", "-l"), true);
            if (out == null || out.isBlank()) {
                return List.of();
            }
            return out.lines()
                      .map(String::trim)
                      .filter(s -> !s.isEmpty())
                      .toList();
        } catch (Exception e) {
            log.warn("列出标签失败：{}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 列出 Remote 配置。
     */
    public List<RemoteConfig> listRemotes(String repoPath) {
        try {
            String out = execute(repoPath, List.of("remote", "-v"), true);
            return outputParser.parseRemotes(out);
        } catch (Exception e) {
            log.warn("查询 Remote 失败：{}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 分页获取提交日志。
     */
    public List<LogEntry> getLog(String repoPath, int page, int pageSize) {
        int skip = Math.max(0, (page - 1) * pageSize);
        String format = "%H%x00%h%x00%an%x00%ae%x00%aI%x00%s%x00%b%x00%P";
        try {
            String out = execute(repoPath, List.of("log", "--skip=" + skip, "-n" + pageSize, "--format=" + format, "--date=iso-strict"), true);
            return outputParser.parseLog(out);
        } catch (Exception e) {
            log.warn("获取日志失败：{}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取 commit 的文件变更列表。
     */
    public List<FileChange> getCommitChanges(String repoPath, String commitId) {
        if (repoPath == null || repoPath.isBlank() || commitId == null || commitId.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径或 commit id 不能为空");
        }
        try {
            String out = execute(repoPath, List.of("show", "--name-status", "--format=", commitId), true);
            return outputParser.parseFileChanges(out);
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, "查询 commit 文件变更失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取文件 Diff。
     */
    public DiffResult getDiff(String repoPath, String path, String oldRev, String newRev) {
        if (path == null || path.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "文件路径不能为空");
        }
        // 构建参数
        List<String> args = new ArrayList<>(List.of("diff", "--no-color"));
        if (oldRev != null && !oldRev.isBlank()) {
            args.add(oldRev);
        }
        if (newRev != null && !newRev.isBlank()) {
            args.add(newRev);
        }
        args.add("--");
        args.add(path);

        try {
            // 尝试标准 diff
            String out = execute(repoPath, args, true);
            if (out != null && !out.isBlank()) {
                return DiffResult.builder()
                                 .path(path)
                                 .oldRev(oldRev)
                                 .newRev(newRev)
                                 .diffText(outputParser.parseDiff(out))
                                 .build();
            }
        } catch (Exception e) {
            log.debug("标准 diff 失败：{}，尝试降级", e.getMessage());
        }

        // 降级：HEAD vs working tree
        try {
            List<String> altArgs = new ArrayList<>(List.of("diff", "--no-color", "HEAD", "--", path));
            String out = execute(repoPath, altArgs, true);
            if (out != null && !out.isBlank()) {
                return DiffResult.builder()
                                 .path(path)
                                 .oldRev("HEAD")
                                 .newRev(newRev)
                                 .diffText(outputParser.parseDiff(out))
                                 .build();
            }
        } catch (Exception e2) {
            log.debug("HEAD diff 失败：{}", e2.getMessage());
        }

        // 终极兜底：手动读文件生成 diff
        return buildFallbackDiff(repoPath, path);
    }

    /**
     * 兜底 diff：手动读 HEAD + 工作区内容生成 unified diff。
     */
    private DiffResult buildFallbackDiff(String repoPath, String path) {
        try {
            String headContent = readFileFromHead(repoPath, path);
            String workContent = readWorkingFile(repoPath, path);

            String oldText = headContent == null ? "" : headContent;
            String newText = workContent == null ? "" : workContent;

            StringBuilder sb = new StringBuilder();
            sb.append("--- a/")
              .append(path)
              .append("\n");
            sb.append("+++ b/")
              .append(path)
              .append("\n");
            String mode = oldText.isEmpty() ? "new file" : (newText.isEmpty() ? "deleted file" : "modified");
            sb.append("@@ -0,0 +0,0 @@ ")
              .append(mode)
              .append("\n");

            if (!oldText.isEmpty()) {
                for (String line : oldText.split("\n", -1)) {
                    sb.append("-")
                      .append(line)
                      .append("\n");
                }
            }
            if (!newText.isEmpty()) {
                for (String line : newText.split("\n", -1)) {
                    sb.append("+")
                      .append(line)
                      .append("\n");
                }
            }
            return DiffResult.builder()
                             .path(path)
                             .oldRev("HEAD")
                             .newRev("WORKTREE")
                             .diffText(sb.toString())
                             .build();
        } catch (Exception e) {
            log.error("兜底 diff 失败：{}", path, e);
            return DiffResult.builder()
                             .path(path)
                             .oldRev("HEAD")
                             .newRev(null)
                             .diffText("")
                             .build();
        }
    }

    /**
     * 批量查询所有 ref。
     */
    public List<RefInfo> batchListRefs(String repoPath) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        // 使用 TAB 作为分隔符，兼容所有 git 版本（旧版不支持 %x00）
        String format = "%(refname)%09%(objectname)%09%(committerdate:iso-strict)%09%(subject)";
        try {
            String out = execute(repoPath,
                                 List.of("for-each-ref", "--format=" + format, "--sort=-committerdate", "refs/heads/", "refs/remotes/", "refs/tags/"),
                                 true);
            return outputParser.parseRefs(out);
        } catch (Exception e) {
            log.warn("批量查询 ref 失败：{}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 查询两个 ref 之间的 commit 列表（顺序：新→旧）。
     * <p>等价于 {@code git log fromRef..toRef}。当 {@code fromRef} 为 null/空/不存在时，
     * 降级为查询 {@code toRef} 的全部历史（首次推送场景：本地分支未设置上游跟踪）。</p>
     * <p>复用 {@code getLog} 的 NUL 分隔 format 和 {@link GitOutputParser#parseLog(String)}，
     * 渲染层的列定义可与主窗口日志区域一致。</p>
     *
     * @param repoPath 仓库路径
     * @param fromRef  起点 ref（不包含），可为 null/空
     * @param toRef    终点 ref（包含），不能为空
     * @param limit    最大数量（≤0 取全部）
     * @return commit 列表；查询失败返回空列表
     */
    public List<LogEntry> getCommitsBetween(String repoPath, String fromRef, String toRef, int limit) {
        if (repoPath == null || repoPath.isBlank()) {
            return List.of();
        }
        if (toRef == null || toRef.isBlank()) {
            return List.of();
        }
        String format = "%H%x00%h%x00%an%x00%ae%x00%aI%x00%s%x00%b%x00%P";
        List<String> args = new ArrayList<>();
        args.add("log");
        if (limit > 0) {
            args.add("-n" + limit);
        }
        args.add(String.format("--format=%s", format));
        args.add("--date=iso-strict");
        // 构建范围表达式 fromRef..toRef
        StringBuilder range = new StringBuilder();
        if (fromRef != null && !fromRef.isBlank()) {
            range.append(fromRef);
        }
        range.append("..")
             .append(toRef);
        args.add(range.toString());
        try {
            String out = execute(repoPath, args, true);
            return outputParser.parseLog(out);
        } catch (Exception e) {
            log.debug("查询 commit 范围失败：{}..{}", fromRef, toRef);
            return List.of();
        }
    }

    /**
     * 检查远程 ref（如 {@code origin/master}）是否存在。
     * <p>用 {@code git rev-parse --verify --quiet <ref>}，存在 → 返回 ref 全 SHA；不存在 → 输出空；</p>
     *
     * @param repoPath 仓库路径
     * @param remote   远程名（如 origin）
     * @param branch   分支名（如 master）
     * @return true=远端 ref 存在；false=不存在
     */
    public boolean remoteRefExists(String repoPath, String remote, String branch) {
        if (repoPath == null || repoPath.isBlank() || remote == null || remote.isBlank() || branch == null || branch.isBlank()) {
            return false;
        }
        try {
            String out = execute(repoPath, List.of("rev-parse", "--verify", "--quiet", remote + "/" + branch), true);
            // 输出 SHA 表示存在；空输出表示不存在（fatal 已被 execute 抑制）
            return out != null && !out.isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    // ========== CommitDialog 辅助方法 ==========

    /**
     * 读取 HEAD 中指定文件的内容。
     */
    public String readFileFromHead(String repoPath, String filePath) {
        try {
            String out = execute(repoPath, List.of("show", "HEAD:" + filePath), true);
            if (out == null) {
                return "";
            }
            String trimmed = out.trim();
            // 排除 git 错误消息（如无 HEAD 时）
            if (trimmed.startsWith("fatal:") || trimmed.contains("fatal:")) {
                return "";
            }
            return out;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 读取工作区文件内容。
     */
    private String readWorkingFile(String repoPath, String filePath) {
        try {
            Path file = Path.of(repoPath, filePath);
            if (!Files.exists(file) || !Files.isRegularFile(file)) {
                return null;
            }
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取 git config。
     */
    public String getConfig(String repoPath, String key) {
        // 用 `git config --get <key>` 而不是 `--local` + `--global` 回退：
        // 当 key 不在 local 时，--local 不抛异常（退出码非 0，输出空），
        // 旧实现捕获到 "" 误判 null 后直接返回，**永远不会回退到 global**。
        // `--get`（无 flag）让 git 自动按 local → global → system 优先级链查找，最自然。
        try {
            String out = execute(repoPath, List.of("config", "--get", key), true);
            return out == null || out.isBlank() ? null : out.trim();
        } catch (Exception e) {
            log.debug("读取 git config 失败：{} = {}", key, e.getMessage());
            return null;
        }
    }

    // ========== 变更操作（同步） ==========

    /**
     * 提交（同步，不带 callback，保留兼容性）。
     * <p>工作区干净时 git commit 返回 exit=1（"nothing to commit"），这不是真正的错误，
     * 而是表示"没有新变更可提交"。此时仍返回当前 HEAD SHA，让 UI 层走成功路径。</p>
     *
     * @param req 提交请求
     * @return 提交的完整 SHA
     */
    public String commit(CommitRequest req) throws GitGuiException {
        return commit(req, null);
    }

    /**
     * 提交（同步，带 callback，实时把 git 进程的输出推到调用方）。
     * <p>使用 git add 先暂存文件，然后 {@code git commit -F tmpfile} 执行提交。</p>
     * <p>特殊路径：</p>
     * <ul>
     *   <li>工作区干净时 git commit 返回 exit=1（"nothing to commit"），不视为错误，返回当前 HEAD SHA</li>
     *   <li>其它非零退出码以 {@link GitGuiException} 抛出</li>
     * </ul>
     *
     * @param req 提交请求（已精简，不再支持 amend/author/signCommit 等）
     * @param cb  进度回调（可空；非空时每行输出实时通过 {@code callback.onOutput(line)} 推送）
     * @return 提交的完整 SHA
     */
    public String commit(CommitRequest req, ProgressCallback cb) throws GitGuiException {
        if (req == null || req.getRepoPath() == null || req.getRepoPath()
                                                           .isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        final String repoPath = req.getRepoPath();

        // 步骤 1：暂存未提交文件（add 无 callback，工具步骤；allowNonZero=true 兼容 working tree clean）
        if (req.getStagedFiles() != null && !req.getStagedFiles()
                                                .isEmpty()) {
            List<String> addArgs = new ArrayList<>(List.of("add"));
            addArgs.addAll(req.getStagedFiles());
            GitProcessBuilder.executeQuietly(repoPath, addArgs, null, true);
        } else {
            GitProcessBuilder.executeQuietly(repoPath, List.of("add", "-A"), null, true);
        }

        // 步骤 2：执行 commit，把 git 进程输出实时推到 callback
        List<String> args = new ArrayList<>(List.of("commit"));
        boolean useTempFile = req.getMessage() != null && !req.getMessage()
                                                              .isBlank();
        try {
            if (useTempFile) {
                Path tmpFile = Files.createTempFile("git-commit-msg-", ".txt");
                try {
                    Files.writeString(tmpFile, req.getMessage(), StandardCharsets.UTF_8);
                    args.add("-F");
                    args.add(tmpFile.toAbsolutePath()
                                    .toString());
                    GitProcessBuilder.execute(repoPath, args, cb);
                } finally {
                    try {
                        Files.deleteIfExists(tmpFile);
                    } catch (Exception ignored) {
                    }
                }
            } else {
                args.add("--allow-empty-message");
                args.add("-m");
                args.add("");
                GitProcessBuilder.execute(repoPath, args, cb);
            }
        } catch (Exception e) {
            // 「nothing to commit, working tree clean」是 exit=1 但不算真正的失败
            if (e instanceof GitGuiException gge && gge.getMessage() != null && gge.getMessage()
                                                                                   .contains("nothing to commit")) {
                log.info("工作区干净，无新变更可提交：repoPath={}", repoPath);
                String fullSha = getBranchHeadSha(repoPath, "HEAD");
                return fullSha != null ? fullSha : "";
            }
            if (e instanceof GitGuiException gge) {
                throw gge;
            }
            throw new GitGuiException(ErrorCode.GIT_EXECUTION_FAILED, e.getMessage(), e);
        }

        // 步骤 3：获取完整 SHA（git commit 输出的是缩写 SHA）
        String fullSha = getBranchHeadSha(repoPath, "HEAD");
        log.info("提交成功：repoPath={}, sha={}", repoPath, fullSha);
        return fullSha != null ? fullSha : "";
    }

    /**
     * 初始化仓库。
     */
    public void init(String dir, boolean bare) {
        List<String> args = new ArrayList<>(List.of("init"));
        if (bare) {
            args.add("--bare");
        }
        args.add(dir);
        execute(null, args, false);
        log.info("仓库初始化成功：{}", dir);
    }

    // ========== 异步操作 ==========

    /**
     * 推送。
     * <p>自 v3 起 UI 选项简化（移除 Force / Tags / Push All / 上游跟踪 / 临时 URL 等），</p>
     * <p>本方法固定产生 {@code git push --progress <remote> <branch>}，不再有条件分支。</p>
     * <p>冗余字段（{@code forceWithLease/force/includeTags/pushAllBranches/...}）暂保留于
     * {@link PushRequest}，便于未来扩展 / 红线检查；这里不再读取它们。</p>
     */
    public void push(PushRequest req, ProgressCallback callback) {
        List<String> args = new ArrayList<>(List.of("push", "--progress"));
        if (req.getRemote() != null && !req.getRemote()
                                           .isBlank()) {
            args.add(req.getRemote());
        }
        if (req.getBranch() != null && !req.getBranch()
                                           .isBlank()) {
            args.add(req.getBranch());
        }
        executeAsync(req.getRepoPath(), args, callback);
        log.info("推送完成：repoPath={}", req.getRepoPath());
    }

    /**
     * 拉取。
     */
    public void pull(PullRequest req, ProgressCallback callback) {
        List<String> args = new ArrayList<>(List.of("pull", "--progress"));
        if (req.isRebaseInsteadOfMerge()) {
            args.add("--rebase");
        }
        if (req.isAutoStash()) {
            args.add("--autostash");
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
        if (req.getRemote() != null && !req.getRemote()
                                           .isBlank()) {
            args.add(req.getRemote());
        }
        if (req.getBranch() != null && !req.getBranch()
                                           .isBlank()) {
            args.add(req.getBranch());
        }
        executeAsync(req.getRepoPath(), args, callback);
        log.info("拉取完成：repoPath={}", req.getRepoPath());
    }

    /**
     * 获取。
     */
    public void fetch(String repoPath, String remote, String branch, boolean prune, ProgressCallback callback) {
        List<String> args = new ArrayList<>(List.of("fetch", "--progress"));
        if (prune) {
            args.add("--prune");
        }
        if (remote != null && !remote.isBlank()) {
            args.add(remote);
        }
        if (branch != null && !branch.isBlank()) {
            args.add(String.format("refs/heads/%s:refs/remotes/%s/%s", branch, remote, branch));
        }
        executeAsync(repoPath, args, callback);
        log.info("获取完成：repoPath={}", repoPath);
    }

    /**
     * 克隆仓库。
     */
    public String clone(CloneRequest req, ProgressCallback callback) {
        List<String> args = new ArrayList<>(List.of("clone", "--progress"));
        if (req.getBranch() != null && !req.getBranch()
                                           .isBlank()) {
            args.add("--branch");
            args.add(req.getBranch());
        }
        if (req.getDepth() > 0) {
            args.add("--depth");
            args.add(String.valueOf(req.getDepth()));
        }
        if (req.isBare()) {
            args.add("--bare");
        }
        args.add(req.getRemoteUrl());
        args.add(req.getTargetDir());
        executeAsync(null, args, callback);
        log.info("克隆成功：{}", req.getTargetDir());
        return req.getTargetDir();
    }

    // ========== Checkout 操作 ==========

    /**
     * 切换/创建分支。
     */
    public void checkout(String repoPath, String branch, boolean create, boolean force) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        if (branch == null || branch.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "分支名不能为空");
        }
        List<String> args = new ArrayList<>(List.of("checkout"));
        if (create) {
            args.add("-b");
        }
        if (force) {
            args.add("-f");
        }
        args.add(branch);
        execute(repoPath, args, false);
        log.info("切换分支成功：{} -> {}", repoPath, branch);
    }

    /**
     * 通过完整 ref 名切换。
     */
    public void checkoutRef(String repoPath, String refName, boolean force) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        if (refName == null || refName.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "refName 不能为空");
        }
        List<String> args = new ArrayList<>(List.of("checkout"));
        if (force) {
            args.add("-f");
        }
        args.add(refName);
        execute(repoPath, args, false);
        log.info("通过 ref 切换成功：{} -> {}", repoPath, refName);
    }

    /**
     * 切换到指定 commit（游离 HEAD）。
     */
    public void checkoutCommit(String repoPath, String commitId, boolean force) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        if (commitId == null || commitId.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "commit id 不能为空");
        }
        List<String> args = new ArrayList<>(List.of("checkout"));
        if (force) {
            args.add("-f");
        }
        args.add(commitId);
        execute(repoPath, args, false);
        log.info("切换到 commit 成功：{} -> {}", repoPath, commitId);
    }

    /**
     * 切换到指定 tag。
     */
    public void checkoutTag(String repoPath, String tag, boolean force) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        if (tag == null || tag.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "tag 名不能为空");
        }
        String tagName = tag.startsWith("refs/tags/") ? tag.substring("refs/tags/".length()) : tag;
        List<String> args = new ArrayList<>(List.of("checkout"));
        if (force) {
            args.add("-f");
        }
        args.add(String.format("tags/%s", tagName));
        execute(repoPath, args, false);
        log.info("切换到 tag 成功：{} -> {}", repoPath, tagName);
    }

    /**
     * 从指定 commit 创建新分支并切换。
     */
    public void checkoutNewBranchFromCommit(String repoPath, String branch, String commitId, boolean forceCheckout, boolean overrideExisting) {
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        if (branch == null || branch.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "分支名不能为空");
        }
        if (commitId == null || commitId.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "commit id 不能为空");
        }
        // git checkout -b/-B branch commit
        List<String> args = new ArrayList<>(List.of("checkout"));
        args.add(overrideExisting ? "-B" : "-b");
        args.add(branch);
        if (forceCheckout) {
            args.add("-f");
        }
        args.add(commitId);
        execute(repoPath, args, false);
        log.info("从 commit 创建新分支成功：repo={}, branch={}, commit={}", repoPath, branch, commitId);
    }

    /**
     * 创建本地分支并设置跟踪远程上游（git checkout -b branch --track origin/branch）。
     */
    public void checkoutWithTrack(String repoPath, String branchName, boolean force) {
        List<String> args = new ArrayList<>(List.of("checkout", "-b", branchName, "--track", String.format("origin/%s", branchName)));
        if (force) {
            args.add("-f");
        }
        execute(repoPath, args, false);
    }

    /**
     * 处理远程分支 checkout（refs/remotes/&lt;remote&gt;/&lt;branch&gt;）。创建本地分支并设置跟踪。
     */
    public void checkoutRemoteRef(String repoPath, String refName, boolean force) {
        String prefix = "refs/remotes/";
        if (!refName.startsWith(prefix)) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "非法的远程 ref 路径：" + refName);
        }
        String rest = refName.substring(prefix.length());
        int slash = rest.indexOf('/');
        if (slash <= 0) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "远程 ref 缺少分支段：" + refName);
        }
        String remote = rest.substring(0, slash);
        String branchPath = rest.substring(slash + 1);

        // git checkout -b branchPath --track remote/branchPath
        List<String> args = new ArrayList<>(List.of("checkout", "-b", branchPath, "--track", String.format("%s/%s", remote, branchPath)));
        if (force) {
            args.add("-f");
        }
        execute(repoPath, args, false);
        log.info("远程分支 checkout 完成：repo={}, ref={} → local={}/{}", repoPath, refName, remote, branchPath);
    }

    // ========== 原 CliGitExecutor 方法（保留） ==========

    /**
     * 通过 CLI 执行子模块更新。
     */
    public void submoduleUpdate(String repoPath, boolean recursive, ProgressCallback callback) {
        List<String> args = new ArrayList<>(List.of("submodule", "update"));
        if (recursive) {
            args.add("--recursive");
        }
        executeAsync(repoPath, args, callback);
    }

    /**
     * 通过 CLI 执行 LFS 安装。
     */
    public void lfsInstall(String repoPath) {
        try {
            execute(repoPath, List.of("lfs", "install"), true);
        } catch (GitGuiException e) {
            log.warn("LFS 安装失败（可能未安装 git-lfs）：{}", e.getMessage());
        }
    }

    /**
     * 通过 CLI 执行 git gc。
     */
    public void gc(String repoPath, ProgressCallback callback) {
        executeAsync(repoPath, List.of("gc", "--prune=now"), callback);
        log.info("GC 完成：repoPath={}", repoPath);
    }
}
