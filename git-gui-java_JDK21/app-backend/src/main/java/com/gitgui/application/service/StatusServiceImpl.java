package com.gitgui.application.service;

import com.gitgui.domain.model.DiffResult;
import com.gitgui.domain.model.FileChange;
import com.gitgui.domain.model.FileStatus;
import com.gitgui.domain.model.LogEntry;
import com.gitgui.domain.model.RefInfo;
import com.gitgui.domain.service.StatusService;
import com.gitgui.infrastructure.cli.CliGitExecutor;
import com.google.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 状态查询服务实现
 * <p>关联 BR：BR-18、BR-24。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class StatusServiceImpl implements StatusService {

    private static final Logger log = LoggerFactory.getLogger(StatusServiceImpl.class);

    private final CliGitExecutor gitExecutor;

    @Inject
    public StatusServiceImpl(CliGitExecutor gitExecutor) {
        this.gitExecutor = gitExecutor;
    }

    @Override
    public List<FileStatus> getStatus(String repoPath, boolean showUntracked, boolean showIgnored) {
        return gitExecutor.getStatus(repoPath, showUntracked);
    }

    @Override
    public List<LogEntry> getLog(String repoPath, int page, int pageSize) {
        if (pageSize <= 0) {
            pageSize = 200;
        }
        return gitExecutor.getLog(repoPath, Math.max(1, page), pageSize);
    }

    @Override
    public DiffResult getDiff(String repoPath, String path, String oldRev, String newRev) {
        return gitExecutor.getDiff(repoPath, path, oldRev, newRev);
    }

    @Override
    public List<String> listBranches(String repoPath) {
        return gitExecutor.listBranches(repoPath);
    }

    @Override
    public String getCurrentBranch(String repoPath) {
        return gitExecutor.getCurrentBranch(repoPath);
    }

    @Override
    public List<String> listTags(String repoPath) {
        return gitExecutor.listTags(repoPath);
    }

    @Override
    public List<LogEntry> listRecentCommits(String repoPath, int limit) {
        return gitExecutor.getLog(repoPath, 1, Math.max(1, limit));
    }

    @Override
    public String getBranchHeadSha(String repoPath, String branch) {
        return gitExecutor.getBranchHeadSha(repoPath, branch);
    }

    @Override
    public List<RefInfo> batchListRefs(String repoPath) {
        return gitExecutor.batchListRefs(repoPath);
    }

    @Override
    public List<LogEntry> listLogEntries(String repoPath, LocalDateTime fromDate, LocalDateTime toDate, String author, String message, int limit) {
        List<LogEntry> raw = gitExecutor.getLog(repoPath, 1, Math.max(1, limit * 4));
        String lowerAuthor = author == null ? "" : author.trim()
                                                         .toLowerCase(Locale.ROOT);
        String lowerMsg = message == null ? "" : message.trim()
                                                        .toLowerCase(Locale.ROOT);
        return raw.stream()
                  .filter(le -> fromDate == null || (le.getCommitTime() != null && !le.getCommitTime()
                                                                                      .isBefore(fromDate)))
                  .filter(le -> toDate == null || (le.getCommitTime() != null && !le.getCommitTime()
                                                                                    .isAfter(toDate)))
                  .filter(le -> lowerAuthor.isEmpty() || (le.getAuthor() != null && le.getAuthor()
                                                                                      .toLowerCase(Locale.ROOT)
                                                                                      .contains(lowerAuthor)))
                  .filter(le -> lowerMsg.isEmpty() || (le.getMessage() != null && le.getMessage()
                                                                                    .toLowerCase(Locale.ROOT)
                                                                                    .contains(lowerMsg)))
                  .limit(Math.max(1, limit))
                  .toList();
    }

    @Override
    public List<FileChange> getCommitChanges(String repoPath, String commitId) {
        return gitExecutor.getCommitChanges(repoPath, commitId);
    }
}
