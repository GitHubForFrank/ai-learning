package com.gitgui.domain.service;

import com.gitgui.domain.model.RemoteConfig;

import java.util.List;

/**
 * 远程配置服务接口
 * <p>关联 BR：BR-09。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface RemoteConfigService {

    /**
     * 列出仓库的所有 Remote。
     *
     * @param repoPath 仓库路径
     * @return Remote 配置列表
     */
    List<RemoteConfig> list(String repoPath);

    /**
     * 新增 Remote。
     *
     * @param repoPath 仓库路径
     * @param name     Remote 名称
     * @param url      Remote URL
     */
    void add(String repoPath, String name, String url);

    /**
     * 更新 Remote URL。
     *
     * @param repoPath 仓库路径
     * @param name     Remote 名称
     * @param url      新 URL
     */
    void update(String repoPath, String name, String url);

    /**
     * 删除 Remote。
     *
     * @param repoPath 仓库路径
     * @param name     Remote 名称
     */
    void delete(String repoPath, String name);

    /**
     * 重命名 Remote。
     *
     * @param repoPath 仓库路径
     * @param oldName  旧名称
     * @param newName  新名称
     */
    void rename(String repoPath, String oldName, String newName);
}
