package com.gitgui.domain.service;

import com.gitgui.domain.model.Favorite;

import java.util.List;

/**
 * 收藏服务接口
 * <p>关联 BR：BR-03（唯一性）、BR-04（置顶与排序）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface FavoriteService {

    /**
     * 新增收藏（BR-03 唯一性校验）。
     *
     * @param repoPath 仓库路径
     * @param alias    别名
     * @param group    分组
     * @return 收藏对象
     * @throws com.gitgui.core.exception.GitGuiException DUPLICATE_FAVORITE 重复收藏
     */
    Favorite add(String repoPath, String alias, String group);

    /**
     * 根据 ID 删除收藏。
     *
     * @param id 主键
     */
    void remove(String id);

    /**
     * 切换置顶状态（BR-04）。
     *
     * @param id     主键
     * @param pinned 是否置顶
     */
    void togglePinned(String id, boolean pinned);

    /**
     * 更新别名与分组。
     *
     * @param id    主键
     * @param alias 别名
     * @param group 分组
     */
    void update(String id, String alias, String group);

    /**
     * 列出全部收藏（按 pinned 降序、sortOrder 升序）。
     *
     * @return 收藏列表
     */
    List<Favorite> list();

    /**
     * 按分组筛选。
     *
     * @param group 分组
     * @return 收藏列表
     */
    List<Favorite> listByGroup(String group);
}
