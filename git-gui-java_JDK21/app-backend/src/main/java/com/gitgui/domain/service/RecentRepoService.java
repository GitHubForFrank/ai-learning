package com.gitgui.domain.service;

import com.gitgui.domain.model.RecentRepo;

import java.util.List;

/**
 * 最近仓库服务接口
 * <p>关联 BR：BR-05（倒序与淘汰）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface RecentRepoService {

    /**
     * 记录打开仓库（存在则更新 + 淘汰最旧，BR-05）。
     *
     * @param repoPath   仓库路径
     * @param lastBranch 上次打开的分支
     */
    void recordOpen(String repoPath, String lastBranch);

    /**
     * 列出最近仓库（按 lastOpenedAt 倒序）。
     *
     * @return 最近仓库列表
     */
    List<RecentRepo> list();

    /**
     * 根据 ID 删除。
     *
     * @param id 主键
     */
    void remove(String id);

    /**
     * 清空全部。
     */
    void clear();
}
