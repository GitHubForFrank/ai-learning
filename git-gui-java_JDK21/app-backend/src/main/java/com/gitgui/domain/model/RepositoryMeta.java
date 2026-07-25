package com.gitgui.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仓库元信息缓存领域模型
 * <p>状态机：随每次打开/刷新更新，仓库不再存在时由清理任务删除。</p>
 * <p>用于多仓库检索结果缓存（BR-01）与异步刷新（BR-02）。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
@Data
@Builder
@TableName("repository_meta")
public class RepositoryMeta {

    /** 主键 UUID */
    @TableId
    private String id;

    /** 仓库绝对路径，唯一 */
    private String repoPath;

    /** 当前分支 */
    private String currentBranch;

    /** HEAD 提交哈希 */
    private String headCommit;

    /** 主远程 URL */
    private String remoteUrl;

    /** 是否有未提交修改 */
    private boolean hasUncommittedChanges;

    /** 元信息最后刷新时间 */
    private LocalDateTime lastSyncedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
