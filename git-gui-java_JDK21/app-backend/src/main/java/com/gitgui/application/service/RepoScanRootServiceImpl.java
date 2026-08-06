package com.gitgui.application.service;

import com.gitgui.core.async.ProgressCallback;
import com.gitgui.core.config.AppConfig;
import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.core.util.IdUtil;
import com.gitgui.domain.model.RepoScanRoot;
import com.gitgui.domain.repository.AppSettingsRepository;
import com.gitgui.domain.repository.RepoScanRootRepository;
import com.gitgui.domain.service.RepoScanRootService;
import com.gitgui.domain.service.RepositoryService;
import com.google.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 扫描根目录服务实现
 * <p>关联 BR：BR-01（多仓库检索）、BR-02（异步可取消）。</p>
 *
 * @author FrankKang
 * @since 2026-07-24
 */
public class RepoScanRootServiceImpl implements RepoScanRootService {

    private static final Logger log = LoggerFactory.getLogger(RepoScanRootServiceImpl.class);

    private final RepoScanRootRepository repoScanRootRepository;
    private final RepositoryService repositoryService;
    private final AppSettingsRepository settingsRepository;

    @Inject
    public RepoScanRootServiceImpl(RepoScanRootRepository repoScanRootRepository, RepositoryService repositoryService,
            AppSettingsRepository settingsRepository) {
        this.repoScanRootRepository = repoScanRootRepository;
        this.repositoryService = repositoryService;
        this.settingsRepository = settingsRepository;
    }

    @Override
    public RepoScanRoot add(String rootPath, String alias, int scanDepth) {
        if (rootPath == null || rootPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "根目录路径不能为空");
        }
        if (scanDepth < 1 || scanDepth > 10) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "扫描深度必须在 1~10 之间（BR-01）");
        }
        if (alias != null && alias.length() > AppConfig.ALIAS_MAX_LENGTH) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "别名长度超过 " + AppConfig.ALIAS_MAX_LENGTH + " 字符");
        }
        RepoScanRoot existing = repoScanRootRepository.findByRootPath(rootPath);
        // 防御性 null 处理：existing 可能为 null 时构造全新对象
        RepoScanRoot root;
        if (existing == null) {
            root = RepoScanRoot.builder()
                               .id(IdUtil.newId())
                               .rootPath(rootPath)
                               .alias(alias == null ? "" : alias)
                               .scanDepth(scanDepth)
                               .lastScannedAt(LocalDateTime.now())
                               .enabled(true)
                               .sortOrder(0)
                               .createdAt(LocalDateTime.now())
                               .build();
        } else {
            root = existing;
            root.setAlias(alias == null ? "" : alias);
            root.setScanDepth(scanDepth);
            root.setLastScannedAt(LocalDateTime.now());
            root.setEnabled(true);
        }
        repoScanRootRepository.save(root);
        // 触发异步扫描
        triggerScan(rootPath, scanDepth);
        log.info("新增/更新扫描根目录：rootPath={}, scanDepth={}", rootPath, scanDepth);
        return root;
    }

    @Override
    public boolean rescan(String rootPath) {
        RepoScanRoot existing = repoScanRootRepository.findByRootPath(rootPath);
        if (existing == null) {
            return false;
        }
        existing.setLastScannedAt(LocalDateTime.now());
        repoScanRootRepository.save(existing);
        triggerScan(rootPath, existing.getScanDepth());
        log.info("重新扫描：rootPath={}, scanDepth={}", rootPath, existing.getScanDepth());
        return true;
    }

    @Override
    public void rescanAll() {
        List<RepoScanRoot> roots = repoScanRootRepository.findEnabled();
        if (roots.isEmpty()) {
            return;
        }
        for (RepoScanRoot root : roots) {
            root.setLastScannedAt(LocalDateTime.now());
            repoScanRootRepository.save(root);
            triggerScan(root.getRootPath(), root.getScanDepth());
        }
        log.info("恢复扫描全部已启用根目录：count={}", roots.size());
    }

    @Override
    public void remove(String id) {
        RepoScanRoot existing = repoScanRootRepository.findById(id);
        if (existing == null) {
            throw new GitGuiException(ErrorCode.NOT_FOUND, "扫描根目录不存在：" + id);
        }
        repoScanRootRepository.deleteById(id);
        log.info("删除扫描根目录：id={}, rootPath={}", id, existing.getRootPath());
    }

    @Override
    public void removeByPath(String rootPath) {
        repoScanRootRepository.deleteByRootPath(rootPath);
        log.info("按路径删除扫描根目录：rootPath={}", rootPath);
    }

    @Override
    public void toggleEnabled(String id, boolean enabled) {
        RepoScanRoot existing = repoScanRootRepository.findById(id);
        if (existing == null) {
            throw new GitGuiException(ErrorCode.NOT_FOUND, "扫描根目录不存在：" + id);
        }
        existing.setEnabled(enabled);
        repoScanRootRepository.save(existing);
        log.info("扫描根目录启用切换：id={}, enabled={}", id, enabled);
    }

    @Override
    public void updateMeta(String id, String alias, int sortOrder) {
        RepoScanRoot existing = repoScanRootRepository.findById(id);
        if (existing == null) {
            throw new GitGuiException(ErrorCode.NOT_FOUND, "扫描根目录不存在：" + id);
        }
        if (alias != null && alias.length() > AppConfig.ALIAS_MAX_LENGTH) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "别名长度超限");
        }
        if (alias != null) {
            existing.setAlias(alias);
        }
        existing.setSortOrder(sortOrder);
        repoScanRootRepository.save(existing);
    }

    @Override
    public List<RepoScanRoot> listEnabled() {
        return repoScanRootRepository.findEnabled();
    }

    @Override
    public List<RepoScanRoot> listAll() {
        return repoScanRootRepository.findAll();
    }

    /**
     * 触发异步扫描（无 UI 进度回调，由 UI 层单独订阅刷新）。
     */
    private void triggerScan(String rootPath, int scanDepth) {
        try {
            repositoryService.scanMultiRepo(rootPath, scanDepth, ProgressCallback.NOOP);
        } catch (Exception e) {
            log.warn("触发扫描失败：rootPath={}", rootPath, e);
        }
    }
}