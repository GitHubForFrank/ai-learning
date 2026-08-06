package com.gitgui.application.service;

import com.gitgui.core.config.AppConfig;
import com.gitgui.core.exception.ErrorCode;
import com.gitgui.core.exception.GitGuiException;
import com.gitgui.core.util.IdUtil;
import com.gitgui.domain.model.Favorite;
import com.gitgui.domain.repository.FavoriteRepository;
import com.gitgui.domain.service.FavoriteService;
import com.google.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 收藏服务实现
 * <p>关联 BR：BR-03（唯一性）、BR-04（置顶与排序）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class FavoriteServiceImpl implements FavoriteService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteServiceImpl.class);

    private final FavoriteRepository favoriteRepository;

    @Inject
    public FavoriteServiceImpl(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    @Override
    public Favorite add(String repoPath, String alias, String group) {
        // BR-03：参数校验
        if (repoPath == null || repoPath.isBlank()) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "仓库路径不能为空");
        }
        if (alias != null && alias.length() > AppConfig.ALIAS_MAX_LENGTH) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "别名最大长度 " + AppConfig.ALIAS_MAX_LENGTH + " 字符（BR-03）");
        }
        if (group != null && group.length() > AppConfig.GROUP_MAX_LENGTH) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "分组最大长度 " + AppConfig.GROUP_MAX_LENGTH + " 字符（BR-03）");
        }
        // BR-03：唯一性校验
        if (favoriteRepository.existsByRepoPath(repoPath)) {
            throw new GitGuiException(ErrorCode.DUPLICATE_FAVORITE, "仓库已收藏：" + repoPath);
        }
        Favorite favorite = Favorite.builder()
                                    .id(IdUtil.newId())
                                    .repoPath(repoPath)
                                    .alias(alias == null ? "" : alias)
                                    .group(group == null ? "" : group)
                                    .pinned(false)
                                    .sortOrder(0)
                                    .remoteUrl("")
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build();
        favoriteRepository.save(favorite);
        log.info("收藏新增：repoPath={}", repoPath);
        return favorite;
    }

    @Override
    public void remove(String id) {
        Favorite existing = favoriteRepository.findById(id);
        if (existing == null) {
            throw new GitGuiException(ErrorCode.NOT_FOUND, "收藏不存在：" + id);
        }
        favoriteRepository.deleteById(id);
        log.info("收藏删除：id={}", id);
    }

    @Override
    public void togglePinned(String id, boolean pinned) {
        // BR-04：置顶切换
        Favorite existing = favoriteRepository.findById(id);
        if (existing == null) {
            throw new GitGuiException(ErrorCode.NOT_FOUND, "收藏不存在：" + id);
        }
        existing.setPinned(pinned);
        favoriteRepository.save(existing);
        log.info("收藏置顶切换：id={}, pinned={}", id, pinned);
    }

    @Override
    public void update(String id, String alias, String group) {
        Favorite existing = favoriteRepository.findById(id);
        if (existing == null) {
            throw new GitGuiException(ErrorCode.NOT_FOUND, "收藏不存在：" + id);
        }
        if (alias != null && alias.length() > AppConfig.ALIAS_MAX_LENGTH) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "别名长度超限（BR-03）");
        }
        if (group != null && group.length() > AppConfig.GROUP_MAX_LENGTH) {
            throw new GitGuiException(ErrorCode.VALIDATION_FAILED, "分组长度超限（BR-03）");
        }
        if (alias != null) {
            existing.setAlias(alias);
        }
        if (group != null) {
            existing.setGroup(group);
        }
        existing.setUpdatedAt(LocalDateTime.now());
        favoriteRepository.save(existing);
    }

    @Override
    public List<Favorite> list() {
        return favoriteRepository.findAll();
    }

    @Override
    public List<Favorite> listByGroup(String group) {
        return favoriteRepository.findByGroup(group);
    }
}
