package com.gitgui.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 最近仓库领域模型
 * <p>状态机：打开仓库时 upsert（已存在则更新 lastOpenedAt/lastBranch/openCount），超限淘汰最旧。</p>
 * <p>遵循 BR-05（倒序与淘汰）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
@TableName("recent_repo")
public class RecentRepo {

    /**
     * 主键 UUID
     */
    @TableId
    private String id;

    /**
     * 仓库绝对路径，唯一
     */
    private String repoPath;

    /**
     * 上次打开的分支
     */
    private String lastBranch;

    /**
     * 最后打开时间（用于倒序展示与淘汰最旧，BR-05）
     */
    private LocalDateTime lastOpenedAt;

    /**
     * 累计打开次数
     */
    private int openCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
