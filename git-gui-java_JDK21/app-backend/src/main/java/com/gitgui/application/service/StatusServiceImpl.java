package com.gitgui.application.service;

import com.gitgui.domain.model.DiffResult;
import com.gitgui.domain.model.FileChange;
import com.gitgui.domain.model.FileStatus;
import com.gitgui.domain.model.LogEntry;
import com.gitgui.domain.model.RefInfo;
import com.gitgui.domain.service.StatusService;
import com.gitgui.infrastructure.jgit.JGitOperationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import com.google.inject.Inject;

/**
 * 状态查询服务实现
 * <p>关联 BR：BR-18、BR-24。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class StatusServiceImpl implements StatusService {

    private static final Logger log = LoggerFactory.getLogger(StatusServiceImpl.class);

    private final JGitOperationExecutor jgitExecutor;

    @Inject
    public StatusServiceImpl(JGitOperationExecutor jgitExecutor) {
        this.jgitExecutor = jgitExecutor;
    }

    @Override
    public List<FileStatus> getStatus(String repoPath, boolean showUntracked, boolean showIgnored) {
        // PRD 4.3：展示已修改、新增、删除、未跟踪、冲突文件
        return jgitExecutor.getStatus(repoPath, showUntracked);
    }

    @Override
    public List<LogEntry> getLog(String repoPath, int page, int pageSize) {
        // BR-18：默认按提交时间倒序，分页加载（单页默认 200 条）
        if (pageSize <= 0) {
            pageSize = 200;
        }
        return jgitExecutor.getLog(repoPath, Math.max(1, page), pageSize);
    }

    @Override
    public DiffResult getDiff(String repoPath, String path, String oldRev, String newRev) {
        return jgitExecutor.getDiff(repoPath, path, oldRev, newRev);
    }

    @Override
    public List<String> listBranches(String repoPath) {
        // 直接代理到 JGit 适配器
        return jgitExecutor.listBranches(repoPath);
    }

    @Override
    public String getCurrentBranch(String repoPath) {
        // 适配器已处理异常（返回 UNKNOWN / DETACHED），UI 层仅做展示
        return jgitExecutor.getCurrentBranch(repoPath);
    }

    @Override
    public List<String> listTags(String repoPath) {
        return jgitExecutor.listTags(repoPath);
    }

    @Override
    public List<LogEntry> listRecentCommits(String repoPath, int limit) {
        // 第 1 页 + limit 即可覆盖「最近 N 条」
        return jgitExecutor.getLog(repoPath, 1, Math.max(1, limit));
    }

    @Override
    public String getBranchHeadSha(String repoPath, String branch) {
        return jgitExecutor.getBranchHeadSha(repoPath, branch);
    }

    @Override
    public List<RefInfo> batchListRefs(String repoPath) {
        return jgitExecutor.batchListRefs(repoPath);
    }

    @Override
    public List<LogEntry> listLogEntries(String repoPath, LocalDateTime fromDate, LocalDateTime toDate,
                                         String author, String message, int limit) {
        // 复用 getLog 第一页 + 上限条数，再在内存里应用过滤条件
        List<LogEntry> raw = jgitExecutor.getLog(repoPath, 1, Math.max(1, limit * 4));
        String lowerAuthor = author == null ? "" : author.trim().toLowerCase(Locale.ROOT);
        String lowerMsg = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        return raw.stream()
                .filter(le -> fromDate == null || (le.getCommitTime() != null && !le.getCommitTime().isBefore(fromDate)))
                .filter(le -> toDate == null || (le.getCommitTime() != null && !le.getCommitTime().isAfter(toDate)))
                .filter(le -> lowerAuthor.isEmpty() || (le.getAuthor() != null && le.getAuthor().toLowerCase(Locale.ROOT).contains(lowerAuthor)))
                .filter(le -> lowerMsg.isEmpty() || (le.getMessage() != null && le.getMessage().toLowerCase(Locale.ROOT).contains(lowerMsg)))
                .limit(Math.max(1, limit))
                .toList();
    }

    @Override
    public List<FileChange> getCommitChanges(String repoPath, String commitId) {
        return jgitExecutor.getCommitChanges(repoPath, commitId);
    }
}
