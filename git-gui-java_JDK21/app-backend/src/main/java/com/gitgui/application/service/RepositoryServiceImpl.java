package com.gitgui.application.service;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.async.TaskHandle;
import com.gitgui.core.constant.OperationType;
import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.core.util.PathUtil;
import com.gitgui.domain.constant.TaskType;
import com.gitgui.domain.model.RepositoryMeta;
import com.gitgui.domain.model.request.CloneRequest;
import com.gitgui.domain.repository.RepositoryMetaRepository;
import com.gitgui.domain.service.AsyncTaskService;
import com.gitgui.domain.service.RepositoryService;
import com.gitgui.domain.service.RecentRepoService;
import com.gitgui.infrastructure.jgit.JGitOperationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.google.inject.Inject;

/**
 * 仓库服务实现
 * <p>关联 BR：BR-01、BR-02、BR-05、BR-41。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class RepositoryServiceImpl implements RepositoryService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);

    private final JGitOperationExecutor jgitExecutor;
    private final RepositoryMetaRepository metaRepository;
    private final AsyncTaskService asyncTaskService;
    private final RecentRepoService recentRepoService;

    /** 多仓库检索结果缓存（线程安全） */
    private final List<RepositoryMeta> scanResults = new CopyOnWriteArrayList<>();

    @Inject
    public RepositoryServiceImpl(JGitOperationExecutor jgitExecutor,
                                 RepositoryMetaRepository metaRepository,
                                 AsyncTaskService asyncTaskService,
                                 RecentRepoService recentRepoService) {
        this.jgitExecutor = jgitExecutor;
        this.metaRepository = metaRepository;
        this.asyncTaskService = asyncTaskService;
        this.recentRepoService = recentRepoService;
    }

    @Override
    public RepositoryMeta openRepository(String repoPath) {
        // BR-41：非 Git 目录抛 REPO_NOT_GIT
        try {
            PathUtil.validateGitRepoPath(repoPath);
        } catch (Exception e) {
            throw new GitGuiException(ErrorCode.REPO_NOT_GIT, "选定目录不是 Git 仓库：" + repoPath);
        }
        RepositoryMeta meta = refreshMeta(repoPath);
        // BR-05：记录最近打开
        recentRepoService.recordOpen(repoPath, meta.getCurrentBranch());
        log.info("打开仓库：{}", repoPath);
        return meta;
    }

    @Override
    public TaskHandle clone(CloneRequest req, ProgressCallback cb) {
        if (req.getRemoteUrl() == null || req.getRemoteUrl().isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请输入远程仓库 URL");
        }
        if (req.getTargetDir() == null || req.getTargetDir().isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请输入本地目标目录");
        }
        return asyncTaskService.submitWrite(req.getTargetDir(), TaskType.CLONE, () -> {
            String clonedPath = jgitExecutor.clone(req, cb);
            // 克隆成功后记录最近仓库
            recentRepoService.recordOpen(clonedPath, "");
        }, cb);
    }

    @Override
    public void initRepository(String dir, boolean bare) {
        if (dir == null || dir.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "请输入目标目录");
        }
        jgitExecutor.init(dir, bare);
    }

    @Override
    public TaskHandle scanMultiRepo(String rootDir, int depth, ProgressCallback cb) {
        // BR-01：默认深度 3，可配置 1~10
        if (depth < 1 || depth > 10) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "检索深度必须在 1~10 之间（BR-01）");
        }
        scanResults.clear();
        return asyncTaskService.submitRead(rootDir, TaskType.MULTI_REPO_SCAN, () -> {
            // BR-02：异步可取消
            scanDirectory(new File(rootDir), depth, 1, cb);
        }, cb);
    }

    @Override
    public RepositoryMeta refreshMeta(String repoPath) {
        String branch = jgitExecutor.getCurrentBranch(repoPath);
        boolean clean = jgitExecutor.isClean(repoPath);
        List<com.gitgui.domain.model.RemoteConfig> remotes = jgitExecutor.listRemotes(repoPath);
        String remoteUrl = remotes.isEmpty() ? "" : remotes.get(0).getFetchUrl();
        RepositoryMeta meta = RepositoryMeta.builder()
                .repoPath(repoPath)
                .currentBranch(branch)
                .headCommit("")
                .remoteUrl(remoteUrl)
                .hasUncommittedChanges(!clean)
                .build();
        metaRepository.save(meta);
        return meta;
    }

    @Override
    public List<RepositoryMeta> getScanResults() {
        return new ArrayList<>(scanResults);
    }

    /**
     * 递归扫描目录查找 Git 仓库（BR-01/BR-02）。
     *
     * @param dir       当前目录
     * @param maxDepth  最大深度
     * @param current   当前深度
     * @param cb        进度回调（含取消检查）
     */
    private void scanDirectory(File dir, int maxDepth, int current, ProgressCallback cb) {
        if (cb != null && cb.isCancelled()) {
            return;
        }
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        // BR-01：只读 .git 目录存在性，不修改任何仓库状态
        File gitDir = new File(dir, ".git");
        if (gitDir.exists()) {
            try {
                RepositoryMeta meta = RepositoryMeta.builder()
                        .repoPath(dir.getAbsolutePath())
                        .currentBranch(jgitExecutor.getCurrentBranch(dir.getAbsolutePath()))
                        .hasUncommittedChanges(!jgitExecutor.isClean(dir.getAbsolutePath()))
                        .build();
                scanResults.add(meta);
                if (cb != null) {
                    cb.onOutput("发现仓库：" + dir.getAbsolutePath());
                }
            } catch (Exception e) {
                log.warn("扫描仓库失败：{}", dir.getAbsolutePath(), e);
            }
            // Git 仓库内部不再递归
            return;
        }
        if (current >= maxDepth) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                // 跳过隐藏目录与 node_modules 等
                String name = child.getName();
                if (name.startsWith(".") || "node_modules".equals(name) || "target".equals(name)) {
                    continue;
                }
                scanDirectory(child, maxDepth, current + 1, cb);
            }
        }
    }
}
