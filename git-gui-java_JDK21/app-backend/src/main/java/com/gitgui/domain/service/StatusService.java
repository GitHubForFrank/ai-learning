package com.gitgui.domain.service;

import com.gitgui.domain.model.DiffResult;
import com.gitgui.domain.model.FileChange;
import com.gitgui.domain.model.FileStatus;
import com.gitgui.domain.model.LogEntry;
import com.gitgui.domain.model.RefInfo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 状态查询服务接口
 * <p>关联 BR：BR-18、BR-24。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface StatusService {

    /**
     * 获取工作区文件状态列表（PRD 4.3）。
     *
     * @param repoPath      仓库路径
     * @param showUntracked 是否显示未跟踪文件
     * @param showIgnored   是否显示已忽略文件
     * @return 文件状态列表
     */
    List<FileStatus> getStatus(String repoPath, boolean showUntracked, boolean showIgnored);

    /**
     * 获取提交日志（BR-18 分页加载）。
     *
     * @param repoPath 仓库路径
     * @param page     页码（从 1 开始）
     * @param pageSize 单页条数
     * @return 日志条目列表
     */
    List<LogEntry> getLog(String repoPath, int page, int pageSize);

    /**
     * 获取文件 Diff（PRD 4.10）。
     *
     * @param repoPath 仓库路径
     * @param path     文件路径
     * @param oldRev   旧版本
     * @param newRev   新版本
     * @return Diff 结果
     */
    DiffResult getDiff(String repoPath, String path, String oldRev, String newRev);

    /**
     * 列出仓库所有分支（本地 + 远程），用于切换分支对话框（PRD 4.6.1）。
     *
     * @param repoPath 仓库路径
     * @return 分支名列表（含 refs/heads/* 与 refs/remotes/* 前缀，由调用方按需处理）
     */
    List<String> listBranches(String repoPath);

    /**
     * 获取当前分支名。
     *
     * @param repoPath 仓库路径
     * @return 当前分支名（已剥离 refs/heads/ 前缀），游离 HEAD 返回 "DETACHED"，异常返回 "UNKNOWN"
     */
    String getCurrentBranch(String repoPath);

    /**
     * 列出仓库所有 Tag（含轻量标签与附注标签），用于 TortoiseGit 风格切换对话框。
     *
     * @param repoPath 仓库路径
     * @return tag 名列表（refs/tags/* 前缀或纯名称，由调用方按需处理）
     */
    List<String> listTags(String repoPath);

    /**
     * 列出最近的提交（用于切换对话框「Commit」下拉框）。
     *
     * @param repoPath 仓库路径
     * @param limit    上限条数（如 200）
     * @return 提交条目列表（倒序）
     */
    List<LogEntry> listRecentCommits(String repoPath, int limit);

    /**
     * 获取分支 HEAD 指向的提交 SHA-1（40 字符十六进制）。
     *
     * @param repoPath 仓库路径
     * @param branch   分支名（本地或远程完整名）
     * @return SHA-1，未找到返回 null
     */
    String getBranchHeadSha(String repoPath, String branch);

    /**
     * 批量查询所有 ref（含本地/远程分支与 tag），并解析每个 ref 指向 commit 的元信息。
     * <p>用于 TortoiseGit 风格 SwitchDialog / BrowseReferencesDialog，避免每个 ref 单独打开仓库。</p>
     *
     * @param repoPath 仓库路径
     * @return ref 信息列表（含 ref 名 / 显示名 / 类型 / SHA / 作者 / 时间 / message）
     */
    List<RefInfo> batchListRefs(String repoPath);

    /**
     * 列出 commit 日志条目（带过滤条件），用于 LogMessagesDialog。
     *
     * @param repoPath 仓库路径
     * @param fromDate 起始日期（含），可空
     * @param toDate   截止日期（含），可空
     * @param author   作者名过滤（substring 匹配），可空
     * @param message  message 关键词过滤（substring 匹配），可空
     * @param limit    上限条数
     * @return commit 日志条目列表
     */
    List<LogEntry> listLogEntries(String repoPath, LocalDateTime fromDate, LocalDateTime toDate, String author, String message, int limit);

    /**
     * 获取 commit 引发的文件变更列表。
     *
     * @param repoPath 仓库路径
     * @param commitId 提交哈希（完整或短哈希均可）
     * @return 文件变更列表
     */
    List<FileChange> getCommitChanges(String repoPath, String commitId);
}
