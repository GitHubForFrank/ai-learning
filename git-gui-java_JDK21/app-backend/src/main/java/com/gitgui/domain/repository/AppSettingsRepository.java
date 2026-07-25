package com.gitgui.domain.repository;

import com.gitgui.domain.model.AppSettings;

import java.util.List;

/**
 * 应用设置仓储接口
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface AppSettingsRepository {

    /**
     * 保存设置（upsert）。
     *
     * @param settings 设置
     */
    void save(AppSettings settings);

    /**
     * 根据 key 查找。
     *
     * @param key 设置键
     * @return 设置对象，不存在返回 null
     */
    AppSettings findByKey(String key);

    /**
     * 列出全部设置。
     *
     * @return 设置列表
     */
    List<AppSettings> findAll();

    /**
     * 根据 key 获取值（不存在返回空字符串）。
     *
     * @param key 设置键
     * @return 设置值
     */
    String getValue(String key);
}
