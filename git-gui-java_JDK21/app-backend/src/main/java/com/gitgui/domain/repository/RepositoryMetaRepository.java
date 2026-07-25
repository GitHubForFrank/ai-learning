package com.gitgui.domain.repository;

import com.gitgui.domain.model.RepositoryMeta;

import java.util.List;

/**
 * 仓库元信息缓存仓储接口
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface RepositoryMetaRepository {

    /**
     * 保存元信息（upsert）。
     *
     * @param meta 元信息
     */
    void save(RepositoryMeta meta);

    /**
     * 根据仓库路径查找。
     *
     * @param repoPath 仓库路径
     * @return 元信息，不存在返回 null
     */
    RepositoryMeta findByRepoPath(String repoPath);

    /**
     * 列出全部元信息。
     *
     * @return 元信息列表
     */
    List<RepositoryMeta> findAll();

    /**
     * 根据仓库路径删除。
     *
     * @param repoPath 仓库路径
     */
    void deleteByRepoPath(String repoPath);
}
