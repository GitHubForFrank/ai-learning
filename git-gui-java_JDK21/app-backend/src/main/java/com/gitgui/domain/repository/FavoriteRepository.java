package com.gitgui.domain.repository;

import com.gitgui.domain.model.Favorite;

import java.util.List;

/**
 * 收藏仓储接口
 * <p>领域层定义的持久化契约，由基础设施层 {@code infrastructure/persistence/repository/} 实现。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface FavoriteRepository {

    /**
     * 保存收藏（新增或更新）。
     *
     * @param favorite 收藏对象
     */
    void save(Favorite favorite);

    /**
     * 根据 ID 查找。
     *
     * @param id 主键
     * @return 收藏对象，不存在返回 null
     */
    Favorite findById(String id);

    /**
     * 根据仓库路径查找（用于唯一性校验，BR-03）。
     *
     * @param repoPath 仓库路径
     * @return 收藏对象，不存在返回 null
     */
    Favorite findByRepoPath(String repoPath);

    /**
     * 列出全部收藏（按 pinned 降序、sortOrder 升序）。
     *
     * @return 收藏列表
     */
    List<Favorite> findAll();

    /**
     * 按分组筛选。
     *
     * @param group 分组名
     * @return 收藏列表
     */
    List<Favorite> findByGroup(String group);

    /**
     * 根据 ID 删除。
     *
     * @param id 主键
     */
    void deleteById(String id);

    /**
     * 判断仓库路径是否已收藏（BR-03 唯一性）。
     *
     * @param repoPath 仓库路径
     * @return true 表示已存在
     */
    boolean existsByRepoPath(String repoPath);
}
