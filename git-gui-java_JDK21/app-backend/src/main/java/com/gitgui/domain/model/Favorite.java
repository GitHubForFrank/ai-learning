package com.gitgui.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 收藏领域模型
 * <p>常用仓库的收藏标记，支持别名、分组、置顶、排序。</p>
 * <p>状态机：创建 → 可编辑（alias/group/pinned/sortOrder）→ 删除（不可恢复）。</p>
 * <p>遵循 BR-03（唯一性）、BR-04（置顶与排序）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
@TableName("favorite")
public class Favorite {

    /**
     * 主键 UUID
     */
    @TableId
    private String id;

    /**
     * 仓库绝对路径，全局唯一（BR-03）
     */
    private String repoPath;

    /**
     * 别名（便于识别，最大 100 字符，BR-03）
     */
    private String alias;

    /**
     * 分组（如项目/团队，最大 50 字符，BR-03）
     */
    @TableField("\"group\"")
    private String group;

    /**
     * 是否置顶（BR-04）
     */
    private boolean pinned;

    /**
     * 排序权重（BR-04）
     */
    private int sortOrder;

    /**
     * 远程 URL（缓存便于列表展示）
     */
    private String remoteUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
