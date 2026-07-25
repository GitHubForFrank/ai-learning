package com.gitgui.domain.repository;

import com.gitgui.domain.model.RecentRepo;

import java.util.List;

/**
 * 最近仓库仓储接口
 * <p>遵循 BR-05（倒序与淘汰）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface RecentRepoRepository {

    /**
     * 保存最近仓库记录（upsert）。
     *
     * @param recentRepo 记录
     */
    void save(RecentRepo recentRepo);

    /**
     * 列出全部最近仓库（按 lastOpenedAt 倒序）。
     *
     * @return 最近仓库列表
     */
    List<RecentRepo> findAll();

    /**
     * 根据仓库路径查找。
     *
     * @param repoPath 仓库路径
     * @return 记录，不存在返回 null
     */
    RecentRepo findByRepoPath(String repoPath);

    /**
     * 根据 ID 删除。
     *
     * @param id 主键
     */
    void deleteById(String id);

    /**
     * 清空全部记录。
     */
    void deleteAll();

    /**
     * 统计记录总数（用于淘汰判断）。
     *
     * @return 记录数
     */
    long count();

    /**
     * 删除最旧的记录（淘汰，BR-05）。
     *
     * @param keep 保留条数
     */
    void deleteOldest(int keep);
}
