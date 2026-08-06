package com.gitgui.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitgui.domain.constant.SettingsCategory;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 应用设置领域模型
 * <p>状态机：key-value 持久化，UI 修改即更新；内置默认值在 V1 迁移时初始化。</p>
 * <p>内置键清单见 01-domain.md AppSettings 字段说明。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
@TableName("app_settings")
public class AppSettings {

    /**
     * 主键 UUID
     */
    @TableId
    private String id;

    /**
     * 设置键，唯一
     */
    private String key;

    /**
     * 设置值（JSON 字符串承载复杂结构）
     */
    private String value;

    /**
     * 分类（RED_LINE/UI/GIT/EXTERNAL_TOOL/SSH/REPO_SCAN/RECENT）
     */
    private SettingsCategory category;

    /**
     * 设置说明
     */
    private String description;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
