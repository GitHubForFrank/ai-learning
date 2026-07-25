package com.gitgui.domain.constant;

/**
 * 设置分类枚举
 * <p>对应 {@code app_settings.category} 字段，区分设置归属。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public enum SettingsCategory {

    /** 红线配置（保护分支、远程白名单、敏感文件规则等） */
    RED_LINE,
    /** UI 配置（主题、语言、快捷键） */
    UI,
    /** Git 配置（user.name、core.autocrlf 等） */
    GIT,
    /** 外部工具（Diff/Merge/Editor） */
    EXTERNAL_TOOL,
    /** SSH 配置 */
    SSH,
    /** 仓库检索配置 */
    REPO_SCAN,
    /** 最近仓库配置 */
    RECENT
}
