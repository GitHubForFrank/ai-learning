package com.gitgui.application.service;

import com.gitgui.core.config.AppConfig;
import com.gitgui.core.util.IdUtil;
import com.gitgui.domain.model.RecentRepo;
import com.gitgui.domain.repository.AppSettingsRepository;
import com.gitgui.domain.repository.RecentRepoRepository;
import com.gitgui.domain.service.RecentRepoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import com.google.inject.Inject;

/**
 * 最近仓库服务实现
 * <p>关联 BR：BR-05（倒序与淘汰）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class RecentRepoServiceImpl implements RecentRepoService {

    private static final Logger log = LoggerFactory.getLogger(RecentRepoServiceImpl.class);

    private final RecentRepoRepository recentRepoRepository;
    private final AppSettingsRepository settingsRepository;

    @Inject
    public RecentRepoServiceImpl(RecentRepoRepository recentRepoRepository,
                                 AppSettingsRepository settingsRepository) {
        this.recentRepoRepository = recentRepoRepository;
        this.settingsRepository = settingsRepository;
    }

    @Override
    public void recordOpen(String repoPath, String lastBranch) {
        // BR-05：打开仓库时 upsert（已存在则更新 lastOpenedAt/openCount）
        RecentRepo existing = recentRepoRepository.findByRepoPath(repoPath);
        if (existing == null) {
            RecentRepo recent = RecentRepo.builder()
                    .id(IdUtil.newId())
                    .repoPath(repoPath)
                    .lastBranch(lastBranch == null ? "" : lastBranch)
                    .lastOpenedAt(LocalDateTime.now())
                    .openCount(1)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            recentRepoRepository.save(recent);
        } else {
            existing.setLastBranch(lastBranch == null ? "" : lastBranch);
            existing.setLastOpenedAt(LocalDateTime.now());
            existing.setOpenCount(existing.getOpenCount() + 1);
            existing.setUpdatedAt(LocalDateTime.now());
            recentRepoRepository.save(existing);
        }
        // BR-05：超限淘汰最旧
        int maxKeep = getMaxKeep();
        recentRepoRepository.deleteOldest(maxKeep);
        log.info("最近仓库记录：repoPath={}, maxKeep={}", repoPath, maxKeep);
    }

    @Override
    public List<RecentRepo> list() {
        // BR-05：按 lastOpenedAt 倒序
        return recentRepoRepository.findAll();
    }

    @Override
    public void remove(String id) {
        recentRepoRepository.deleteById(id);
    }

    @Override
    public void clear() {
        recentRepoRepository.deleteAll();
    }

    /**
     * 获取最近仓库最大保留数（默认 20，可配置，BR-05）。
     */
    private int getMaxKeep() {
        String value = settingsRepository.getValue("recent_repo.max_keep");
        if (value == null || value.isBlank()) {
            return AppConfig.DEFAULT_RECENT_MAX_KEEP;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return AppConfig.DEFAULT_RECENT_MAX_KEEP;
        }
    }
}
