package com.gitgui.domain.service;

import com.gitgui.domain.model.SensitiveFileRule;

import java.util.List;

/**
 * 设置服务接口
 * <p>关联 BR：BR-27、BR-28、BR-29、BR-30、BR-32、BR-37、BR-38、BR-39。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public interface SettingsService {

    /**
     * 获取设置值。
     *
     * @param key 设置键
     * @return 设置值，不存在返回空字符串
     */
    String get(String key);

    /**
     * 设置值。
     *
     * @param key   设置键
     * @param value 设置值
     */
    void set(String key, String value);

    /**
     * 获取保护分支清单（BR-27）。
     *
     * @return 保护分支列表（支持通配符）
     */
    List<String> getProtectedBranches();

    /**
     * 获取远程白名单（BR-28）。
     *
     * @return 远程白名单列表，空列表表示不限制
     */
    List<String> getRemoteWhitelist();

    /**
     * 获取敏感文件规则（BR-32）。
     *
     * @return 敏感文件规则列表
     */
    List<SensitiveFileRule> getSensitiveFileRules();

    /**
     * 获取推送超大文件阈值（MB，BR-29）。
     *
     * @return 阈值 MB
     */
    int getLargeFileThresholdMb();

    /**
     * 红线总开关是否开启（BR-30）。
     *
     * @return true 表示开启
     */
    boolean isRedLineEnabled();

    /**
     * 设置红线总开关（BR-30，切换本身写入 audit_log）。
     *
     * @param enabled 是否开启
     */
    void setRedLineEnabled(boolean enabled);

    /**
     * 设置保护分支清单。
     *
     * @param branches 分支列表
     */
    void setProtectedBranches(List<String> branches);

    /**
     * 设置远程白名单。
     *
     * @param whitelist 白名单列表
     */
    void setRemoteWhitelist(List<String> whitelist);
}
